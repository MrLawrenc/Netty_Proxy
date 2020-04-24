package com.swust.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : hz20035009-逍遥
 * @date : 2020/4/20 13:42
 * @description : TODO
 */
public class ClientManager {
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
    public  static void clean(){
        ID_SERVICE_CHANNEL_MAP.clear();
        CHANNEL_MAP.clear();
    }

}