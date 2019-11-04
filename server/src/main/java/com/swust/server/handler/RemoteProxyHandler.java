package com.swust.server.handler;

import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageMetadata;
import com.swust.common.protocol.MessageType;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author : LiuMing
 * @date : 2019/11/4 13:54
 * @description :   代理服务器的handler，当请求公网暴露的代理端口时，会转发到相应的客户端，
 */
public class RemoteProxyHandler extends CommonHandler {

    /**
     * 当前的netty服务端，转发请求，将来自外网的请求转发到内网，将来自内网的响应响应给外部客户端
     */
    private CommonHandler proxyHandler;

    public RemoteProxyHandler(CommonHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }

    /**
     * 外部请求外网代理的端口时调用
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Message message = new Message();
        message.setType(MessageType.CONNECTED);
        message.getMetadata().setChannelId(ctx.channel().id().asLongText());
        proxyHandler.getCtx().writeAndFlush(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Message message = new Message();
        message.setType(MessageType.DISCONNECTED);
        MessageMetadata metadata = new MessageMetadata().setChannelId(ctx.channel().id().asLongText());
        message.setMetadata(metadata);
        proxyHandler.getCtx().writeAndFlush(message);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] data = (byte[]) msg;
        Message message = new Message();
        message.setType(MessageType.DATA);
        message.setData(data);
        message.getMetadata().setChannelId(ctx.channel().id().asLongText());
        proxyHandler.getCtx().writeAndFlush(message);
    }
}
