package com.swust.common.protocol;

import com.swust.common.exception.Exception;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:58
 * @description :   消息类型
 */
public enum MessageType {

    /**
     * 注册
     */
    REGISTER(1),
    /**
     * 注册结果
     */
    REGISTER_RESULT(2),
    /**
     * 请求公网服务代理的连接
     */
    CONNECTED(3),
    /**
     * 断开
     */
    DISCONNECTED(4),
    /**
     * 数据包
     */
    DATA(5),
    /**
     * 心跳包
     */
    KEEPALIVE(6);

    private int code;

    MessageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MessageType valueOf(int code) throws Exception {
        for (MessageType item : MessageType.values()) {
            if (item.code == code) {
                return item;
            }
        }
        throw new Exception("MessageType code error: " + code);
    }
}
