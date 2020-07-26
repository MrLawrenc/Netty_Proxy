package com.swust.common.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author : LiuMing
 * @date : 2019/11/4 14:54
 */
@Slf4j
public class CommonHandler extends ChannelInboundHandlerAdapter {
    protected ChannelHandlerContext ctx;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("exceptionCaught", cause);
        log.info("local:{} remote:{}",ctx.channel().localAddress(),ctx.channel().remoteAddress());
        if (!(cause.getCause() instanceof IOException) || !"Connection reset by peer".equals(cause.getMessage())) {
            ctx.close();
        }
    }

}
