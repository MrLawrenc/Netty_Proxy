package com.swust.common.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author : MrLawrenc
 * @date : 2020/4/25 16:53
 * @description : 客户端参数
 */
@Getter
@Setter
@Accessors(chain = true)
public class ClientConfig extends ServerConfig {

    public ClientConfig(String pwd, String port) {
        super(pwd, port);
    }

    /**
     * 服务器host
     */
    private String serverHost;

    /**
     * 需要被代理的服务器地址
     */
    private List<String> proxyHost;
    /**
     * 需要被代理的服务器端口
     */
    private List<String> proxyPort;
    /**
     * 需要外网暴露的代理服务器开放的端口
     */
    private List<String> remotePort;


}