package com.swust.client.handler;

import com.swust.client.ClientManager;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import com.swust.common.protocol.MessageType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : LiuMing
 * 2019/11/4 14:05
 * 外部请求到公网服务器，公网服务器将请求转发到当前服务器，当前服务器建立客户端，访问本地服务
 */
@Slf4j
public class LocalProxyHandler extends ChannelInboundHandlerAdapter {

    /**
     * 本机的netty客户端，该客户端和公网的netty服务端有一个长链接，使用该channel发送消息到公网netty服务端，
     * 之后服务端再将结果响应给外部的请求
     * <br>
     * 不使用channel是减少当前handler执行链
     */
    private ChannelHandlerContext serverChannel;
    private String remoteChannelId;

    public LocalProxyHandler(ChannelHandlerContext serverChannel, String remoteChannelId) {
        this.serverChannel = serverChannel;
        this.remoteChannelId = remoteChannelId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ClientManager.ID_SERVICE_CHANNEL_MAP.put(remoteChannelId, ctx);
        ClientManager.unlock(remoteChannelId);
        //log.debug("put proxy channel id : {}", remoteChannelId);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] data = (byte[]) msg;
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.DATA);
        message.setData(data);
        header.setChannelId(remoteChannelId);
        serverChannel.writeAndFlush(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.DISCONNECTED);
        header.setChannelId(remoteChannelId);
        serverChannel.writeAndFlush(message);
    }
}
