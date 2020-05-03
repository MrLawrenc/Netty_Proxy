package com.swust.client;

import com.swust.client.handler.LocalProxyHandler;
import com.swust.common.config.LogUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import lombok.Getter;

/**
 * @author : LiuMing
 * @date : 2019/11/4 20:00
 * @description :   内网代理客户端
 */
@Getter
public class IntranetClient {

    private static final NioEventLoopGroup WORK = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
    private Channel channel;

    /**
     * 开启内网代理客户端，禁用sync()，否则可能死锁
     *
     * @param host          内网代理客户端的host
     * @param port          内网代理客户端的port
     * @param serverChannel 与服务端交互的channel
     * @param channelId     外网代理服务端的channel id
     * @return 内网代理客户端channel
     */
    public IntranetClient connect(String host, int port, ChannelHandlerContext serverChannel, String channelId) throws InterruptedException {
        Bootstrap b = new Bootstrap();
        b.group(WORK)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        LocalProxyHandler localProxyHandler = new LocalProxyHandler(serverChannel, channelId);
                        ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(), localProxyHandler);
                    }
                });
        ChannelFuture future = b.connect(host, port).sync();
        this.channel = future.channel();
        future.addListener(f -> {
            if (f.isSuccess()) {
                LogUtil.infoLog("Start client proxy success，host:{} port:{}", host, port);
            } else {
                LogUtil.errorLog("Start client proxy fail，host:{} port:{}", host, port);
            }
        });
        return this;
    }
}