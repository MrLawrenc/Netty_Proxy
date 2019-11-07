package com.swust.common.codec;

import com.alibaba.fastjson.JSON;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * @author : LiuMing
 * @date : 2019/11/4 14:38
 * @description :   消息编码器
 */
public class MessageEncoder extends MessageToByteEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            MessageHeader header = msg.getHeader();
            byte[] headerBytes;
            try {
                headerBytes = JSON.toJSONBytes(header);
            } catch (Exception e) {
                System.out.println("消息头解析错误.......................");
                e.printStackTrace();
                return;
            }
            dataOutputStream.writeInt(headerBytes.length);
            dataOutputStream.write(headerBytes);
            byte[] data = msg.getData();
            if (data != null && data.length > 0) {
                dataOutputStream.write(data);
            }
            byte[] message = byteArrayOutputStream.toByteArray();
            out.writeInt(message.length);
            out.writeBytes(message);
        }

    }

}
