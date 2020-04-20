package com.swust.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : hz20035009-逍遥
 * @date : 2020/4/20 16:37
 * @description :
 */
public class Manager<T extends Parent> {
    public static final ConcurrentHashMap<String, ChannelHandlerContext> ID_CHANNEL_MAP = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Channel, List<T>> CHANNEL_MAP = new ConcurrentHashMap<>();

    public void add2ChannelMap(Channel key, T target) {
        List<T> channels = CHANNEL_MAP.get(key);
        if (Objects.isNull(channels)) {
            channels = new ArrayList<>(8);
        }
        channels.add(target);
    }

    public void removeChannelMap(Channel key) {
        CHANNEL_MAP.remove(key).forEach(T::close);
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