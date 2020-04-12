package com.swust.client.handler;

import com.alibaba.fastjson.JSON;
import com.swust.client.ClientMain;
import com.swust.client.TcpClient;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import com.swust.common.protocol.MessageType;
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

    private Map<String, CommonHandler> channelHandlerMap = Collections.synchronizedMap(new HashMap<>());
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

        super.channelActive(ctx);
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
            processConnected(message);
        } else if (type == MessageType.DISCONNECTED) {
            processDisconnected(message);
        } else if (type == MessageType.DATA) {
            processData(message);
        } else if (type == MessageType.KEEPALIVE) {
            // 心跳包, 不处理
            //lossConnectCount.getAndSet(0);
        } else {
            throw new Exception("Unknown type: " + type);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        try {
            logger.warning(String.format("will close channelId:%s", ctx.channel().id()));
            logger.info("will restart client...................");
        } finally {
            ClientMain.start();
        }
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

    /**
     * 该请求来源于，请求外网暴露的端口，外网通过netty服务端转发来到这里的
     * 请求内部代理服务，建立netty客户端，请求访问本地的服务，获取返回结果
     */
    private void processConnected(Message receiveMessage) {

        try {
            ClientHandler thisHandler = this;
            TcpClient localConnection = new TcpClient();
            localConnection.connect(proxyAddress, proxyPort, new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    LocalProxyHandler localProxyHandler = new LocalProxyHandler(thisHandler,
                            receiveMessage.getHeader().getChannelId());
                    ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(), localProxyHandler);

                    channelHandlerMap.put(receiveMessage.getHeader().getChannelId(), localProxyHandler);
                    channelGroup.add(ch);
                }
            });
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
    private void processDisconnected(Message message) {
        String channelId = message.getHeader().getChannelId();
        CommonHandler handler = channelHandlerMap.get(channelId);
        if (handler != null) {
            handler.getCtx().close();
            channelHandlerMap.remove(channelId);
        }
    }

    /**
     * if message.getType() == MessageType.DATA
     */
    private void processData(Message message) {
        String channelId = message.getHeader().getChannelId();
        CommonHandler handler = channelHandlerMap.get(channelId);
        if (handler != null) {
            try {
                ChannelHandlerContext ctx = handler.getCtx();
                ctx.writeAndFlush(message.getData());
            } catch (Exception e) {
                logger.throwing(getClass().getName(), "ctx=" + ctx, e);
            }
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
