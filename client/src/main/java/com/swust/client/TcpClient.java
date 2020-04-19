package com.swust.client;

import com.swust.common.exception.ClientException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
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

    public Channel connect(String host, int port, ChannelInitializer<?> channelInitializer) throws ClientException {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(2);
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(channelInitializer);

            Channel channel = b.connect(host, port).sync().channel();
            channel.closeFuture().addListener((ChannelFutureListener) future -> workerGroup.shutdownGracefully());
            return channel;
        } catch (Exception e) {
            workerGroup.shutdownGracefully();
            return null;
        }
    }
}