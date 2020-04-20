package com.swust.common.config;

/**
 * @author : hz20035009-逍遥
 * @date : 2020/4/20 9:39
 * @description : TODO
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : LiuMingyao
 * @date : 2019/3/16 13:55
 * @description : 日志封装类
 * <p>依赖slf4j</p>
 */
public final class LogUtil {

    private LogUtil() {
    }

    private static Logger getLogger(Class clz) {
        return LoggerFactory.getLogger(clz);
    }

    private static Logger getLogger(String clzName) {
        return LoggerFactory.getLogger(clzName);
    }

    /**
     * @author : LiuMing
     * @date : 2019/8/19 11:25
     * @description :   info日志
     */
    public static void infoLog(String str, Object... messages) {
        String clzName = getClzName();
        getLogger(clzName).info(str, messages);
    }

    /**
     * @author : LiuMing
     * @date : 2019/9/19 13:32
     * @description :   debug日志
     */
    public static void debugLog(String str, Object... messages) {
        Logger logger = getLogger(getClzName());
        if (logger.isDebugEnabled()) {
            logger.debug(str, messages);
        }

    }


    /**
     * @author : LiuMing
     * @date : 2019/8/19 13:37
     * @description :   error日志
     */
    public static void errorLog(String str, Object... messages) {
        String clzName = getClzName();
        getLogger(clzName).error(str, messages);
    }


    /**
     * @author : LiuMing
     * @date : 2019/8/19 13:53
     * @description :   warn日志
     */
    public static void warnLog(String str, Object... messages) {
        String clzName = getClzName();
        getLogger(clzName).warn(str, messages);
    }

    /**
     * @param params 需要拼接的参数
     * @date : 2019/3/19 13:53
     * @author Liu Ming
     * 记录普通的日志信息
     */
    @Deprecated
    public static void infoLog(Class clz, String str, Object... params) {
        getLogger(clz).warn(str, params);
    }

    @Deprecated
    public static void debugLog(Class clz, String str, Object... params) {
        if (getLogger(clz).isDebugEnabled()) {
            getLogger(clz).debug(str, params);
        }
    }

    /**
     * @author : Liu Ming
     * @date : 2019/3/19 13:53
     * 记录异常日志信息
     */
    @Deprecated
    public static void warnLog(Class clz, String str, Object... params) {
        getLogger(clz).warn(str, params);
    }

    @Deprecated
    public static void errorLog(Class clz, String str, Object... params) {
        getLogger(clz).error(str, params);
    }

    /**
     * @author : LiuMing
     * @date : 2019/9/19 13:28
     * @description :   获取当前使用日志方法的类名
     */
    private static String getClzName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return stackTrace.length < 3 ? "  非本地类   " : stackTrace[2].getClassName();
    }
}