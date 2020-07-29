package com.swust.server.handler;

import com.swust.common.codec.MessageDecoder1;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hz20035009-逍遥
 * date   2020/7/28 17:56
 * <p>
 * 消息分发器
 */
@ChannelHandler.Sharable
@Slf4j
public class MessageDispatcher extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf data = (ByteBuf) msg;

        if (false) {
            int len = data.getInt(4);
            data.getBytes(4, new byte[1024]);

            ChannelPipeline pipeline = ctx.pipeline();
            //动态更改handler
            pipeline.replace("encode", "encode", new MessageDecoder1());
            pipeline.replace("decode", "decode", new MessageDecoder1());
            pipeline.replace("businessHandler", "businessHandler", new MessageDecoder1());
        }

        //continue execute
        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}