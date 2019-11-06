package com.swust.server.handler;

import com.swust.server.TcpServer;
import com.swust.common.exception.Exception;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws java.lang.Exception {
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
                // 心跳包
                lossConnectCount = 0;
                System.out.println("心跳包............");
            } else {
                throw new Exception("Unknown type: " + message.getType());
            }
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        remoteConnectionServer.close();
        if (hasRegister) {
            System.out.println("Stop server on port: " + port);
        }
    }

    /**
     * 处理客户端注册,每个客户端注册成功都会启动一个服务，绑定客户端指定的端口
     */
    private void processRegister(Message message) {
        String password = message.getMetadata().getPassword();

        if (this.password == null || !this.password.equals(password)) {
            message.getMetadata().setSuccess(false).setDescription("Token is wrong");
        } else {
            //客户端指定对外开放的端口
            int port = message.getMetadata().getOpenTcpPort();
            try {
                TcpServerHandler thisHandler = this;
                remoteConnectionServer.initTcpServer(port, new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(),
                                new RemoteProxyHandler(thisHandler));
                        channels.add(ch);
                    }
                });

                message.getMetadata().setSuccess(true);
                this.port = port;
                hasRegister = true;
                System.out.println("Register success, start server on port: " + port);
            } catch (java.lang.Exception e) {
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
     * 断开
     */
    private void processDisconnected(Message message) {
        channels.close(channel -> channel.id().asLongText().equals(message.getMetadata().getChannelId()));
    }
}
