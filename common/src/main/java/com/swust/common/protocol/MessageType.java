package com.swust.common.protocol;


import lombok.Getter;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:58
 * @description :   消息类型
 */
public enum MessageType {

    /**
     * 客户端请求向服务端注册
     */
    REGISTER(1),
    /**
     * 服务端响应注册成功
     */
    REGISTER_RESULT(2),
    /**
     * 通知客户端有请求连接到服务端的指定端口
     */
    CONNECTED(3),
    /**
     * 断开连接请求
     */
    DISCONNECTED(4),
    /**
     * 服务端和客户端之间发送的数据包
     */
    DATA(5),
    /**
     * 客户端和服务端心跳包
     */
    KEEPALIVE(6),

    /**
     * 服务端集群的心跳包
     */
    SERVER_KEEPALIVE(7);
    @Getter
    private int code;

    MessageType(int code) {
        this.code = code;
    }
}
