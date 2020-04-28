package com.swust.server;

import com.swust.server.handler.RemoteProxyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:37
 * @description :   外网代理
 */
@Data
public class ExtranetServer {

    private ExtrantServerInitializer initializer = new ExtrantServerInitializer();

    private Channel channel;
    private int port;

    private ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


    public ExtranetServer initTcpServer(int port, Channel clientChannel) throws InterruptedException {
        this.port = port;
        initializer.setClientChannel(clientChannel).setProxyServer(this);
        ServerBootstrap b = new ServerBootstrap();
        b.group(TcpServer.BOSS_GROUP, TcpServer.WORKER_GROUP)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.TRACE))
                .childHandler(initializer)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        channel = b.bind(port).sync().channel();
        return this;
    }

    @Data
    @Accessors(chain = true)
    public class ExtrantServerInitializer extends ChannelInitializer<SocketChannel> {
        private Channel clientChannel;
        private ExtranetServer proxyServer;

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder());
            ch.pipeline().addLast("remoteHandler", new RemoteProxyHandler(clientChannel, proxyServer, port));
        }
    }
}
