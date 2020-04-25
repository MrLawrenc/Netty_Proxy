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

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author : LiuMing
 * @date : 2019/11/4 13:42
 * @description :   客户端 handler
 */
public class ClientHandler extends CommonHandler {

    private List<Integer> ports;
    private String password;
    private List<String> proxyAddress;
    private List<Integer> proxyPort;
    /**
     * 默认重新拉起客户端的起始秒数
     */
    private static final int DEFAULT_TRY_SECONDS = 4;
    /**
     * 默认重新拉起客户端尝试的次数上限
     */
    private static final int DEFAULT_TRY_COUNT = 10;


    public ClientHandler(List<String> ports, String password, List<String> proxyAddress, List<String> proxyPort) {
        this.ports = ports.stream().map(Integer::parseInt).collect(Collectors.toList());
        this.password = password;
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort.stream().map(Integer::parseInt).collect(Collectors.toList());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ports.forEach(port -> {
            Message message = new Message();
            MessageHeader header = message.getHeader();
            header.setType(MessageType.REGISTER).setOpenTcpPort(port).setPassword(password);
            ctx.writeAndFlush(message);
        });
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
        ClientManager.clean();

        CompletableFuture.runAsync(() -> {
            int sleep = DEFAULT_TRY_SECONDS;
            int count = 0;
            while (count < DEFAULT_TRY_COUNT) {
                try {
                    TimeUnit.SECONDS.sleep(sleep);
                    ClientMain.start();
                    LogUtil.infoLog("重启客户端成功.............");
                    return;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                sleep <<= 1;
                count++;
                LogUtil.warnLog("重启客户端失败，当前是第{}次尝试，即将在{}s后重试！", count, sleep);
            }
            LogUtil.errorLog("重试次数达到上限,,,,即将正常退出！");
            System.exit(0);
        });
    }

    /**
     * 处理在服务端注册结果
     */
    private void processRegisterResult(Message message) {
        if (message.getHeader().isSuccess()) {
            LogUtil.infoLog("代理服务端开启成功！server msg:{}", message.getHeader().getDescription());
        } else {
            LogUtil.errorLog("代理服务端开启失败,即将终止服务！server msg:{}", message.getHeader().getDescription());
            System.exit(0);
        }
    }


    /**
     * 该请求来源于，请求外网暴露的端口，外网通过netty服务端转发来到这里的
     * 请求内部代理服务，建立netty客户端，请求访问本地的服务，获取返回结果
     */
    private void processConnected(Channel channel, Message receiveMessage) {
        String channelId = receiveMessage.getHeader().getChannelId();
        int openTcpPort = receiveMessage.getHeader().getOpenTcpPort();
        int index = ports.indexOf(openTcpPort);
        try {
            IntranetClient intranetClient = new IntranetClient().connect(proxyAddress.get(index), proxyPort.get(index), new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    LocalProxyHandler localProxyHandler = new LocalProxyHandler(channel, channelId);
                    ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(), localProxyHandler);
                }
            });
            ClientManager.add2ChannelMap(channel, intranetClient);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.errorLog("连接内网服务失败 msg:{}", e.getMessage());
            Message message = new Message();
            MessageHeader header = message.getHeader();
            if (index == -1) {
                LogUtil.errorLog("client ports config:{}  current open tcp port:{}", JSON.toJSONString(ports), openTcpPort);
                header.setDescription("当前msg传递的openTcpPort为空");
            }
            header.setType(MessageType.DISCONNECTED);
            header.setChannelId(receiveMessage.getHeader().getChannelId());
            ctx.writeAndFlush(message);
        }
    }

    private void processData(Message message) {
        String channelId = message.getHeader().getChannelId();
        ChannelHandlerContext context = ClientManager.ID_SERVICE_CHANNEL_MAP.get(channelId);
        if (Objects.isNull(context)) {
            LogUtil.errorLog("根据与外网代理服务端连接的用户客户端channelId未找到相应的内网代理客户端！msg:{}", message.getHeader().toString());
        } else {
            context.writeAndFlush(message.getData());
        }
    }

    /**
     * 与代理服务端连接的用户客户端断开连接，处理资源，以及断开内网代理客户端
     */
    private void processDisconnected(Channel channel, Message message) throws Exception {
        ChannelHandlerContext context = ClientManager.ID_SERVICE_CHANNEL_MAP.get(message.getHeader().getChannelId());
        if (Objects.isNull(context)) {
            LogUtil.warnLog("收到与外网代理服务端连接的用户客户端断开连接的消息，但未找到与之对应的内网代理客户端channel，可能已经关闭！");
        } else {
            context.close();
            ClientManager.removeChannelMapByProxyClient(channel, message.getHeader().getChannelId());
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
