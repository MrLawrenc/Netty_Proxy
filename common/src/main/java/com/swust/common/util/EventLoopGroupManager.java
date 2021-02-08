package com.swust.common.util;

import io.netty.channel.EventLoopGroup;

import java.util.concurrent.TimeUnit;

/**
 * @date : 2020/4/28 15:49
 * @description : 线程组信息
 */
public class EventLoopGroupManager {


    public void schedule(EventLoopGroup loopGroup, Runnable command,
                         long delay, TimeUnit unit) {
        loopGroup.forEach(eventExecutor -> {
        });
        loopGroup.schedule(command, delay, unit);
    }
}