package com.swust.common.util;

import com.swust.common.cmd.CmdOptions;
import com.swust.common.entity.ClientConfig;
import com.swust.common.entity.ConfigBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.Options;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author : MrLawrenc
 * @date : 2020/4/25 13:14
 * 命令行参数工具类
 */
@Slf4j
public class CommandUtil {
    /**
     * 设置客户端参数列表
     */
    public static void addClientOptions(Options options) {
        options.addOption(CmdOptions.HELP.getOpt(), CmdOptions.HELP.getLongOpt(),
                CmdOptions.HELP.isHasArgs(), CmdOptions.HELP.getDescription());
        options.addOption(CmdOptions.HOST.getOpt(), CmdOptions.HOST.getLongOpt(),
                CmdOptions.HOST.isHasArgs(), CmdOptions.HOST.getDescription());
        options.addOption(CmdOptions.PORT.getOpt(), CmdOptions.PORT.getLongOpt(),
                CmdOptions.PORT.isHasArgs(), CmdOptions.PORT.getDescription());
        options.addOption(CmdOptions.PASSWORD.getOpt(), CmdOptions.PASSWORD.getLongOpt(),
                CmdOptions.PASSWORD.isHasArgs(), CmdOptions.PASSWORD.getDescription());
        options.addOption(CmdOptions.PROXY_HOST.getOpt(), CmdOptions.PROXY_HOST.getLongOpt(),
                CmdOptions.PROXY_HOST.isHasArgs(), CmdOptions.PROXY_HOST.getDescription());
        options.addOption(CmdOptions.PROXY_PORT.getOpt(), CmdOptions.PROXY_PORT.getLongOpt(),
                CmdOptions.PROXY_PORT.isHasArgs(), CmdOptions.PROXY_PORT.getDescription());
        options.addOption(CmdOptions.REMOTE_PORT.getOpt(), CmdOptions.REMOTE_PORT.getLongOpt(),
                CmdOptions.REMOTE_PORT.isHasArgs(), CmdOptions.REMOTE_PORT.getDescription());

        options.addOption(CmdOptions.PROFILE.getOpt(), CmdOptions.PROFILE.getLongOpt(),
                CmdOptions.PROFILE.isHasArgs(), CmdOptions.PROFILE.getDescription());
    }

    public static Properties parseProfile(String filePath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(filePath));
            log.info(properties.toString());
        } catch (IOException e) {
            throw new RuntimeException("Parse profile error!", e);
        }
        return properties;
    }

    public static ClientConfig clientConfigByProperties(String filePath) {
        Properties properties = parseProfile(filePath);
        return ConfigBuilder.buildClient(properties.get("password").toString()
                , properties.get("port").toString(), properties.get("host").toString()
                , CommandUtil.parseArray(properties.get("proxyHost").toString())
                , CommandUtil.parseArray(properties.get("proxyPort").toString())
                , CommandUtil.parseArray(properties.get("remotePort").toString()));

    }

    /**
     * 入参格式必须为 : [1,2,3,4] 或 1,2,3,4
     */
    public static List<String> parseArray(String source) {
        String replace = source.replace("\\[", "").replace("]", "");
        return Arrays.asList(replace.split(","));
    }

}