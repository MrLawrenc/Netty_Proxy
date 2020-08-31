package com.swust.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : hz20035009-逍遥
 * 2020/4/20 13:42
 */
@Slf4j
public class ClientManager {


    public static NioEventLoopGroup PROXY_WORK = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() << 2,
            new DefaultThreadFactory("client-proxy-work"));
    /**
     * 锁集合，主要防止代理服务在高并发环境下出现内网代理客户端还未连接上，但是外网数据包已经到了，这时会出现匹配不到对应得内网客户端
     */
    public static final Map<String, InnerLock> MONITOR_MAP = new ConcurrentHashMap<>();

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


    /**
     * key 开启的内网channel id
     * value timestamp
     */
    public static final ConcurrentHashMap<String, Long> CHANNEL_TIME_MAP = new ConcurrentHashMap<>();
    /**
     * lock
     */
    private final static Object MONITOR = new Object();

    public static void lock(final String channelId) {
        synchronized (MONITOR) {
            System.out.println("lock:" + channelId);
            InnerLock innerLock = MONITOR_MAP.get(channelId);
            if (Objects.nonNull(innerLock) && innerLock.success) {
                return;
            }
            //2s
            long waitTime = 2000;
            CHANNEL_TIME_MAP.put(channelId, System.currentTimeMillis());
            while (true) {
                try {
                    MONITOR.wait(waitTime);
                    InnerLock newLock = MONITOR_MAP.get(channelId);
                    if (Objects.nonNull(newLock) && newLock.success) {
                        break;
                    }

                    if (System.currentTimeMillis() - CHANNEL_TIME_MAP.get(channelId) > 1000 * 60) {
                        System.out.println("channel id ：" + channelId + " 的数据超过1min未被处理将被丢弃");
                        break;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            CHANNEL_TIME_MAP.remove(channelId);
        }

    }

    public static void unlock(final String channelId) {
        synchronized (MONITOR) {
            MONITOR_MAP.put(channelId, new InnerLock().setDateTime(LocalDateTime.now()).setSuccess(true));
            MONITOR.notifyAll();
        }
    }

    public static void add2ChannelMap(Channel key, IntranetClient target) {
        List<IntranetClient> channels = CHANNEL_MAP.get(key);
        if (Objects.isNull(channels)) {
            channels = Collections.synchronizedList(new ArrayList<>(16));
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

    @Accessors(chain = true)
    @Setter
    @Getter
    private static class InnerLock {
        private boolean success;
        private boolean dataFast;
        private LocalDateTime dateTime;
    }
}