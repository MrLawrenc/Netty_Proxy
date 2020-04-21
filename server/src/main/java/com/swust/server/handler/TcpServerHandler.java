package com.swust.server.handler;

import com.alibaba.fastjson.JSON;
import com.swust.common.config.LogUtil;
import com.swust.common.exception.ServerException;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
import com.swust.server.ExtranetServer;
import com.swust.server.ServerManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:50
 * @description :   tcp handler
 */
public class TcpServerHandler extends CommonHandler {
    private String password;


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
            throw new Exception("Unknown message: " + JSON.toJSONString(msg));
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
                throw new ServerException("Unknown type: " + type);
            }
        }
    }


    /**
     * 暴露的外网代理服务端资源清理
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ServerManager.INSTANCE.removeChannelMap(ctx.channel());
    }

    /**
     * 处理客户端注册,每个客户端注册成功都会启动一个服务，绑定客户端指定的端口
     *
     * @param channelClient 与当前服务端保持连接的内网channel
     */
    private void processRegister(ChannelHandlerContext channelClient, Message message) {
        String password = message.getHeader().getPassword();
        LogUtil.infoLog("注册消息:{}", JSON.toJSONString(message));
        if (this.password == null || !this.password.equals(password)) {
            message.getHeader().setSuccess(false).setDescription("Token is wrong");
        } else {
            //客户端指定对外开放的端口
            int port = message.getHeader().getOpenTcpPort();
            try {
                ExtranetServer extranetServer = new ExtranetServer().initTcpServer(port, new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(),
                                new RemoteProxyHandler(channelClient.channel()));
                    }
                });
                if (Objects.isNull(extranetServer)) {
                    LogUtil.errorLog(" start proxy server on port: " + port + "  fail!");
                    return;
                }
                ServerManager.INSTANCE.add2ChannelMap(channelClient.channel(), extranetServer);
                message.getHeader().setSuccess(true);
                LogUtil.infoLog("Register success, start server on port: " + port);
            } catch (java.lang.Exception e) {
                e.printStackTrace();
                LogUtil.errorLog("Register fail, msg:{} port: ", message, port);
                return;
            }
        }
        message.getHeader().setType(MessageType.REGISTER_RESULT);
        ctx.writeAndFlush(message);
    }

    /**
     * 处理收到转发的内网响应数据包
     */
    private void processData(Message message) {
        ChannelHandlerContext handlerContext = ServerManager.ID_CHANNEL_MAP.get(message.getHeader().getChannelId());
        if (Objects.isNull(handlerContext)) {
            LogUtil.errorLog("收到内网代理客户端的消息，但是未找到相应的外网代理服务单端返回！msg:{}", message.getHeader().toString());
        } else {
            handlerContext.writeAndFlush(message.getData());
        }

    }

    /**
     * 断开,先关闭外网暴露的代理，在关闭连接的客户端
     */
    private void processDisconnected(Message message) throws InterruptedException {
        String channelId = message.getHeader().getChannelId();
        ChannelHandlerContext handlerContext = ServerManager.ID_CHANNEL_MAP.get(channelId);
        if (Objects.nonNull(handlerContext)) {
            LogUtil.warnLog("收到内网代理客户端的关闭请求! 即将关闭与外网代理服务端连接的客户端！移除channelId:{}", channelId);
            handlerContext.channel().close();
            ServerManager.INSTANCE.removeIdChannelMap(channelId);
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
                    LogUtil.errorLog("Read idle  will loss connection. retryNum:{}", count);
                    ctx.close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }


}
