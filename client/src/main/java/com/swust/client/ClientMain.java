package com.swust.client;

import com.swust.client.handler.ClientHandler;
import com.swust.common.cmd.CmdOptions;
import com.swust.common.codec.MessageDecoder;
import com.swust.common.codec.MessageEncoder;
import com.swust.common.config.LogUtil;
import com.swust.common.constant.Constant;
import com.swust.common.entity.ClientConfig;
import com.swust.common.entity.ConfigBuilder;
import com.swust.common.util.CommandUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.StringUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.util.logging.Logger;

/**
 * @author : LiuMing
 * @date : 2019/11/4 14:15
 * @description :   内网的netty客户端，该客户端内部嵌了一个或多个代理客户端，内部的代理客户端是访问本地的应用
 *
 * 单机 -h localhost -p 9000 -password 123lmy -proxy_h localhost -proxy_p 880 -remote_p 11000
 * 多个 -profile F:\JavaProject\Netty_Proxy\client.pro
 */
public class ClientMain {
    private static Logger logger = Logger.getGlobal();
    private static ClientConfig clientConfig;


    public static void main(String[] args) throws Exception {
        Options options = new Options();

        CommandUtil.addClientOptions(options);

        CommandLine cmd = new DefaultParser().parse(options, args);
        if (cmd.hasOption(CmdOptions.HELP.getLongOpt()) || cmd.hasOption(CmdOptions.HELP.getOpt())) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(Constant.OPTIONS, options);
            return;
        }
        String profile = cmd.getOptionValue(CmdOptions.PROFILE.getOpt());
        if (!StringUtil.isNullOrEmpty(profile)) {
            LogUtil.infoLog("读取配置文件信息。。。。。。。。。。。。。。");
            clientConfig = CommandUtil.clientConfigByProperties(profile);
        } else {
            //opt和longOpt都可以拿到命令对应的值
            String serverAddress = cmd.getOptionValue(CmdOptions.HOST.getOpt());
            if (serverAddress == null) {
                logger.severe("server_addr cannot be null");
                return;
            }
            String serverPort = cmd.getOptionValue(CmdOptions.PORT.getOpt());
            if (serverPort == null) {
                logger.severe("server_port cannot be null");
                return;
            }
            String password = cmd.getOptionValue(CmdOptions.PASSWORD.getOpt());
            String proxyAddress = cmd.getOptionValue(CmdOptions.PROXY_HOST.getOpt());
            if (proxyAddress == null) {
                logger.severe("proxy_addr cannot be null");
                return;
            }

            String proxyPort = cmd.getOptionValue(CmdOptions.PROXY_PORT.getOpt());
            if (proxyPort == null) {
                logger.severe("proxy_port cannot be null");
                return;
            }

            String remotePort = cmd.getOptionValue(CmdOptions.REMOTE_PORT.getOpt());
            if (remotePort == null) {
                logger.severe("remote_port cannot be null");
                return;
            }
            clientConfig = ConfigBuilder.buildClient(password, serverPort, serverAddress, CommandUtil.parseArray(proxyAddress)
                    , CommandUtil.parseArray(proxyPort), CommandUtil.parseArray(remotePort));
        }
        start();
    }


    public static void start() throws Exception {
        TcpClient.connect(clientConfig.getServerHost(), Integer.parseInt(clientConfig.getServerPort()), new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ClientHandler clientHandler = new ClientHandler(clientConfig.getRemotePort(), clientConfig.getServerPassword(),
                        clientConfig.getProxyHost(), clientConfig.getProxyPort());
                ch.pipeline().addLast(
                        new MessageDecoder(), new MessageEncoder(),
                        new IdleStateHandler(60, 20, 0), clientHandler);
            }
        });
    }


}
