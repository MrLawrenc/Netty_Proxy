package com.swust.server;

import com.swust.common.cmd.CmdOptions;
import com.swust.common.codec.MessageDecoder;
import com.swust.common.codec.MessageEncoder;
import com.swust.common.config.LogFormatter;
import com.swust.common.config.LogUtil;
import com.swust.common.constant.Constant;
import com.swust.server.handler.TcpServerHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.cli.*;

import java.util.concurrent.TimeUnit;

/**
 * @author : LiuMing
 * @date : 2019/11/4 9:46
 * @description :   服务端
 */
public class ServerMain {
    private static Channel serverChannel;

    /**
     * Apache Commons CLI是开源的命令行解析工具，它可以帮助开发者快速构建启动命令，并且帮助你组织命令的参数、以及输出列表等。
     * 参考博文:https://www.cnblogs.com/xing901022/archive/2016/06/22/5608823.html
     * CLI分为三个过程：
     * <p>      </>定义阶段：在Java代码中定义Optin参数，定义参数、是否需要输入值、简单的描述等
     * <p>      </>解析阶段：应用程序传入参数后，CLI进行解析
     * <p>      </>询问阶段：通过查询CommandLine询问进入到哪个程序分支中
     */
    public static void main(String[] args) throws Exception {
        LogFormatter.init();
        /*
         *
         * 1.定义阶段:
         * 其中Option的参数：
         *   第一个参数：参数的简单形式
         *   第二个参数：参数的复杂形式
         *   第三个参数：是否需要额外的输入
         *   第四个参数：对参数的描述信息
         *
         * */
        Options options = new Options();
        options.addOption(CmdOptions.HELP.getOpt(), CmdOptions.HELP.getLongOpt(),
                CmdOptions.HELP.isHasArgs(), CmdOptions.HELP.getDescription());
        options.addOption(CmdOptions.PORT.getOpt(), CmdOptions.PORT.getLongOpt(),
                CmdOptions.PORT.isHasArgs(), CmdOptions.PORT.getDescription());
        options.addOption(CmdOptions.PASSWORD.getOpt(), CmdOptions.PASSWORD.getLongOpt(),
                CmdOptions.PASSWORD.isHasArgs(), CmdOptions.PASSWORD.getDescription());
        /*
         *
         * 2.解析阶段
         *      通过解析器解析参数
         * */
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        /*
         * 3.询问阶段
         *      根据commandLine查询参数，提供服务
         * */
        if (cmd.hasOption(CmdOptions.HELP.getOpt()) || cmd.hasOption(CmdOptions.HELP.getLongOpt())) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(Constant.OPTIONS, options);
        } else {
            int port = Integer.parseInt(cmd.getOptionValue(CmdOptions.PORT.getLongOpt(), Constant.DEFAULT_PORT));
            String password = cmd.getOptionValue(CmdOptions.PASSWORD.getLongOpt(), Constant.DEFAULT_PASSWORD);

            start(port, password);
        }
    }


    private static void start(int port, String password) throws Exception {
        serverChannel = new TcpServer().initTcpServer(port, new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                TcpServerHandler tcpServerHandler = new TcpServerHandler(password);
                //int为4字节，定义的长度字段(长度字段+消息体)
                ch.pipeline().addLast(new MessageDecoder(), new MessageEncoder(),
                        new IdleStateHandler(60, 20, 0, TimeUnit.SECONDS),
                        tcpServerHandler);
            }
        });
        LogUtil.infoLog("服务端启动成功 port:{} pwd:{}", port, password);
    }

    /**
     * 获取服务端channel，该channel永远存在，不会被主动关闭
     */
    public static Channel getServerChannel() {
        return serverChannel;
    }
}
