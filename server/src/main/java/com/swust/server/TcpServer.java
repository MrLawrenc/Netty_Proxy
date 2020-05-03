package com.swust.server;

import com.swust.common.config.LogUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:37
 * @description :   Tcp服务端
 */
@Getter
public class TcpServer {

    public Channel initTcpServer(int port, ChannelInitializer<?> channelInitializer) throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup work = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        b.group(boss, work)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.TRACE))
                .childHandler(channelInitializer)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture future = b.bind(port).sync();
        future.addListener(fu -> {
            if (fu.isSuccess()) {
                LogUtil.warnLog("Server start success!");
            } else {
                LogUtil.warnLog("Server start fail! will close current service!");
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
