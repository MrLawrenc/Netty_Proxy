package com.swust.common.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author : LiuMingyao
 * @date : 2019/11/4 15:10
 * @description : TODO
 */
@Data
@Accessors(chain = true)
public   class MessageMetadata {
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