package com.swust.common;

import io.netty.channel.Channel;
import lombok.Data;

/**
 * @author : hz20035009-逍遥
 * @date : 2020/4/20 16:38
 * @description : TODO
 */
@Data
public abstract class Parent {
    protected Channel channel;
    private int port;

    protected abstract void close();
}