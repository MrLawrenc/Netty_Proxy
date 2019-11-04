package com.swust.server.handler;

import com.swust.server.net.TcpServer;
import com.xxg.natx.common.exception.NatxException;
import com.xxg.natx.common.handler.CommonHandler;
import com.xxg.natx.common.protocol.Message;
import com.xxg.natx.common.protocol.MessageType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;

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

    private boolean hasRegister = false;

    public TcpServerHandler(String password) {
        this.password = password;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        Message message = (Message) msg;
        //客户端注册
        if (message.getType() == MessageType.REGISTER) {
            processRegister(message);
        } else if (hasRegister) {
            if (message.getType() == MessageType.DISCONNECTED) {
                processDisconnected(message);
            } else if (message.getType() == MessageType.DATA) {
                processData(message);
            } else if (message.getType() == MessageType.KEEPALIVE) {
                System.out.println("心跳包............");
                // 心跳包, 不处理
            } else {
                throw new NatxException("Unknown type: " + message.getType());
            }
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        remoteConnectionServer.close();
        if (hasRegister) {
            System.out.println("Stop server on port: " + port);
        }
    }

    /**
     * 处理客户端注册
     */
    private void processRegister(Message message) {
        String password = message.getMetadata().getPassword();

        if (this.password == null || !this.password.equals(password)) {
            message.getMetadata().setSuccess(false).setDescription("Token is wrong");
        } else {
            int port = message.getMetadata().getOpenTcpPort();
            try {
                TcpServerHandler thisHandler = this;
                remoteConnectionServer.initTcpServer(port, new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(),
                                new RemoteProxyHandler(thisHandler));
                        channels.add(ch);
                    }
                });

                message.getMetadata().setSuccess(true);
                this.port = port;
                hasRegister = true;
                System.out.println("Register success, start server on port: " + port);
            } catch (Exception e) {
                message.getMetadata().setSuccess(false).setDescription(e.getMessage());
                e.printStackTrace();
            }
        }
        message.setType(MessageType.REGISTER_RESULT);
        ctx.writeAndFlush(message);

        if (!hasRegister) {
            System.out.println("Client register error: " + message.getMetadata().getDescription());
            ctx.close();
        }
    }

    /**
     * 处理收到转发的内网响应数据包
     */
    private void processData(Message message) {
        channels.writeAndFlush(message.getData(), channel ->
                channel.id().asLongText().equals(message.getMetadata().getChannelId()));
    }

    /**
     * if message.getType() == MessageType.DISCONNECTED
     *
     * @param message
     */
    private void processDisconnected(Message message) {
        channels.close(channel -> channel.id().asLongText().equals(message.getMetadata().getChannelId()));
    }
}
