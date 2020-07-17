package com.swust.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : LiuMing
 * 2019/11/4 20:00
 * tcp连接
 */
@Slf4j
public class TcpClient {

    public static void connect(String host, int port, ChannelInitializer<?> channelInitializer) throws RuntimeException {
        Bootstrap b = new Bootstrap();
        System.out.println("Runtime.getRuntime().availableProcessors():"+Runtime.getRuntime().availableProcessors());
        NioEventLoopGroup work = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setNameFormat("netty-client-%d").build());
        try {
            b.group(work)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(channelInitializer);

            ChannelFuture future = b.connect(host, port).sync();
            future.addListener((ChannelFutureListener) future1 -> {
                boolean success = future1.isSuccess();
                if (success) {
                    log.info("connect {} : {} success", host, port);
                } else {
                    log.error("connect {} : {} fail", host, port);
                }
            });

            future.channel().closeFuture().addListener(f -> work.shutdownGracefully());
        } catch (Exception e) {
            work.shutdownGracefully();
            throw new RuntimeException("start client fail!", e);
        }
    }
}