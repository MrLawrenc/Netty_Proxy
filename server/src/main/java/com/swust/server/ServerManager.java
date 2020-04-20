package com.swust.server;

import com.swust.common.Manager;

/**
 * @author : hz20035009-逍遥
 * @date : 2020/4/20 13:32
 * @description : TODO
 */
public final class ServerManager extends Manager<ExtranetServer> {
    public static final ServerManager INSTANCE = new ServerManager();


    /**
     * key 当前消息唯一标识，一般为channel id，value 为与当前外网代理的服务端交互的channel ctx
     */
 /*   public static final ConcurrentHashMap<String, ChannelHandlerContext> ID_CHANNEL_MAP = new ConcurrentHashMap<>();


    private static final ConcurrentHashMap<Channel, List<ExtranetServer>> CHANNEL_MAP = new ConcurrentHashMap<>();*/
    public void removeIdChannelMap(String key) {
        ID_CHANNEL_MAP.remove(key).close();
    }


}