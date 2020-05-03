package com.swust.server;

import com.swust.common.config.LogUtil;
import com.swust.server.handler.RemoteProxyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Data;
import lombok.Setter;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:37
 * @description :   外网代理
 */
@Data
public class ExtranetServer {
    private ExtrantServerInitializer initializer;

    /**
     * 当前代理的serverChannel
     */
    private Channel channel;
    private int port;

    private ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


    public ExtranetServer initTcpServer(int port, ChannelHandlerContext clientCtx) {
        this.port = port;
        this.initializer = new ExtrantServerInitializer(clientCtx, this);
        ServerBootstrap b = new ServerBootstrap();
        b.group(ServerManager.PROXY_BOSS_GROUP, ServerManager.PROXY_WORKER_GROUP)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.TRACE))
                .childHandler(initializer)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture future = b.bind(port);
        future.addListener(f -> {
            if (f.isSuccess()) {
                LogUtil.infoLog("Register success, start server on port: {}", port);
            } else {
                LogUtil.errorLog(" Start proxy server on port:{}  fail! ", port);
            }
        });
        this.channel = future.channel();
        return this;
    }

    /**
     * 外网代理服务端的initializer
     */
    public class ExtrantServerInitializer extends ChannelInitializer<SocketChannel> {
        @Setter
        private ChannelHandlerContext clientCtx;
        private ExtranetServer proxyServer;

        public ExtrantServerInitializer(ChannelHandlerContext clientCtx, ExtranetServer proxyServer) {
            this.clientCtx = clientCtx;
            this.proxyServer = proxyServer;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new ByteArrayDecoder(), new ByteArrayEncoder());
            ch.pipeline().addLast("remoteHandler", new RemoteProxyHandler(clientCtx, proxyServer, port));
        }
    }
}
