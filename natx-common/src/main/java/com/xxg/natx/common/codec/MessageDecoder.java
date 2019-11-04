package com.xxg.natx.common.codec;

import com.alibaba.fastjson.JSON;
import com.xxg.natx.common.protocol.Message;
import com.xxg.natx.common.protocol.MessageMetadata;
import com.xxg.natx.common.protocol.MessageType;
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
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List out) throws Exception {
        int type = msg.readInt();
        MessageType messageType = MessageType.valueOf(type);
        int metaDataLength = msg.readInt();
        CharSequence metaDataString = msg.readCharSequence(metaDataLength, CharsetUtil.UTF_8);

        byte[] data = null;
        if (msg.isReadable()) {
            data = ByteBufUtil.getBytes(msg);
        }

        Message message = new Message();
        message.setType(messageType);
        message.setMetadata(JSON.parseObject(metaDataString.toString(), MessageMetadata.class));
        message.setData(data);
        out.add(message);
    }

}
