package com.swust.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : hz20035009-逍遥
 * @date : 2020/4/20 16:37
 * @description :
 */
public class Manager<T extends Parent> {
    public static final ConcurrentHashMap<String, ChannelHandlerContext> ID_CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * 后期支持一对多.如：内网启动一个客户端，要求服务端开启多个代理服务端请求。
     */
    private final ConcurrentHashMap<Channel, List<T>> CHANNEL_MAP = new ConcurrentHashMap<>();

    public void add2ChannelMap(Channel key, T target) {
        List<T> channels = CHANNEL_MAP.get(key);
        if (Objects.isNull(channels)) {
            channels = new ArrayList<>(8);
        }
        channels.add(target);
    }

    public T hasServer4ChannelMap(Channel key, int port) {
        List<T> list = CHANNEL_MAP.get(key);
        return list == null || list.size() == 0 ? null : list.stream().filter(t -> t.getPort() == port).findFirst().orElse(null);
    }


    public void removeChannelMap(Channel key) {
        Optional.ofNullable(CHANNEL_MAP.remove(key)).ifPresent(r -> r.forEach(T::close));
    }

    public void removeChannelByTarget(String channelId, Channel target) {
        ID_CHANNEL_MAP.remove(channelId);
        CHANNEL_MAP.values().forEach(value -> {
            Iterator<T> iterator = value.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getChannel().equals(target)) {
                    iterator.remove();
                    return;
                }
            }
        });
    }
}