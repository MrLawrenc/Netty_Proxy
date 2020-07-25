package com.swust.common.codec;

import com.alibaba.fastjson.JSON;
import com.swust.common.protocol.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * @author MrLawrenc
 * date  2020/7/25 18:06
 * 三种方式均可以使用 {@link MessageDecoder1} {@link MessageDecoder2} {@link MessageDecoder0}
 * 1. 分为读头部和数据两部分
 * 2. 所有数据一次性读
 * 3. 引入check point来读取
 */
public class MessageDecoder0 extends ReplayingDecoder<MessageDecoder0.MyDecoderState> {

    private int dataLen;

    public MessageDecoder0() {
        super(MyDecoderState.READ_LENGTH);
    }

    /**
     *
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
        switch (state()) {
            case READ_LENGTH:
                dataLen = byteBuf.readInt();
            case READ_CONTENT:
                byte[] data = new byte[dataLen];
                byteBuf.readBytes(data);
                Message message = JSON.parseObject(data, Message.class);
                out.add(message);
                break;
            default:
                throw new Error("Shouldn't reach here.");
        }
    }

    public enum MyDecoderState {
        READ_LENGTH,
        READ_CONTENT;
    }
}
