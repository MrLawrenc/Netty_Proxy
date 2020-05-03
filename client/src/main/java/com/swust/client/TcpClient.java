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
    private static final NioEventLoopGroup WORKER_GROUP = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

    public static void connect(String host, int port, ChannelInitializer<?> channelInitializer) throws RuntimeException {
        try {
            Bootstrap b = new Bootstrap();
            b.group(WORKER_GROUP)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.ERROR))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(channelInitializer);

            ChannelFuture future = b.connect(host, port).sync();
            future.addListener((ChannelFutureListener) future1 -> {
                boolean success = future1.isSuccess();
                if (success) {
                    LogUtil.infoLog("connect {} : {} success", host, port);
                    //future1.channel().closeFuture().addListener((ChannelFutureListener) f -> WORKER_GROUP.shutdownGracefully());
                } else {
                    LogUtil.errorLog("connect {} : {} fail", host, port);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("start client fail!");
        }
    }
}