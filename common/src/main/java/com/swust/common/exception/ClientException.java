package com.swust.common.exception;

/**
 * @author : LiuMingyao
 * @date : 2020/4/12 13:53
 * @description : TODO
 */
public class ClientException extends ProxyException {

    public ClientException(String message) {
        super(message);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }
}