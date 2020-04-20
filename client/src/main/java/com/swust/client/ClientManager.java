package com.swust.client;

import com.swust.common.Manager;

/**
 * @author : hz20035009-逍遥
 * @date : 2020/4/20 13:42
 * @description : TODO
 */
public class ClientManager extends Manager<IntranetClient> {
    public static final ClientManager INSTANCE = new ClientManager();
    /**
     * key 当前消息唯一标识，一般为channel id，value 为与当前内网服务端交互的代理客户端channel ctx
     */
    //public static final ConcurrentHashMap<String, ChannelHandlerContext> ID_CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * key 当前客户端channel  value 与当前客户端所关联的所有内网channel
     */
    //private static final ConcurrentHashMap<Channel, List<IntranetClient>> CHANNEL_MAP = new ConcurrentHashMap<>();

    /*public static void add2ChannelMap(Channel key, IntranetClient target) {
        List<IntranetClient> channels = CHANNEL_MAP.get(key);
        if (Objects.isNull(channels)) {
            channels = new ArrayList<>(8);
        }
        channels.add(target);
    }

    public static void removeChannelMap(Channel key) {
        CHANNEL_MAP.remove(key).forEach(IntranetClient::close);
    }

    public static void removeChannelByTarget(String channelId, Channel target) {
        ID_CHANNEL_MAP.remove(channelId);
        CHANNEL_MAP.values().forEach(value -> {
            Iterator<IntranetClient> iterator = value.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getChannel().equals(target)) {
                    iterator.remove();
                    return;
                }
            }
        });
    }*/
}