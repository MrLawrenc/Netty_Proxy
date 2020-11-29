package com.swust.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : MrLawrenc
 * date  2020/10/5 17:00
 * 国庆期间搞下netty
 */
@Slf4j
public class FileSyncClient {

    public void connectFileServer(String host, int port) throws InterruptedException {
        NioEventLoopGroup work = new NioEventLoopGroup(2, new DefaultThreadFactory("main-client-work"));
        Bootstrap b = new Bootstrap();
        try {
            b.group(work).channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast("fileSyncHandler", new FileSyncClientHandler());
                        }
                    });

            ChannelFuture future = b.connect(host, port).sync();
            future.addListener((ChannelFutureListener) future1 -> {
                boolean success = future1.isSuccess();
            });
            future.channel().closeFuture().sync();
        } finally {
            work.shutdownGracefully();
        }

    }
}