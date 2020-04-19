package com.swust.server.handler;

import com.alibaba.fastjson.JSON;
import com.swust.common.exception.ServerException;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
import com.swust.server.TcpServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:50
 * @description :   tcp handler
 */
public class TcpServerHandler extends CommonHandler {
    private String password;
    private int port;
    private TcpServer remoteConnectionServer = new TcpServer();

    private static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 注册信息保存
     */
    protected static final Map<ChannelHandlerContext, Boolean> REGISTER_STATE = new HashMap<>();


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
            processRegister(ctx.channel(), message);
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

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warning("close proxy  client channel port：" + ctx.channel().remoteAddress());
    }

    /**
     * 维持客户端与当前暴露出去的代理服务端联系
     */
    private static Map<Channel, Channel> ctxMap = new HashMap<>();

    /**
     * 处理客户端注册,每个客户端注册成功都会启动一个服务，绑定客户端指定的端口
     *
     * @param channelClient 与当前服务端保持连接的内网channel
     */
    private void processRegister(Channel channelClient, Message message) {
        String password = message.getHeader().getPassword();

        if (this.password == null || !this.password.equals(password)) {
            message.getHeader().setSuccess(false).setDescription("Token is wrong");
        } else {
            //客户端指定对外开放的端口
            int port = message.getHeader().getOpenTcpPort();
            try {
                TcpServerHandler thisHandler = this;
                Channel proxyChannel = remoteConnectionServer.initTcpServer(port, new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(),
                                new RemoteProxyHandler(thisHandler));
                        channels.add(ch);
                    }
                });
                if (Objects.isNull(proxyChannel)) {
                    logger.info(" start proxy server on port: " + port + "  fail!");
                    return;
                }
                ctxMap.put(channelClient, proxyChannel);
                message.getHeader().setSuccess(true);
                this.port = port;
                logger.info("Register success, start server on port: " + port);
            } catch (java.lang.Exception e) {
                message.getHeader().setSuccess(false).setDescription(e.getMessage());
                e.printStackTrace();
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
        channels.writeAndFlush(message.getData(), channel ->
                channel.id().asLongText().equals(message.getHeader().getChannelId()));
    }

    /**
     * 断开,先关闭外网暴露的代理，在关闭连接的客户端
     */
    private void processDisconnected(Message message) throws InterruptedException {
        AtomicReference<Channel> target = new AtomicReference<>();
        channels.close(channel -> {
            if (channel.id().asLongText().equals(message.getHeader().getChannelId())) {
                logger.severe(String.format("proxy service send disconnect msg! proxy : %s", channel.localAddress()));
                target.set(channel);
                return true;
            }
            return false;
        }).awaitUninterruptibly();
        if (Objects.nonNull(target.get())) {
            logger.info("step2: find target channel,will close proxy client! ");
            target.get().close().sync();
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
                    logger.severe("Read idle  will loss connection. retryNum:" + count);
                    ctx.close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }


}
