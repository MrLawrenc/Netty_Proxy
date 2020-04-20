package com.swust.server;

import com.swust.common.Parent;
import com.swust.common.config.LogUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:37
 * @description :   外网代理
 */
public class ExtranetServer extends Parent {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public ExtranetServer initTcpServer(int port, ChannelInitializer<?> channelInitializer) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(2);
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
            LogUtil.warnLog("start fail! will close group!");
            e.printStackTrace();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
        return null;
    }

    @Override
    public void close() {
        try {
            if (channel != null) {
                channel.close().sync();
            }
        } catch (InterruptedException ignored) {
        }
    }
}
