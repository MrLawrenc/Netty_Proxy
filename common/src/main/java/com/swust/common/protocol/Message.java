package com.swust.common.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:59
 * @description :   客户端和服务端直接通信的消息体
 */
@Data
@Accessors(chain = true)
public class Message {

    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 数据包
     */
    private byte[] data;

    /**
     * 附带的其他信息，如密码等
     */
    private MessageMetadata metadata;

    public Message() {
        this.metadata = new MessageMetadata();
    }
}
