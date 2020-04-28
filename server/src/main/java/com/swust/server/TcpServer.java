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
    public static final EventLoopGroup BOSS_GROUP = new NioEventLoopGroup(1);
    public static final EventLoopGroup WORKER_GROUP = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());

    public Channel initTcpServer(int port, ChannelInitializer<?> channelInitializer) {

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(BOSS_GROUP, WORKER_GROUP)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.TRACE))
                    .childHandler(channelInitializer)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture channelFuture = b.bind(port).sync();
            Channel channel = channelFuture.channel();
            channel.closeFuture().addListener((ChannelFutureListener) future -> {
                WORKER_GROUP.shutdownGracefully();
                BOSS_GROUP.shutdownGracefully();
            });
            return channel;
        } catch (Exception e) {
            LogUtil.warnLog("server start fail! will close group!");
            WORKER_GROUP.shutdownGracefully();
            BOSS_GROUP.shutdownGracefully();
            throw new RuntimeException("启动服务端失败！");
        }
    }

}
