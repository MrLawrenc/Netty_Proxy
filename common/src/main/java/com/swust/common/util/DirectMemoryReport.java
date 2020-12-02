package com.swust.common.util;

import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 对外内存统计
 * https://github.com/netty/netty/issues/7769
 * jdk11开启 　-Dio.netty.tryReflectionSetAccessible=true 否则不会使用对外内存，{@link io.netty.util.internal.PlatformDependent0#DIRECT_BUFFER_CONSTRUCTOR}为null
 * 将导致{@link PlatformDependent#DIRECT_MEMORY_COUNTER}为null，具体可以跟下PlatformDependent0和PlatformDependent的静态方法初始化源码。
 *
 * @author hz20035009-逍遥
 * date   2020/12/2 14:55
 */
@Slf4j
public class DirectMemoryReport {

    public void start() throws Exception {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (log.isDebugEnabled()) {
                        log.info("netty_direct_memory:{}k", PlatformDependent.usedDirectMemory() / 1024);
                    }
                } catch (Exception ignored) {
                }
            }
        }, 10, 2 * 1000);
    }

    @Deprecated
    public void doReport() {
        try {
            Field field = PlatformDependent.class.getDeclaredField("DIRECT_MEMORY_COUNTER");
            field.setAccessible(true);
            AtomicLong counter = (AtomicLong) field.get(PlatformDependent.class);
            int l = (int) (counter.get() / 1024);
            log.info("netty_direct_memory:{}k", l);
        } catch (Exception ignored) {
        }
    }
}