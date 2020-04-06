package com.swust.common.codec;

import com.alibaba.fastjson.JSON;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author : LiuMing
 * @date : 2019/11/4 15:03
 * @description :   消息解码器
 */
public class MessageDecoder extends ReplayingDecoder<Message> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
        int headLen = byteBuf.readInt();
        String head = byteBuf.readCharSequence(headLen, StandardCharsets.UTF_8).toString();


        int bodyLen = byteBuf.readInt();

        byte[] data = new byte[bodyLen];
        byteBuf.readBytes(data);

        Message message = new Message(JSON.parseObject(head, MessageHeader.class), data);
        out.add(message);
    }
}
