package com.swust.server.handler;

import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import com.swust.common.protocol.MessageType;
import com.swust.server.ServerManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : LiuMing
 * 2019/11/4 13:54
 * 代理服务器的handler，当请求公网暴露的代理端口时，会转发到相应的客户端，
 * fix:若启用多客户端，且绑定的代理服务为同一个，@ChannelHandler.Sharable注解会导致clientCtx被覆盖
 */

@Getter
@Setter
@Slf4j
@ChannelHandler.Sharable
public class RemoteProxyHandler extends ChannelInboundHandlerAdapter {
    private ChannelHandlerContext clientCtx;
    private final int port;

    /**
     * 外部请求外网代理的端口时调用，保存的服务端channel会给内网客户端发送消息 proxyHandler.getCtx().writeAndFlush(message);
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String channelId = ctx.channel().id().asLongText();
        ServerManager.USER_CLIENT_MAP.put(channelId, ctx);
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.CONNECTED);
        header.setOpenTcpPort(port);
        header.setChannelId(channelId);
        clientCtx.writeAndFlush(message);
    }

    public RemoteProxyHandler(ChannelHandlerContext clientCtx, int port) {
        this.clientCtx = clientCtx;
        this.port = port;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] data = (byte[]) msg;
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.DATA);
        message.setData(data);
        header.setChannelId(ctx.channel().id().asLongText());
        clientCtx.writeAndFlush(message);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asLongText();
        ServerManager.USER_CLIENT_MAP.remove(channelId, ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

}
