package com.swust.common.codec;

import com.alibaba.fastjson.JSON;
import com.swust.common.protocol.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * @author : LiuMing
 * @date : 2019/11/4 15:03
 * @description :   消息解码器
 */
public class MessageDecoder2 extends ReplayingDecoder<Void> {


    /**
     * 以下三种方式均可以使用
     * 1. 分为读头部和数据两部分
     * 2. 所有数据一次性读
     * 3. 引入check point来读取
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {


        int dataLen = byteBuf.readInt();
        byte[] data = new byte[dataLen];
        byteBuf.readBytes(data);
        Message message = JSON.parseObject(data, Message.class);
        out.add(message);


    }

}
