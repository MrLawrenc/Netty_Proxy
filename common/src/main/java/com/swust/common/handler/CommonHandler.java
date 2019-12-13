package com.swust.common.handler;

import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Getter;

/**
 * @author : LiuMing
 * @date : 2019/11/4 14:54
 * @description :   公共handler
 */
public class CommonHandler extends ChannelInboundHandlerAdapter {
    protected int lossConnectCount = 0;
    @Getter
    protected ChannelHandlerContext ctx;
    /**
     * 默认读超时上限
     */
    private static final byte DEFAULT_RECONNECTION_LIMIT = 2;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println(String.format("client/server exception(%s) ...............", cause.getMessage()));
        cause.printStackTrace();
        Channel channel = ctx.channel();
        if (!channel.isActive()) {
            System.out.println("############### -- 客户端 -- " + channel.remoteAddress() + "  断开了连接！");
            ctx.close();
        } else {
            ctx.fireExceptionCaught(cause);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                System.out.println(ctx.channel().remoteAddress() + " Read idle ");
                lossConnectCount++;
                if (lossConnectCount > DEFAULT_RECONNECTION_LIMIT) {
                    System.out.println("Read idle  will loss connection.");
                    ctx.close();
                }
            } else if (e.state() == IdleState.WRITER_IDLE) {
                System.out.println("Write idle will write again.");
                Message message = new Message();
                message.getHeader().setType(MessageType.KEEPALIVE);
                ctx.writeAndFlush(message);
            }
        }
    }
}
