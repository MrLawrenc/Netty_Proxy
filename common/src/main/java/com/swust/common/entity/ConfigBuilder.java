package com.swust.common.entity;

import java.util.List;

/**
 * @author : MrLawrenc
 * @date : 2020/4/25 16:58
 * @description : TODO
 */
public class ConfigBuilder {
    public static ServerConfig buildServer(String password, String port) {
        return new ServerConfig(password, port);
    }

    public static ClientConfig buildClient(String password, String port, String serverHost, List<String> proxyHost
            , List<String> proxyPort, List<String> remotePort) {
        return new ClientConfig(password, port).setServerHost(serverHost).setProxyHost(proxyHost).setProxyPort(proxyPort)
                .setRemotePort(remotePort);
    }
}