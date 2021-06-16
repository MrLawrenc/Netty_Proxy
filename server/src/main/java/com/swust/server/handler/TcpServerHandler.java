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
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : Lawrence
 * 2019/11/4 10:50
 * tcp server handler
 */
@Slf4j
@ChannelHandler.Sharable
public class TcpServerHandler extends CommonHandler {
    private final String password;

    /**
     * 默认读超时上限
     */
    private static final byte DEFAULT_RECONNECTION_LIMIT = 5;
    private static final Map<String, AtomicInteger> DEFAULT_COUNT = new ConcurrentHashMap<>();


    public TcpServerHandler(String password) {
        if (StringUtil.isNullOrEmpty(password)){
            throw new RuntimeException("password is not must null");
        }
        this.password = password;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
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
                //断线包
                processDisconnected(message);
            } else if (type == MessageType.DATA) {
                //正常数据包
                processData(message);
            } else if (type == MessageType.KEEPALIVE) {
                // 心跳包
                String channelId = ctx.channel().id().asLongText();
                AtomicInteger counter = DEFAULT_COUNT.get(channelId);
                if (Objects.isNull(counter)) {
                    DEFAULT_COUNT.put(channelId, new AtomicInteger(0));
                } else {
                    //ignore concurrency
                    counter.set(0);
                }
            } else {
                throw new ServerException("unknown msg type: " + type);
            }
        }
    }


    /**
     * 处理客户端注册,每个客户端注册成功都会启动一个代理服务server，并且按"代理客户端"要求绑定指定的端口
     *
     * @param ctx     客户端channel
     * @param message 消息包
     */
    private void processRegister(ChannelHandlerContext ctx, Message message) {
        String password = message.getHeader().getPassword();
        if (!this.password.equals(password)) {
            message.getHeader().setDescription("token(" + password + ") check failed!");
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
            log.error("received intranet proxy client message，but the corresponding proxy server was not found! ");
        } else {
            userCtx.writeAndFlush(message.getData());
        }
    }

    /**
     * 收到代理客户端主动断开连接报文,先关闭由该客户端启动的 外网代理，再关闭连接的客户端
     */
    private void processDisconnected(Message message) {
        ChannelHandlerContext userCtx = ServerManager.USER_CLIENT_MAP.get(message.getHeader().getChannelId());
        if (Objects.nonNull(userCtx)) {
            userCtx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.error("the client({}) is disconnected", ctx.channel().remoteAddress());
        if (ctx.channel().isActive()) {
            ctx.close();
        }
        DEFAULT_COUNT.remove(ctx.channel().id().asLongText());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            String channelId = ctx.channel().id().asLongText();
            AtomicInteger count = DEFAULT_COUNT.get(channelId);
            IdleStateEvent e = (IdleStateEvent) evt;
            //When the heartbeat packet reported by the client is not received within the specified time, the client is considered offline and the connection will be disconnected
            if (e.state() == IdleState.READER_IDLE) {
                if (Objects.isNull(count)) {
                    count = new AtomicInteger(0);
                }
                int current = count.incrementAndGet();
                if (current > DEFAULT_RECONNECTION_LIMIT) {
                    DEFAULT_COUNT.remove(channelId);
                    log.error("read idle  will loss connection. retryNum:{}", current);
                    ctx.close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

}
