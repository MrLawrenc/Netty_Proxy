package com.swust.client;

import com.swust.common.Parent;
import com.swust.common.config.LogUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author : LiuMing
 * @date : 2019/11/4 20:00
 * @description :   内网代理客户端
 */
public class IntranetClient extends Parent {
    private NioEventLoopGroup workerGroup;

    public IntranetClient connect(String host, int port, ChannelInitializer<?> channelInitializer) {
        workerGroup = new NioEventLoopGroup(1);
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.ERROR))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(channelInitializer);

            channel = b.connect(host, port).sync().channel();
            channel.closeFuture().addListener(l -> workerGroup.shutdownGracefully());
            LogUtil.infoLog("开启内网代理客户端成功，host:{} port:{}", host, port);
            return this;
        } catch (Exception e) {
            LogUtil.errorLog("开启内网代理客户端失败，host:{} port:{}", host, port);
            workerGroup.shutdownGracefully();
            return null;
        }
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