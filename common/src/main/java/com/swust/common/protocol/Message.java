package com.swust.common.protocol;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:59
 * @description :   客户端和服务端直接通信的消息体
 */

@Accessors(chain = true)
public class Message {


    /**
     * 数据包
     */
    @Getter
    @Setter
    private byte[] data;

    /**
     * 消息头，附带的其他信息
     */
    @Getter
    @Setter
    private MessageHeader header;

    public Message() {
        this.header = new MessageHeader();
    }
}
