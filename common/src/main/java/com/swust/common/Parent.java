package com.swust.common;

import io.netty.channel.Channel;
import lombok.Getter;

/**
 * @author : hz20035009-逍遥
 * @date : 2020/4/20 16:38
 * @description : TODO
 */
public abstract class Parent {
    @Getter
    protected Channel channel;

    protected abstract void close();
}