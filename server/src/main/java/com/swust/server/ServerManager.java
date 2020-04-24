package com.swust.server;

import com.swust.common.protocol.Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : hz20035009-逍遥
 * @date : 2020/4/20 13:32
 * @description : 工具类
 */
public final class ServerManager {

    /**
     * 与当前外网代理服务端连接的用户客户端channel，使用其channel id作为msg的头信息进行传递
     */
    public static final List<ChannelHandlerContext> USER_CLIENT_CHANNEL = Collections.synchronizedList(new ArrayList<>());

    /**
     * key 为内网客户端
     * value 内网客户端要求开启的外网代理服务端，可以是多个
     */
    public static final ConcurrentHashMap<Channel, List<ExtranetServer>> CHANNEL_MAP = new ConcurrentHashMap<>();


    /**
     * 将代理服务端绑定到当前客户端
     */
    public static void add2ChannelMap(Channel key, ExtranetServer target) {
        List<ExtranetServer> channels = CHANNEL_MAP.get(key);
        if (Objects.isNull(channels)) {
            channels = new ArrayList<>(16);
        }
        channels.add(target);
        CHANNEL_MAP.put(key, channels);
    }


    /**
     * 根据msg的头id，查找到对应的channel
     */
    public static ChannelHandlerContext findChannelByMsg(Message message) {
        return USER_CLIENT_CHANNEL.stream().filter(channel -> channel.channel().id().asLongText().equals(message.getHeader().getChannelId()))
                .findAny().orElse(null);
    }


    /**
     * 端口和服务端映射关系
     */
    public static final Map<Integer, ExtranetServer> PORT_MAP = new HashMap<>();

    /**
     * 判断是否存在与当前客户端绑定的代理服务端
     */
    public static ExtranetServer hasServer4ChannelMap(Channel key, int port) {
        List<ExtranetServer> list = CHANNEL_MAP.get(key);
        return list == null || list.size() == 0 ? null : list.stream().filter(t -> t.getPort() == port).findFirst().orElse(null);
    }

}