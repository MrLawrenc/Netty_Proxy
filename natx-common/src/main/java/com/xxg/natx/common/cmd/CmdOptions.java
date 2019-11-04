package com.xxg.natx.common.cmd;


/**
 * @author : LiuMing
 * @date : 2019/11/4 9:37
 * @description :   命令行参数枚举
 */
public enum CmdOptions {
    /**
     * 帮助命令
     */
    HELP("h", "help", false, "Help  command"),
    /**
     * 服务端启动端口
     */
    PORT("p", "port", true, "Server port"),
    /**
     * 指定密码
     */
    PASSWORD("pwd", "password", true, "Server start password and Client connection password"),
    /**
     * 指定服务端ip地址
     */
    HOST("h", "host", true, "Server host"),
    /**
     * 被代理的服务ip
     */
    PROXY_HOST("proxy_h", "proxy_host", true, "Proxy host"),
    /**
     * 被代理服务的端口
     */
    PROXY_PORT("proxy_p", "proxy_port", true, "Proxy port"),
    /**
     * 公网服务端对外提供访问内网应用的端口
     */
    REMOTE_PORT("remote_p", "remote_port", true, "Remote port");
    private String opt;
    private String longOpt;
    private String description;
    private boolean hasArgs;

    public boolean isHasArgs() {
        return hasArgs;
    }

    public String getOpt() {
        return opt;
    }

    public String getLongOpt() {
        return longOpt;
    }

    public String getDescription() {
        return description;
    }

    /**
     * cmd命令各参数说明
     *
     * @param opt         参数的简单形式
     * @param longOpt     参数的复杂形式
     * @param hasArgs     是否需要额外的输入
     * @param description 对参数的描述信息
     */
    CmdOptions(String opt, String longOpt, boolean hasArgs, String description) {
        this.hasArgs = hasArgs;
        this.opt = opt;
        this.longOpt = longOpt;
        this.description = description;
    }

}
