package com.swust.client.handler;

import com.alibaba.fastjson.JSON;
import com.swust.client.ClientMain;
import com.swust.client.IntranetClient;
import com.swust.common.config.LogUtil;
import com.swust.common.exception.ClientException;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import com.swust.common.protocol.MessageType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

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

    /**
     * 默认重新拉起客户端的起始秒数
     */
    private static final int DEFAULT_TRY_SECONDS = 10;
    /**
     * 默认重新拉起客户端尝试的次数上限
     */
    private static final int DEFAULT_TRY_COUNT = 5;

    /**
     * key 服务端与当前连接的channel  value 是本地内网代理客户端
     */
    private static ConcurrentHashMap<Channel, IntranetClient> channelMap = new ConcurrentHashMap<>();
    /**
     * key 外网代理服务端channel的id  value 是本地内网代理客户端
     */
    private Map<String, IntranetClient> idIntranetMap = Collections.synchronizedMap(new HashMap<>());

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


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LogUtil.errorLog("客户端触发channelInactive,即将关闭对应的内网代理客户端 local:{}   remote:{}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        channelMap.get(ctx.channel()).close();
        channelMap.remove(ctx.channel());


        CompletableFuture.runAsync(() -> {
            int sleep = DEFAULT_TRY_SECONDS;
            int count = 0;
            while (count < DEFAULT_TRY_COUNT) {
                try {
                    ClientMain.start();
                    LogUtil.infoLog("重启客户端成功.............");
                    return;
                } catch (Throwable ignored) {
                }
                count++;
                LogUtil.warnLog("重启客户端失败，当前是第{}次尝试，即将在{}s后重试！", count, sleep);
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
            LogUtil.infoLog("代理服务端开启成功！");
        } else {
            LogUtil.infoLog("代理服务端开启失败！{}", message.getHeader().getDescription());
            System.exit(0);
        }
    }

    private static final ConcurrentHashMap<Channel, Channel> CHANNEL_HASH_MAP = new ConcurrentHashMap<>();

    /**
     * 该请求来源于，请求外网暴露的端口，外网通过netty服务端转发来到这里的
     * 请求内部代理服务，建立netty客户端，请求访问本地的服务，获取返回结果
     */
    private void processConnected(Channel serverChannel, Message receiveMessage) {
        try {
            IntranetClient intranetClient = new IntranetClient().connect(proxyAddress, proxyPort, new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    LocalProxyHandler localProxyHandler = new LocalProxyHandler(serverChannel,
                            receiveMessage.getHeader().getChannelId());
                    ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(), localProxyHandler);
                }
            });
            channelMap.put(serverChannel, intranetClient);
            idIntranetMap.put(receiveMessage.getHeader().getChannelId(), intranetClient);
        } catch (Exception e) {
            logger.throwing(getClass().getName(), "连接内网服务失败...........", e);
            Message message = new Message();
            MessageHeader header = message.getHeader();
            header.setType(MessageType.DISCONNECTED);
            header.setChannelId(receiveMessage.getHeader().getChannelId());
            ctx.writeAndFlush(message);
            idIntranetMap.remove(receiveMessage.getHeader().getChannelId());
        }
    }

    /**
     * if message.getType() == MessageType.DISCONNECTED
     */
    private void processDisconnected(Channel channel, Message message) throws Exception {
        IntranetClient intranetClient = idIntranetMap.get(message.getHeader().getChannelId());
        if (Objects.nonNull(intranetClient)) {
            logger.warning("收到外网客户端断开连接的消息！即将关闭对应的内网代理客户端！");
            intranetClient.close();
        }
    }

    /**
     * if message.getType() == MessageType.DATA
     */
    private void processData(Message message) {
        String channelId = message.getHeader().getChannelId();
        IntranetClient intranetClient = idIntranetMap.get(channelId);
        if (Objects.isNull(intranetClient)) {
            LogUtil.errorLog("根据外网代理服务端的channelId为找到相应的内网客户端！msg:{}", message.getHeader().toString());
        } else {
            intranetClient.getChannel().writeAndFlush(message.getData());
        }
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
