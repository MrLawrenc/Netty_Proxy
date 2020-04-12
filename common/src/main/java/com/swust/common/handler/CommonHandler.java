package com.swust.common.handler;

import com.swust.common.exception.ProxyException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author : LiuMing
 * @date : 2019/11/4 14:54
 * @description :   公共handler
 */
public class CommonHandler extends ChannelInboundHandlerAdapter {
    protected Logger logger = Logger.getGlobal();

    protected AtomicInteger lossConnectCount = new AtomicInteger(0);
    @Getter
    protected ChannelHandlerContext ctx;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ProxyException) {
            logger.info("ProxyException.......................");
        } else {
            logger.warning(String.format("client/server exception(%s) will close!...............", cause.getMessage()));
            Channel channel = ctx.channel();
            logger.warning("#### exceptionCaught #### " + String.format("localAddr:%s  remoteAddr:%s", channel.localAddress(), channel.remoteAddress()) + " obj->" + this.getClass().getSimpleName());
            ctx.close();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        /*if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                int retryNum = lossConnectCount.incrementAndGet();
                if (retryNum > DEFAULT_RECONNECTION_LIMIT) {
                    logger.severe("Read idle  will loss connection. retryNum:" + retryNum);
                    ctx.close();
                }
            } else if (e.state() == IdleState.WRITER_IDLE) {
                Message message = new Message();
                message.getHeader().setType(MessageType.KEEPALIVE);
                ctx.writeAndFlush(message);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }*/
    }
}
