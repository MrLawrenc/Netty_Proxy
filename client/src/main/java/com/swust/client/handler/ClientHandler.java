package com.swust.client.handler;

import com.alibaba.fastjson.JSON;
import com.swust.client.ClientMain;
import com.swust.client.TcpClient;
import com.swust.common.exception.ClientException;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import com.swust.common.protocol.MessageType;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author : LiuMing
 * @date : 2019/11/4 13:42
 * @description :   客户端 handler
 */
public class ClientHandler extends CommonHandler {

    private int port;
    private String password;
    private String proxyAddress;
    private int proxyPort;

    private Map<String, Channel> channelHandlerMap = Collections.synchronizedMap(new HashMap<>());
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public ClientHandler(int port, String password, String proxyAddress, int proxyPort) {
        this.port = port;
        this.password = password;
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws java.lang.Exception {
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.REGISTER).setOpenTcpPort(port).setPassword(password);
        message.setHeader(header);
        ctx.writeAndFlush(message);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws java.lang.Exception {

        if (!(msg instanceof Message)) {
            throw new Exception("Unknown message: " + JSON.toJSONString(msg));
        }
        Message message = (Message) msg;
        MessageType type = message.getHeader().getType();
        if (type == MessageType.REGISTER_RESULT) {
            processRegisterResult(message);
        } else if (type == MessageType.CONNECTED) {
            processConnected(ctx.channel(), message);
        } else if (type == MessageType.DATA) {
            processData(message);
        } else if (type == MessageType.DISCONNECTED) {
            processDisconnected(ctx.channel(), message);
        } else if (type == MessageType.KEEPALIVE) {
        } else {
            throw new ClientException("Unknown type: " + type);
        }
    }

    private static ConcurrentHashMap<Channel, Channel> channelMap = new ConcurrentHashMap<>();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        logger.warning(String.format("触发客户端channelInactive remote:%s local:%s", channel.remoteAddress(), channel.localAddress()));
        logger.info("will close client proxy!");
        channelMap.get(ctx.channel()).close();
        CompletableFuture.runAsync(() -> {
            int sleep = 10;
            while (true) {
                try {
                    Channel newChannel = ClientMain.start();
                    if (Objects.nonNull(newChannel)) {
                        logger.info("重启客户端成功.............");
                        return;
                    }
                } catch (Throwable ignored) {
                }
                logger.severe("restart clint fail! will sleep " + sleep + " ms!");
                try {
                    TimeUnit.SECONDS.sleep(sleep);
                    sleep <<= 1;
                } catch (InterruptedException ignored) {
                }
            }

        });
    }

    /**
     * 处理在服务端注册结果
     */
    private void processRegisterResult(Message message) {
        if (message.getHeader().isSuccess()) {
            System.out.println("Register to  Server");
        } else {
            System.out.println("Register fail: " + message.getHeader().getDescription());
            ctx.close();
        }
    }

    private static final ConcurrentHashMap<Channel, Channel> CHANNEL_HASH_MAP = new ConcurrentHashMap<>();

    /**
     * 该请求来源于，请求外网暴露的端口，外网通过netty服务端转发来到这里的
     * 请求内部代理服务，建立netty客户端，请求访问本地的服务，获取返回结果
     */
    private void processConnected(Channel serverChannel, Message receiveMessage) {
        try {
            TcpClient localConnection = new TcpClient();
            Channel channel = localConnection.connect(proxyAddress, proxyPort, new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    LocalProxyHandler localProxyHandler = new LocalProxyHandler(serverChannel,
                            receiveMessage.getHeader().getChannelId());
                    ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(), localProxyHandler);
                }
            });
            channelMap.put(serverChannel, channel);
            channelHandlerMap.put(receiveMessage.getHeader().getChannelId(), channel);
            channelGroup.add(channel);
        } catch (Exception e) {
            logger.throwing(getClass().getName(), "连接内网服务失败...........", e);
            Message message = new Message();
            MessageHeader header = message.getHeader();
            header.setType(MessageType.DISCONNECTED);
            header.setChannelId(receiveMessage.getHeader().getChannelId());
            ctx.writeAndFlush(message);
            channelHandlerMap.remove(receiveMessage.getHeader().getChannelId());
        }
    }

    /**
     * if message.getType() == MessageType.DISCONNECTED
     */
    private void processDisconnected(Channel channel, Message message) {
        Channel localProxyChannel = channelMap.get(channel);
        if (Objects.nonNull(localProxyChannel)) {
            logger.warning("收到服务端发来关闭本地代理客户端的消息！");
            localProxyChannel.close();
        }
    }

    /**
     * if message.getType() == MessageType.DATA
     */
    private void processData(Message message) {
        String channelId = message.getHeader().getChannelId();
        Channel localProxyChannel = channelHandlerMap.get(channelId);
        localProxyChannel.writeAndFlush(message.getData());
    }

    /**
     * 维持内网连接,6h执行一次
     */
    public void heartPkg(LocalProxyHandler localProxyHandler) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    localProxyHandler.getCtx().writeAndFlush("heart pkg");
                } catch (Exception e) {
                    logger.warning("time  warning ..................");
                }
            }
        }, 1000 * 60 * 60 * 6, 1000 * 60 * 60 * 6);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.WRITER_IDLE) {
                Message message = new Message();
                message.getHeader().setType(MessageType.KEEPALIVE);
                ctx.writeAndFlush(message);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
