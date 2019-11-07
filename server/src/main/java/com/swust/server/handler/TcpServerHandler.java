package com.swust.server.handler;

import com.alibaba.fastjson.JSON;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
import com.swust.server.TcpServer;
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
        if (!(msg instanceof Message)) {
            throw new Exception("Unknown message: " + JSON.toJSONString(msg));
        }
        Message message = (Message) msg;
        MessageType type = message.getHeader().getType();
        //客户端注册
        if (type == MessageType.REGISTER) {
            processRegister(message);
        } else if (hasRegister) {
            if (type == MessageType.DISCONNECTED) {
                processDisconnected(message);
            } else if (type == MessageType.DATA) {
                processData(message);
            } else if (type == MessageType.KEEPALIVE) {
                // 心跳包
                lossConnectCount = 0;
                System.out.println("心跳包............");
            } else {
                throw new Exception("Unknown type: " + type);
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
        String password = message.getHeader().getPassword();

        if (this.password == null || !this.password.equals(password)) {
            message.getHeader().setSuccess(false).setDescription("Token is wrong");
        } else {
            //客户端指定对外开放的端口
            int port = message.getHeader().getOpenTcpPort();
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

                message.getHeader().setSuccess(true);
                this.port = port;
                hasRegister = true;
                System.out.println("Register success, start server on port: " + port);
            } catch (java.lang.Exception e) {
                message.getHeader().setSuccess(false).setDescription(e.getMessage());
                e.printStackTrace();
            }
        }
        message.getHeader().setType(MessageType.REGISTER_RESULT);
        ctx.writeAndFlush(message);

        if (!hasRegister) {
            System.out.println("Client register error: " + message.getHeader().getDescription());
            ctx.close();
        }
    }

    /**
     * 处理收到转发的内网响应数据包
     */
    private void processData(Message message) {
        channels.writeAndFlush(message.getData(), channel ->
                channel.id().asLongText().equals(message.getHeader().getChannelId()));
    }

    /**
     * 断开
     */
    private void processDisconnected(Message message) {
        channels.close(channel -> channel.id().asLongText().equals(message.getHeader().getChannelId()));
    }
}
