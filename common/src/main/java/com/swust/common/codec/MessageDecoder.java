package com.swust.common.codec;

import com.alibaba.fastjson.JSON;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

/**
 * @author : LiuMing
 * @date : 2019/11/4 15:03
 * @description :   消息解码器
 */
public class MessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        int headerLen = msg.readInt();
        String headerStr = msg.readCharSequence(headerLen, CharsetUtil.UTF_8).toString();
        MessageHeader messageHeader = JSON.parseObject(headerStr, MessageHeader.class);
        byte[] bytes = null;
        /*
         * 注意 ByteBufUtil.getBytes(msg)方法不会改变readIndex，因此不能使用while，不然会一直死循环，如果使用msg.readByte()则
         * 需要一直while读
         */
        if (msg.isReadable()) {

            bytes = ByteBufUtil.getBytes(msg);
        }
        out.add(new Message().setHeader(messageHeader).setData(bytes));
    }

}
