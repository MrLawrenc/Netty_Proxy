package com.swust.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : hz20035009-逍遥
 * 2020/4/20 13:32
 */
@Slf4j
public final class ServerManager {
    /**
     * 全局的boss group
     */
    public static final EventLoopGroup PROXY_BOSS_GROUP = new NioEventLoopGroup(2,
            new DefaultThreadFactory("server-boss"));
    /**
     * 全局的work group
     */
    public static final EventLoopGroup PROXY_WORKER_GROUP = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() << 1
            , new DefaultThreadFactory("server-work"));


    /**
     * 与当前外网代理服务端连接的用户客户端channel，使用其channel id作为msg的头信息进行传递
     */
    public static final Map<String, ChannelHandlerContext> USER_CLIENT_MAP = new ConcurrentHashMap<>();


    /**
     * 所有已开启的代理服务端
     */
    private static final List<ExtranetServer> PROXY_SERVER_LIST = Collections.synchronizedList(new ArrayList<>());


    /**
     * 判断当前端口的代理服务端是否存在且存活
     *
     * @param port 代理端口
     * @return 是否存在且存活
     */
    public static boolean alreadyExists(ChannelHandlerContext ctx, int port) {
        for (ExtranetServer extranetServer : PROXY_SERVER_LIST) {
            if (extranetServer.getPort() == port && extranetServer.getChannel().isActive()) {
                extranetServer.getInitializer().getRemoteProxyHandler().setClientCtx(ctx);
                log.info("the proxy server that exists and opens the port({}) will update the binding relationship with the intranet client", port);
                return true;
            }
        }
        return false;
    }

    public static void addProxyServer(ExtranetServer extranetServer) {
        PROXY_SERVER_LIST.add(extranetServer);
    }



}