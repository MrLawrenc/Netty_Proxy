package com.swust.client;

import com.swust.common.config.LogUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author : LiuMing
 * @date : 2019/11/4 20:00
 * @description :   tcp连接
 */
public class TcpClient {

    public static void connect(String host, int port, ChannelInitializer<?> channelInitializer) throws Exception {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(2);
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.ERROR))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(channelInitializer);

            ChannelFuture future = b.connect(host, port).sync();
            future.addListener((ChannelFutureListener) future1 -> {
                boolean success = future1.isSuccess();
                if (success) {
                    LogUtil.infoLog("连接 {} : {} 成功", host, port);
                    future1.channel().closeFuture().addListener((ChannelFutureListener) f -> workerGroup.shutdownGracefully());
                } else {
                    LogUtil.errorLog("连接 {} : {} 失败", host, port);
                    workerGroup.shutdownGracefully();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            workerGroup.shutdownGracefully();
            throw new RuntimeException("开启客户端失败!");
        }
    }
}