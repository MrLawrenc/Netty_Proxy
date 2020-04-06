package com.swust.common.protocol;

import com.swust.common.constant.Constant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:59
 * @description :   客户端和服务端直接通信的消息体
 */

@Accessors(chain = true)
@AllArgsConstructor
@ToString
public class Message {


    /**
     * 消息头，附带的其他信息
     */
    @Getter
    @Setter
    private MessageHeader header;
    /**
     * 数据包,不做data=null的校验
     */
    @Getter
    @Setter
    private byte[] data = Constant.NULL_TOKEN.getBytes();


    public Message() {
        this.header = new MessageHeader();
    }
}
