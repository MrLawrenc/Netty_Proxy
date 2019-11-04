package com.xxg.natx.client;

import com.xxg.natx.client.handler.ClientHandler;
import com.xxg.natx.client.net.TcpConnection;
import com.xxg.natx.common.cmd.CmdOptions;
import com.xxg.natx.common.codec.MessageDecoder;
import com.xxg.natx.common.codec.MessageEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.cli.*;

/**
 * @author : LiuMing
 * @date : 2019/11/4 14:15
 * @description :   内网的netty客户端，该客户端内部嵌了一个客户端，内部的客户端是访问本地的应用
 */
public class Client {

    public static void main(String[] args) throws Exception {

        Options options = new Options();
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

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption(CmdOptions.HELP.getLongOpt()) || cmd.hasOption(CmdOptions.HELP.getOpt())) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("options", options);
        } else {

            //opt和longOpt都可以拿到命令对应的值
            String serverAddress = cmd.getOptionValue(CmdOptions.HOST.getOpt());
            if (serverAddress == null) {
                System.out.println("server_addr cannot be null");
                return;
            }
            String serverPort = cmd.getOptionValue(CmdOptions.PORT.getOpt());
            if (serverPort == null) {
                System.out.println("server_port cannot be null");
                return;
            }
            String password = cmd.getOptionValue(CmdOptions.PASSWORD.getOpt());
            String proxyAddress = cmd.getOptionValue(CmdOptions.PROXY_HOST.getOpt());
            if (proxyAddress == null) {
                System.out.println("proxy_addr cannot be null");
                return;
            }
            String proxyPort = cmd.getOptionValue(CmdOptions.PROXY_PORT.getOpt());
            if (proxyPort == null) {
                System.out.println("proxy_port cannot be null");
                return;
            }
            String remotePort = cmd.getOptionValue(CmdOptions.REMOTE_PORT.getOpt());
            if (remotePort == null) {
                System.out.println("remote_port cannot be null");
                return;
            }
            System.out.println("pwd:" + password);
            TcpConnection tcpConnection = new TcpConnection();
            tcpConnection.connect(serverAddress, Integer.parseInt(serverPort), new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ClientHandler clientHandler = new ClientHandler(Integer.parseInt(remotePort), password,
                            proxyAddress, Integer.parseInt(proxyPort));
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                            new MessageDecoder(), new MessageEncoder(),
                            new IdleStateHandler(60, 30, 0), clientHandler);
                }
            });
        }
    }
}
