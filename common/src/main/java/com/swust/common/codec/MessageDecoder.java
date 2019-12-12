package com.swust.common.codec;

import com.alibaba.fastjson.JSON;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.CharsetUtil;

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
       /* if (bodyLen == 3 && byteBuf.readCharSequence(bodyLen, StandardCharsets.UTF_8).toString().equals(Constant.NULL_TOKEN)) {
            Message message = new Message(JSON.parseObject(head, MessageHeader.class), data);
            System.out.println("decode:" + message);
            out.add(message);
        }*/
        byte[] data = new byte[bodyLen];
        byteBuf.readBytes(data);

        Message message = new Message(JSON.parseObject(head, MessageHeader.class), data);
        out.add(message);
       /* System.out.println(msg);
        decode0(msg, out);*/
    }

    private void decode0(ByteBuf msg, List<Object> out) {
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

    private void decode1(ByteBuf msg, List<Object> out) {
        /*
         * 这里ByteBuf实际使用的是PooledSlicedByteBuf，有zero copy和池化特性
         * */
        String headerStr = msg.readCharSequence(msg.readInt(), CharsetUtil.UTF_8).toString();
        MessageHeader messageHeader = JSON.parseObject(headerStr, MessageHeader.class);
        byte[] bytes = null;
        //ByteBuf unwrap = msg.unwrap();//如果该msg是包装过后的，使用该方法可以拿出非包装的ByteBuf
        if (msg.isReadable()) {
            bytes = ByteBufUtil.getBytes(msg);
        }
        out.add(new Message().setHeader(messageHeader).setData(bytes));
    }
}
