package com.swust.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Data;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:37
 * @description :   外网代理
 */
@Data
public class ExtranetServer {
    public static Channel clientChannel;

    private Channel channel;
    private int port;

    public ExtranetServer initTcpServer(Channel clientChannel, int port, ChannelInitializer<?> channelInitializer) {
        ExtranetServer.clientChannel = clientChannel;
        this.port = port;
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(2);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.TRACE))
                    .childHandler(channelInitializer)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            channel = b.bind(port).sync().channel();
            channel.closeFuture().addListener(l -> {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            });
            return this;
        } catch (Exception e) {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            throw new RuntimeException(e);
        }
    }
}
