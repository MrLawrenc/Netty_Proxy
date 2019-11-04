package com.swust.server;

import com.swust.server.handler.TcpServerHandler;
import com.swust.server.net.TcpServer;
import com.swust.common.cmd.CmdOptions;
import com.swust.common.codec.MessageDecoder;
import com.swust.common.codec.MessageEncoder;
import com.swust.common.constant.Constant;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.cli.*;

/**
 * @author : LiuMing
 * @date : 2019/11/4 9:46
 * @description :   服务端
 */
public class Server {


    public static void main(String[] args) throws InterruptedException, ParseException {


        /*
         * Apache Commons CLI是开源的命令行解析工具，它可以帮助开发者快速构建启动命令，并且帮助你组织命令的参数、以及输出列表等。
         * 参考博文:https://www.cnblogs.com/xing901022/archive/2016/06/22/5608823.html
         * CLI分为三个过程：
         *      定义阶段：在Java代码中定义Optin参数，定义参数、是否需要输入值、简单的描述等
         *      解析阶段：应用程序传入参数后，CLI进行解析
         *      询问阶段：通过查询CommandLine询问进入到哪个程序分支中
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

        //打印所有参数列表 fixme 可以注释掉
        HelpFormatter formatter1 = new HelpFormatter();
        formatter1.printHelp(Constant.OPTIONS, options);

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

            TcpServer tcpServer = new TcpServer();
            tcpServer.initTcpServer(port, new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    TcpServerHandler tcpServerHandler = new TcpServerHandler(password);
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                            new MessageDecoder(), new MessageEncoder(),
                            new IdleStateHandler(60, 30, 0),
                            tcpServerHandler);
                }
            });
            System.out.println("Tcp server started on port " + port + "\nPassword is " + password);
        }
    }
}
