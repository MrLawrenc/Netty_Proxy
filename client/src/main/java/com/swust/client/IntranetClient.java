package com.swust.client;

import com.swust.client.handler.ClientHandler;
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
     * 开启内网代理客户端
     * <br>
     * fix(2020-05-03) :
     * 复现：通过方法使用channel.eventLoop()获取客户端的NioEventLoopGroup,并作为代理客户端的group，此时是不能使用sync阻塞的，所以出现了每次
     * 通过外网ssh代理连接内网ssh服务的时候，会出现连接被重置的错误
     * 原因：在{@link com.swust.client.handler.ClientHandler#processData}获取外网数据包的时候，实际内网的代理客户端还未开启成功
     * 解决：1）和客户端的线程组分开使用，且将开启内网代理客户端的方法改为sync。2）若还想共用客户端的group，则必须在processData方法写数据之前
     * 连接上内网代理客户端（可以在processData阻塞，知道内网代理开启成功）
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
            ClientHandler.readyOpenClient.remove(channelId);
        });
        return this;
    }
}