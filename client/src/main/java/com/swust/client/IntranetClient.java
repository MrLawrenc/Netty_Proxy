package com.swust.client;

import com.swust.client.handler.LocalProxyHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : LiuMing
 * @date : 2019/11/4 20:00
 * 内网代理客户端
 */
@Getter
@Slf4j
public class IntranetClient {


    private Channel channel;


    /**
     * 开启内网代理客户端
     * <br>
     * fix(2020-05-03) :
     * 复现：通过方法使用channel.eventLoop()获取客户端的NioEventLoopGroup,并作为代理客户端的group，此时是不能使用sync阻塞的，所以出现了每次
     * 通过外网ssh代理连接内网ssh服务的时候，会出现连接被重置的错误
     * 原因：在{@link com.swust.client.handler.ClientHandler#processData}获取外网数据包的时候，实际内网的代理客户端还未开启成功
     * 解决：1）和客户端的线程组分开使用，且将开启内网代理客户端的方法改为sync。2）若还想共用客户端的group，则必须在processData方法写数据之前
     * 连接上内网代理客户端（可以在processData阻塞，直到内网代理开启成功）
     * <p>
     * 2020-08-06 注意 共用eventLoop适用于代理只转发不接受响应的情况
     *
     * @param host          内网代理客户端的host
     * @param port          内网代理客户端的port
     * @param serverChannel 与服务端交互的channel
     * @param channelId     外网代理服务端的channel id
     * @return 内网代理客户端channel
     */
    public IntranetClient connect(String host, int port, ChannelHandlerContext serverChannel, String channelId, EventLoop eventLoop) throws InterruptedException {
        Bootstrap b = new Bootstrap();
//        b.group(ClientManager.PROXY_WORK)
        b.group(eventLoop)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        LocalProxyHandler localProxyHandler = new LocalProxyHandler(serverChannel, channelId);
                        ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder(), localProxyHandler);
                    }
                });
        ChannelFuture future = b.connect(host, port);
        this.channel = future.channel();
        future.addListener(f -> {
            if (f.isSuccess()) {
                log.debug("connect {}:{} success", host, port);
            } else {
                log.error("connect client proxy fail，host:{} port:{}", host, port);
            }
        });
        return this;
    }
}