package com.swust.client.handler;

import com.alibaba.fastjson.JSON;
import com.swust.client.ClientMain;
import com.swust.client.ClientManager;
import com.swust.client.IntranetClient;
import com.swust.common.config.LogUtil;
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

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
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
    private static final int DEFAULT_TRY_COUNT = 10;


    public ClientHandler(int port, String password, String proxyAddress, int proxyPort) {
        this.port = port;
        this.password = password;
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.REGISTER).setOpenTcpPort(port).setPassword(password);
        ctx.writeAndFlush(message);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws java.lang.Exception {
        if (!(msg instanceof Message)) {
            throw new Exception("Unknown message: " + JSON.toJSONString(msg));
        }
        Message message = (Message) msg;
        MessageType type = message.getHeader().getType();
        if (type == MessageType.KEEPALIVE) {
            return;
        }
        if (type == MessageType.REGISTER_RESULT) {
            processRegisterResult(message);
        } else if (type == MessageType.CONNECTED) {
            processConnected(ctx.channel(), message);
        } else if (type == MessageType.DATA) {
            processData(message);
        } else if (type == MessageType.DISCONNECTED) {
            processDisconnected(ctx.channel(), message);
        } else {
            LogUtil.errorLog("未知消息  msg:{}", message.toString());
        }
    }


    /**
     * 重连
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LogUtil.errorLog("客户端触发channelInactive,即将在关闭相应资源后重连！");
        ClientManager.INSTANCE.removeChannelMap(ctx.channel());

        CompletableFuture.runAsync(() -> {
            int sleep = DEFAULT_TRY_SECONDS;
            int count = 0;
            while (count < DEFAULT_TRY_COUNT) {
                try {
                    ClientMain.start();
                    LogUtil.infoLog("重启客户端成功.............");
                    return;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                count++;
                LogUtil.warnLog("重启客户端失败，当前是第{}次尝试，即将在{}s后重试！", count, sleep);
                try {
                    TimeUnit.SECONDS.sleep(sleep);
                    sleep <<= 1;
                } catch (InterruptedException ignored) {
                }
            }
            LogUtil.errorLog("重试次数达到上限,,,,退出！");
            System.exit(0);
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


    /**
     * 该请求来源于，请求外网暴露的端口，外网通过netty服务端转发来到这里的
     * 请求内部代理服务，建立netty客户端，请求访问本地的服务，获取返回结果
     */
    private void processConnected(Channel channel, Message receiveMessage) {
        try {
            IntranetClient intranetClient = new IntranetClient().connect(proxyAddress, proxyPort, new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    LocalProxyHandler localProxyHandler = new LocalProxyHandler(channel, receiveMessage.getHeader().getChannelId());
                    ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(), localProxyHandler);
                }
            });
            ClientManager.INSTANCE.add2ChannelMap(channel, intranetClient);
        } catch (Exception e) {
            LogUtil.errorLog("连接内网服务失败 msg:{]", e.getMessage());
            Message message = new Message();
            MessageHeader header = message.getHeader();
            header.setType(MessageType.DISCONNECTED);
            header.setChannelId(receiveMessage.getHeader().getChannelId());
            ctx.writeAndFlush(message);
        }
    }

    /**
     * if message.getType() == MessageType.DISCONNECTED
     */
    private void processDisconnected(Channel channel, Message message) throws Exception {
        //todo
    }

    /**
     * if message.getType() == MessageType.DATA
     */
    private void processData(Message message) {
        String channelId = message.getHeader().getChannelId();
        ChannelHandlerContext context = ClientManager.ID_CHANNEL_MAP.get(channelId);
        if (Objects.isNull(context)) {
            LogUtil.errorLog("根据外网代理服务端的channelId未找到相应的内网客户端！msg:{}", message.getHeader().toString());
        } else {
            LogUtil.debugLog("内网代理客户端发送消息 msg:{}\n channel:{}", message.toString(), context.channel());
            context.writeAndFlush(message.getData());
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
                    // localProxyHandler.getCtx().writeAndFlush("heart pkg");
                } catch (Exception e) {
                    LogUtil.warnLog("time  warning ..................");
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
