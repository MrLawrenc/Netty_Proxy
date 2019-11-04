package com.swust.client.handler;

import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageMetadata;
import com.swust.common.protocol.MessageType;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author : LiuMing
 * @date : 2019/11/4 14:05
 * @description :   外部请求到公网服务器，公网服务器将请求转发到当前服务器，当前服务器建立客户端，访问本地服务
 */
public class LocalProxyHandler extends CommonHandler {

    /**
     * 本机的netty客户端，该客户端和公网的netty服务端有一个长链接，使用该channel发送小道到公网netty服务端，
     * 之后服务端再将结果响应给外部的请求
     */
    private CommonHandler proxyHandler;
    private String remoteChannelId;

    public LocalProxyHandler(CommonHandler proxyHandler, String remoteChannelId) {
        this.proxyHandler = proxyHandler;
        this.remoteChannelId = remoteChannelId;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] data = (byte[]) msg;
        Message message = new Message();
        message.setType(MessageType.DATA);
        message.setData(data);
        message.getMetadata().setChannelId(remoteChannelId);
        proxyHandler.getCtx().writeAndFlush(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Message message = new Message();
        message.setType(MessageType.DISCONNECTED);
        message.setMetadata(new MessageMetadata().setChannelId(remoteChannelId));
        proxyHandler.getCtx().writeAndFlush(message);
    }
}
