package com.xxg.natx.client.handler;

import com.xxg.natx.client.net.TcpConnection;
import com.xxg.natx.common.exception.NatxException;
import com.xxg.natx.common.handler.CommonHandler;
import com.xxg.natx.common.protocol.Message;
import com.xxg.natx.common.protocol.MessageMetadata;
import com.xxg.natx.common.protocol.MessageType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.ConcurrentHashMap;

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

    private ConcurrentHashMap<String, CommonHandler> channelHandlerMap = new ConcurrentHashMap<>();
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public ClientHandler(int port, String password, String proxyAddress, int proxyPort) {
        this.port = port;
        this.password = password;
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // register client information
        Message message = new Message();
        message.setType(MessageType.REGISTER);
        MessageMetadata messageMetadata = new MessageMetadata().setOpenTcpPort(port).setPassword(password);
        message.setMetadata(messageMetadata);
        ctx.writeAndFlush(message);

        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        Message message = (Message) msg;
        if (message.getType() == MessageType.REGISTER_RESULT) {
            processRegisterResult(message);
        } else if (message.getType() == MessageType.CONNECTED) {
            processConnected(message);
        } else if (message.getType() == MessageType.DISCONNECTED) {
            processDisconnected(message);
        } else if (message.getType() == MessageType.DATA) {
            processData(message);
        } else if (message.getType() == MessageType.KEEPALIVE) {
            // 心跳包, 不处理
        } else {
            throw new NatxException("Unknown type: " + message.getType());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelGroup.close();
        System.out.println("Loss connection to Natx server, Please restart!");
    }

    /**
     * 处理在服务端注册结果
     */
    private void processRegisterResult(Message message) {
        if (message.getMetadata().isSuccess()) {
            System.out.println("Register to  Server");
        } else {
            System.out.println("Register fail: " + message.getMetadata().getDescription());
            ctx.close();
        }
    }

    /**
     * 该请求来源于，请求外网暴露的端口，外网通过netty服务端转发来到这里的
     * 请求内部代理服务，建立netty客户端，请求访问本地的服务，获取返回结果
     */
    private void processConnected(Message receiveMessage) throws Exception {

        try {
            ClientHandler thisHandler = this;
            TcpConnection localConnection = new TcpConnection();
            localConnection.connect(proxyAddress, proxyPort, new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    LocalProxyHandler localProxyHandler = new LocalProxyHandler(thisHandler,
                            receiveMessage.getMetadata().getChannelId());
                    ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(), localProxyHandler);

                    channelHandlerMap.put(receiveMessage.getMetadata().getChannelId(), localProxyHandler);
                    channelGroup.add(ch);
                }
            });
        } catch (Exception e) {
            Message message = new Message();
            message.setType(MessageType.DISCONNECTED);
            message.getMetadata().setChannelId(receiveMessage.getMetadata().getChannelId());
            ctx.writeAndFlush(message);
            channelHandlerMap.remove(receiveMessage.getMetadata().getChannelId());
            throw e;
        }
    }

    /**
     * if message.getType() == MessageType.DISCONNECTED
     */
    private void processDisconnected(Message message) {
        String channelId = message.getMetadata().getChannelId();
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
        String channelId = message.getMetadata().getChannelId();
        CommonHandler handler = channelHandlerMap.get(channelId);
        if (handler != null) {
            ChannelHandlerContext ctx = handler.getCtx();
            ctx.writeAndFlush(message.getData());
        }
    }
}
