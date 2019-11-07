package com.swust.common.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author : LiuMingyao
 * @date : 2019/11/4 15:10
 * @description : 消息头，禁止手动创建对象，一半序列化的时候框架创建,推荐使用new Message().getHeader()获取头信息对象
 */
@Data
@Accessors(chain = true)
public class MessageHeader {
    /**
     * 消息类型
     */
    private MessageType type;
    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 密码
     */
    private String password;

    /**
     * 消息描述
     */
    private String description;
    /**
     * 端口,该端口是客户端指定的公网端口，可以通过公网端口访问内网服务。
     */
    private int openTcpPort;
    /**
     * 主机
     */
    private String host;
    /**
     * 当前请求连接的channel long id
     */
    private String channelId;
}