package com.swust.client;

import com.swust.common.config.LogUtil;
import com.swust.common.exception.ClientException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;

/**
 * @author : LiuMing
 * @date : 2019/11/4 20:00
 * @description :   内网代理客户端
 */
@Getter
public class IntranetClient {
    NioEventLoopGroup workerGroup;
    Channel channel;

    public IntranetClient connect(String host, int port, ChannelInitializer<?> channelInitializer) throws ClientException {
        workerGroup = new NioEventLoopGroup(1);
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(channelInitializer);

            channel = b.connect(host, port).sync().channel();
            LogUtil.infoLog("开启内网代理客户端成功，host:{} port:{}", host, port);
            return this;
        } catch (Exception e) {
            LogUtil.errorLog("开启内网代理客户端失败，host:{} port:{}", host, port);
            workerGroup.shutdownGracefully();
            return null;
        }
    }

    public void close() throws InterruptedException {
        if (channel != null) {
            channel.close().sync();
        }
        workerGroup.shutdownGracefully();
    }
}