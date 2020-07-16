package com.swust.server.handler;

import com.alibaba.fastjson.JSON;
import com.swust.common.exception.ServerException;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
import com.swust.server.ExtranetServer;
import com.swust.server.ServerManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author : LiuMing
 * 2019/11/4 10:50
 * tcp handler
 */
@Slf4j
public class TcpServerHandler extends CommonHandler {
    private String password;

    private final ExecutorService poolExecutor = new ThreadPoolExecutor(16, 64, 3, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1024));
    /**
     * 默认读超时上限
     */
    private static final byte DEFAULT_RECONNECTION_LIMIT = 5;
    private static final Map<ChannelHandlerContext, Integer> DEFAULT_COUNT = new HashMap<>();


    public TcpServerHandler(String password) {
        this.password = password;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws java.lang.Exception {
        if (!(msg instanceof Message)) {
            throw new Exception("Unknown message,msg type: " + msg.getClass().getName());
        }
        poolExecutor.submit(() -> {
            Message message = (Message) msg;
            MessageType type = message.getHeader().getType();
            //客户端注册
            if (type == MessageType.REGISTER) {
                processRegister(ctx, message);
            } else {
                if (type == MessageType.DISCONNECTED) {
                    try {
                        processDisconnected(message);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (type == MessageType.DATA) {
                    processData(message);
                } else if (type == MessageType.KEEPALIVE) {
                    // 心跳包
                    DEFAULT_COUNT.put(ctx, 0);
                } else {
                    throw new ServerException("Unknown type: " + type);
                }
            }
        });
    }


    /**
     * 处理客户端注册,每个客户端注册成功都会启动一个服务，绑定客户端指定的端口
     *
     * @param ctx 与当前服务端保持连接的内网客户端channel
     */
    private void processRegister(ChannelHandlerContext ctx, Message message) {
        Channel channel = ctx.channel();
        String password = message.getHeader().getPassword();
        if (this.password == null || !this.password.equals(password)) {
            message.getHeader().setSuccess(false).setDescription("Token check failed!");
        }

        boolean needRegister = false;
        //客户端指定对外开放的端口
        int port = message.getHeader().getOpenTcpPort();
        ExtranetServer result = ServerManager.hasServer4ChannelMap(channel, port);
        if (result != null) {
            log.warn("The current proxy({}) server already exists!", port);
            log.warn("The current proxy server already exists and this proxy request is ignored!");
            if (result.getChannel().isActive()) {
                log.warn("The current proxy server already exists and this proxy request is ignored!");
                return;
            }
            result.getChannel().close();
            log.warn("The current proxy server is deactivated and is about to restart!");
        } else {
            ExtranetServer old = ServerManager.PORT_MAP.get(port);
            if (Objects.isNull(old)) {
                log.info("There is no proxy server bound to the current client. A new proxy is about to be opened!");
                log.info("msg:{}", JSON.toJSONString(message));
                needRegister = true;
            } else {
                log.info("There is no proxy server bound to the current client,but a proxy server with the port({}) open is bound directly", port);
                log.info("msg:{}", JSON.toJSONString(message));
                old.getInitializer().setClientCtx(ctx);
                ServerManager.add2ChannelMap(channel, old);
            }
        }

        if (needRegister) {
            try {
                ExtranetServer extranetServer = new ExtranetServer().initTcpServer(port, ctx);
                ServerManager.PORT_MAP.put(port, extranetServer);
                ServerManager.add2ChannelMap(channel, extranetServer);
                message.getHeader().setSuccess(true).setDescription("Server already start port on " + port);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Register fail, msg:{} port: {}", message, port);
                return;
            }
        } else {
            message.getHeader().setSuccess(true).setDescription("A proxy server with the current port bound does not need to be opened(already open)!");
        }
        message.getHeader().setType(MessageType.REGISTER_RESULT);
        ctx.writeAndFlush(message);
    }

    /**
     * 处理收到转发的内网响应数据包
     */
    private void processData(Message message) {
        ChannelHandlerContext userCtx = ServerManager.findChannelByMsg(message);
        if (Objects.isNull(userCtx)) {
            log.error("Received Intranet proxy client message，but the corresponding proxy server was not found! ");
        } else {
            userCtx.writeAndFlush(message.getData());
        }
    }

    /**
     * 断开,先关闭外网暴露的代理，在关闭连接的客户端
     */
    private void processDisconnected(Message message) throws InterruptedException {
        ChannelHandlerContext userCtx = ServerManager.findChannelByMsg(message);
        if (Objects.nonNull(userCtx)) {
            log.warn("Received the message of disconnection of the internal network client ");
            userCtx.close();
        }
    }

    /**
     * 暴露的外网代理服务端资源清理
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("The client is disconnected");
        List<ExtranetServer> servers = ServerManager.CHANNEL_MAP.get(ctx.channel());
        if (Objects.isNull(servers) || servers.size() == 0) {
            log.error("The proxy server opened based on the client was not found!");
        } else {
            log.info("Disconnect all proxy servers that are opened according to the client!");
            servers.forEach(server -> server.getGroup().close());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            Integer count = DEFAULT_COUNT.get(ctx);
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                if (Objects.isNull(count)) {
                    count = 0;
                }
                DEFAULT_COUNT.put(ctx, count++);
                if (count > DEFAULT_RECONNECTION_LIMIT) {
                    DEFAULT_COUNT.remove(ctx);
                    log.error("Read idle  will loss connection. retryNum:{}", count);
                    ctx.close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }


}
