package com.swust.server.handler;

import com.swust.common.exception.ServerException;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
import com.swust.server.ExtranetServer;
import com.swust.server.ServerManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : LiuMing
 * 2019/11/4 10:50
 * tcp handler
 */
@Slf4j
@ChannelHandler.Sharable
public class TcpServerHandler extends CommonHandler {
    private final String password;

    /**
     * 默认读超时上限
     */
    private static final byte DEFAULT_RECONNECTION_LIMIT = 5;
    private static final Map<ChannelHandlerContext, Integer> DEFAULT_COUNT = new ConcurrentHashMap<>();


    public TcpServerHandler(String password) {
        this.password = password;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws java.lang.Exception {
        if (!(msg instanceof Message)) {
            log.error("unknown message,msg type: {}  remote addr : {}", msg, ctx.channel().remoteAddress());
            return;
        }
        Message message = (Message) msg;
        MessageType type = message.getHeader().getType();
        //客户端注册
        if (type == MessageType.REGISTER) {
            processRegister(ctx, message);
        } else {
            if (type == MessageType.DISCONNECTED) {
                processDisconnected(message);
            } else if (type == MessageType.DATA) {
                processData(message);
            } else if (type == MessageType.KEEPALIVE) {
                // 心跳包
                DEFAULT_COUNT.put(ctx, 0);
            } else {
                throw new ServerException("unknown type: " + type);
            }
        }
    }


    /**
     * 处理客户端注册,每个客户端注册成功都会启动一个服务，绑定客户端指定的端口
     *
     * @param ctx 与当前服务端保持连接的内网客户端channel
     */
    private void processRegister(ChannelHandlerContext ctx, Message message) {
        String password = message.getHeader().getPassword();
        if (this.password == null || !this.password.equals(password)) {
            message.getHeader().setSuccess(false).setDescription("token check failed!");
        } else {
            message.getHeader().setSuccess(true).setDescription("success!");
        }

        //客户端指定对外开放的端口
        int port = message.getHeader().getOpenTcpPort();

        boolean alreadyExists = ServerManager.alreadyExists(ctx, port);
        if (!alreadyExists) {
            ExtranetServer extranetServer = new ExtranetServer().initTcpServer(port, ctx);
            ServerManager.addProxyServer(extranetServer);
        }

        message.getHeader().setType(MessageType.REGISTER_RESULT);
        ctx.writeAndFlush(message);
    }

    /**
     * 处理收到转发的内网响应数据包
     */
    private void processData(Message message) {
        ChannelHandlerContext userCtx = ServerManager.USER_CLIENT_MAP.get(message.getHeader().getChannelId());
        if (Objects.isNull(userCtx)) {
            System.out.println(message.getHeader() + "  " + message.getData().length);
            log.error("received intranet proxy client message，but the corresponding proxy server was not found! ");
        } else {
            userCtx.writeAndFlush(message.getData());
        }
    }

    /**
     * 断开,先关闭外网暴露的代理，再关闭连接的客户端
     */
    private void processDisconnected(Message message) {
        ChannelHandlerContext userCtx = ServerManager.USER_CLIENT_MAP.get(message.getHeader().getChannelId());
        if (Objects.nonNull(userCtx)) {
            userCtx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("the client({}) is disconnected", ctx.channel().remoteAddress());
        if (ctx.channel().isActive()) {
            ctx.close();
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
                    log.error("read idle  will loss connection. retryNum:{}", count);
                    ctx.close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

}
