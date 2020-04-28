package com.swust.client;

import com.swust.common.config.LogUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
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

    private Channel channel;

    public IntranetClient connect(String host, int port, ChannelInitializer<?> channelInitializer) throws InterruptedException {
        try {
            Bootstrap b = new Bootstrap();
            b.group(TcpClient.WORKER_GROUP)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.ERROR))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(channelInitializer);
            channel = b.connect(host, port).sync().channel();
            LogUtil.infoLog("start client proxy success，host:{} port:{}", host, port);
            return this;
        } catch (Exception e) {
            LogUtil.errorLog("start client proxy fail，host:{} port:{}", host, port);
            throw e;
        }
    }
}