package com.swust.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : hz20035009-逍遥
 * @date : 2020/4/20 13:32
 * @description : TODO
 */
public final class ServerManager {

    /**
     * 端口和服务端映射关系
     */
    public static final Map<Integer, ExtranetServer> PORT_MAP = new HashMap<>();
    /**
     * key 当前消息唯一标识，一般为channel id，value 为与当前外网代理的服务端交互的channel ctx
     */
    public static final ConcurrentHashMap<String, ChannelHandlerContext> ID_CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * 后期支持一对多.如：内网启动一个客户端，要求服务端开启多个代理服务端请求。
     */
    public static final ConcurrentHashMap<Channel, List<ExtranetServer>> CHANNEL_MAP = new ConcurrentHashMap<>();


    public static void add2ChannelMap(Channel key, ExtranetServer target) {
        List<ExtranetServer> channels = CHANNEL_MAP.get(key);
        if (Objects.isNull(channels)) {
            channels = new ArrayList<>(8);
        }
        channels.add(target);
    }

    public static void removeIdChannelMap(String key) {
        ID_CHANNEL_MAP.remove(key).close();
    }

    public static ExtranetServer hasServer4ChannelMap(Channel key, int port) {
        List<ExtranetServer> list = CHANNEL_MAP.get(key);
        return list == null || list.size() == 0 ? null : list.stream().filter(t -> t.getPort() == port).findFirst().orElse(null);
    }

}