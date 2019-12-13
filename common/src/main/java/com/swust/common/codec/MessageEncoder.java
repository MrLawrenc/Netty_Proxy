package com.swust.common.codec;

import com.alibaba.fastjson.JSON;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author : LiuMing
 * @date : 2019/11/4 14:38
 * @description :   消息编码器
 */
public class MessageEncoder extends MessageToByteEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf byteBuf) throws Exception {
        MessageHeader head = msg.getHeader();
        byte[] headBytes = JSON.toJSONBytes(head);
        byteBuf.writeInt(headBytes.length);
        byteBuf.writeBytes(headBytes);

        byteBuf.writeInt(msg.getData().length);
        byteBuf.writeBytes(msg.getData());
    }
}
