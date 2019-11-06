package com.swust.common.handler;

import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @author : LiuMing
 * @date : 2019/11/4 14:54
 * @description :   公共handler
 */
public class CommonHandler extends ChannelInboundHandlerAdapter {
    protected int lossConnectCount = 0;
    protected ChannelHandlerContext ctx;

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Exception caught ...");
        cause.printStackTrace();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                System.out.println(ctx.channel().remoteAddress() + "读超时");
                lossConnectCount++;
                if (lossConnectCount > 2) {
                    System.out.println("Read idle loss connection.");
                    ctx.close();
                }
            } else if (e.state() == IdleState.WRITER_IDLE) {
                Message message = new Message();
                message.setType(MessageType.KEEPALIVE);
                ctx.writeAndFlush(message);
            }
        }
    }
}
