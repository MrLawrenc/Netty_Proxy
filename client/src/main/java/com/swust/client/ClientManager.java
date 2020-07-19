package com.swust.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : hz20035009-逍遥
 * 2020/4/20 13:42
 */
public class ClientManager {

    public static NioEventLoopGroup WORK = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() << 1,
            new DefaultThreadFactory("client-work"));
    public static NioEventLoopGroup PROXY_WORK = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() << 2,
            new DefaultThreadFactory("client-proxy-work"));
    /**
     * 锁集合，主要防止代理服务在高并发环境下出现内网代理客户端还未连接上，但是外网数据包已经到了，这时会出现匹配不到对应得内网客户端
     */
    public static final Map<String, Object> MONITOR_MAP = new ConcurrentHashMap<>();

    /**
     * key 与外网代理服务端连接的channelid
     * value 连接到内网开启的服务端的客户端channel
     */
    public static final Map<String, ChannelHandlerContext> ID_SERVICE_CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * key 为与外网服务端连接的channel
     * value 内网客户端开启的内网客户端，会连接到指定的内网服务，可以是多个客户端
     */
    public static final ConcurrentHashMap<Channel, List<IntranetClient>> CHANNEL_MAP = new ConcurrentHashMap<>();


    public static void lock(String channelId) {
        Object monitor = new Object();
        MONITOR_MAP.put(channelId, monitor);
        synchronized (monitor) {
            try {
                monitor.wait(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void unlock(String channelId) {
        Object monitor = MONITOR_MAP.get(channelId);
        if (Objects.isNull(monitor)) {
            return;
        }
        synchronized (monitor) {
            monitor.notify();
        }
        MONITOR_MAP.remove(channelId);
    }

    public static void add2ChannelMap(Channel key, IntranetClient target) {
        List<IntranetClient> channels = CHANNEL_MAP.get(key);
        if (Objects.isNull(channels)) {
            channels = new ArrayList<>(16);
        }
        channels.add(target);
    }

    /**
     * 根据channel id，移除channel映射
     */
    public static void removeChannelMapByProxyClient(Channel channel, String channelId) {
        List<IntranetClient> intranetClients = CHANNEL_MAP.get(channel);
        if (Objects.nonNull(intranetClients)) {
            intranetClients.removeIf(intranetClient -> intranetClient.getChannel().id().asLongText().equals(channelId));
        }
    }

    /**
     * 重置容器
     */
    public static void reset() {
    }

}