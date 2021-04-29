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
 *  消息解码器
 */
public class MessageDecoder1 extends ReplayingDecoder<Void> {


    /**
     * 以下三种方式均可以使用
     * 1. 分为读头部和数据两部分
     * 2. 所有数据一次性读
     * 3. 引入check point来读取
     */
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
