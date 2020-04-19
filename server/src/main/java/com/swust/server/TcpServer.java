package com.swust.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.logging.Logger;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:37
 * @description :   Tcp服务端
 */
public class TcpServer {
    protected Logger logger = Logger.getGlobal();
    private Channel tcpChannel;

    /**
     * tcp服务端初始化
     */
    public boolean initTcpServer(int port, ChannelInitializer<?> channelInitializer) {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(channelInitializer)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture channelFuture = b.bind(port).sync();
            tcpChannel = channelFuture.channel();
            tcpChannel.closeFuture().addListener((ChannelFutureListener) future -> {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            });
        } catch (Exception e) {
            logger.warning("start fail! will close group!");
            e.printStackTrace();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            return false;
        }
        return true;
    }

    /**
     * 关闭tcp服务端
     */
    public void close() {
        if (tcpChannel != null) {
            tcpChannel.close();
        }
    }
}
