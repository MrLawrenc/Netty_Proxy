package com.swust.common.handler;

import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Getter;

import java.time.LocalDateTime;
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
        logger.warning(String.format("client/server exception(%s) ...............", cause.getMessage()));
        Channel channel = ctx.channel();
        logger.warning("#### exceptionCaught #### " + String.format("localAddr:%s  remoteAddr:%s", channel.localAddress(), channel.remoteAddress()) + " obj->" + this.getClass().getSimpleName());
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                logger.warning(LocalDateTime.now() + " : " + ctx.channel().remoteAddress() + " Read idle ");
                int retryNum = lossConnectCount.incrementAndGet();
                if (retryNum > DEFAULT_RECONNECTION_LIMIT) {
                    logger.severe("Read idle  will loss connection.");
                    ctx.close();
                }
            } else if (e.state() == IdleState.WRITER_IDLE) {
                logger.info(LocalDateTime.now().toString() + ":Write idle will write again.");
                Message message = new Message();
                message.getHeader().setType(MessageType.KEEPALIVE);
                ctx.writeAndFlush(message);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
