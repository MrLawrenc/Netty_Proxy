package com.swust.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : LiuMing
 * 2019/11/4 10:37
 * Tcp服务端
 */
@Getter
@Slf4j
public class TcpServer {

    public Channel initTcpServer(int port, ChannelInitializer<?> channelInitializer) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup work = new NioEventLoopGroup();
        bootstrap.group(boss, work)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.TRACE))
                .childHandler(channelInitializer)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture future = bootstrap.bind(port).sync();
        future.addListener(fu -> {
            if (fu.isSuccess()) {
                log.info("Server  started on port {}!", port);
            } else {
                log.error("Server start fail! will close current service!");
                System.exit(0);
            }
        });
        Channel channel = future.channel();
        channel.closeFuture().addListener((ChannelFutureListener) f -> {
            work.shutdownGracefully();
            boss.shutdownGracefully();
            ServerManager.PROXY_BOSS_GROUP.shutdownGracefully();
            ServerManager.PROXY_WORKER_GROUP.shutdownGracefully();
        });
        return channel;
    }

}
