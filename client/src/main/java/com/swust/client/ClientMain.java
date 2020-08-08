package com.swust.client;

import com.swust.client.handler.ClientHandler;
import com.swust.common.cmd.CmdOptions;
import com.swust.common.codec.MessageDecoder0;
import com.swust.common.codec.MessageEncoder;
import com.swust.common.constant.Constant;
import com.swust.common.entity.ClientConfig;
import com.swust.common.entity.ConfigBuilder;
import com.swust.common.util.CommandUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 * @author : LiuMing
 * @date : 2019/11/4 14:15
 * 内网的netty客户端，该客户端内部嵌了一个或多个代理客户端，内部的代理客户端是访问本地的应用
 * <p>
 * 单机 -h localhost -p 9000 -password 123lmy -proxy_h localhost -proxy_p 880 -remote_p 11000
 * 多个 -profile F:\JavaProject\Netty_Proxy\client.pro
 */
@Slf4j
public class ClientMain {
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
            log.info("start read profile info");
            clientConfig = CommandUtil.clientConfigByProperties(profile);
        } else {
            //opt和longOpt都可以拿到命令对应的值
            String serverAddress = cmd.getOptionValue(CmdOptions.HOST.getOpt());
            if (serverAddress == null) {
                log.error("server_addr cannot be null");
                return;
            }
            String serverPort = cmd.getOptionValue(CmdOptions.PORT.getOpt());
            if (serverPort == null) {
                log.error("server_port cannot be null");
                return;
            }
            String password = cmd.getOptionValue(CmdOptions.PASSWORD.getOpt());
            String proxyAddress = cmd.getOptionValue(CmdOptions.PROXY_HOST.getOpt());
            if (proxyAddress == null) {
                log.error("proxy_addr cannot be null");
                return;
            }

            String proxyPort = cmd.getOptionValue(CmdOptions.PROXY_PORT.getOpt());
            if (proxyPort == null) {
                log.error("proxy_port cannot be null");
                return;
            }

            String remotePort = cmd.getOptionValue(CmdOptions.REMOTE_PORT.getOpt());
            if (remotePort == null) {
                log.error("remote_port cannot be null");
                return;
            }
            clientConfig = ConfigBuilder.buildClient(password, serverPort, serverAddress, CommandUtil.parseArray(proxyAddress)
                    , CommandUtil.parseArray(proxyPort), CommandUtil.parseArray(remotePort));
        }
        start();
    }


    public static void start() throws Exception {
        UnorderedThreadPoolEventExecutor eventExecutors = new UnorderedThreadPoolEventExecutor(Runtime.getRuntime().availableProcessors() , new DefaultThreadFactory("main-client-business"));

        connect(clientConfig.getServerHost(), Integer.parseInt(clientConfig.getServerPort()), new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ClientHandler clientHandler = new ClientHandler(clientConfig.getRemotePort(), clientConfig.getServerPassword(),
                        clientConfig.getProxyHost(), clientConfig.getProxyPort());
                ch.pipeline().addLast(new MessageDecoder0(), new MessageEncoder(),
                        new IdleStateHandler(60, 20, 0));
                ch.pipeline().addLast(eventExecutors, clientHandler);
            }
        });
    }

    private static void connect(String host, int port, ChannelInitializer<?> channelInitializer) throws Exception {
        Bootstrap b = new Bootstrap();
        NioEventLoopGroup work = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() << 1,
                new DefaultThreadFactory("main-client-work"));
        try {
            b.group(work)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(channelInitializer);

            ChannelFuture future = b.connect(host, port).sync();
            future.addListener((ChannelFutureListener) future1 -> {
                boolean success = future1.isSuccess();
                if (success) {
                    log.info("connect {} : {} success", host, port);
                } else {
                    log.error("connect {} : {} fail", host, port);
                }
            });
            future.channel().closeFuture().addListener(f -> work.shutdownGracefully());
        } catch (Exception e) {
            work.shutdownGracefully();
            log.error("start client fail!", e);
            throw e;
        }
    }


}
