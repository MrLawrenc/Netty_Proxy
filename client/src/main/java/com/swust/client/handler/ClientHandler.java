package com.swust.client.handler;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.swust.client.ClientMain;
import com.swust.client.ClientManager;
import com.swust.client.IntranetClient;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import com.swust.common.protocol.MessageType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author : LiuMing
 * 2019/11/4 13:42
 * 客户端 handler
 */
@Slf4j
public class ClientHandler extends CommonHandler {
    private final ExecutorService poolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() << 2, 3, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(Integer.MAX_VALUE >> 4),new ThreadFactoryBuilder().setNameFormat("send-data-%d").build());

    private final ExecutorService connectPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() << 2, 3, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(Integer.MAX_VALUE >> 4),new ThreadFactoryBuilder().setNameFormat("create-conn-%d").build());

    private List<Integer> ports;
    private String password;
    private List<String> proxyAddress;
    private List<Integer> proxyPort;
    /**
     * 默认重新拉起客户端的起始秒数
     */
    private static final int DEFAULT_TRY_SECONDS = 4;
    /**
     * 默认重新拉起客户端尝试的次数上限
     */
    private static final int DEFAULT_TRY_COUNT = 10;


    public ClientHandler(List<String> ports, String password, List<String> proxyAddress, List<String> proxyPort) {
        this.ports = ports.stream().map(Integer::parseInt).collect(Collectors.toList());
        this.password = password;
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort.stream().map(Integer::parseInt).collect(Collectors.toList());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ports.forEach(port -> {
            Message message = new Message();
            MessageHeader header = message.getHeader();
            header.setType(MessageType.REGISTER).setOpenTcpPort(port).setPassword(password);
            ctx.writeAndFlush(message);
        });
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws java.lang.Exception {
        if (!(msg instanceof Message)) {
            throw new Exception("unknown message type: " + msg.getClass().getName());
        }


        Message message = (Message) msg;
        MessageType type = message.getHeader().getType();
        if (type == MessageType.KEEPALIVE) {
            return;
        }
        if (type == MessageType.REGISTER_RESULT) {
            processRegisterResult(message);
        } else if (type == MessageType.CONNECTED) {
            //connectPool.execute(() -> processConnected(ctx, message));
            processConnected(ctx, message);
        } else if (type == MessageType.DATA) {
            //poolExecutor.execute(() -> processData(message));
            processData(message);
        } else if (type == MessageType.DISCONNECTED) {
            processDisconnected(ctx.channel(), message);
        } else {
            log.error("unknown  msg:{}", message.toString());
        }

    }


    /**
     * 重连
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("client trigger channelInactive,prepare to reconnect after closing the resource!");
        ClientManager.reset();

        CompletableFuture.runAsync(() -> {
            int sleep = DEFAULT_TRY_SECONDS;
            int count = 0;
            while (count < DEFAULT_TRY_COUNT) {
                try {
                    TimeUnit.SECONDS.sleep(sleep);
                    ClientMain.start();
                    log.info("Restart client success!");
                    return;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                sleep <<= 1;
                count++;
                log.warn("Restart client fail,This is the {} retry,will try again after {}s", count, sleep);
            }
            log.error("The maximum number of retries reached,will exit");
            System.exit(0);
        });
    }

    /**
     * 处理在服务端注册结果
     */
    private void processRegisterResult(Message message) {
        if (message.getHeader().isSuccess()) {
            log.info("The proxy server started successfully,server msg:{}", message.getHeader().getDescription());
        } else {
            log.error("The proxy server failed to open,server msg:{}", message.getHeader().getDescription());
            System.exit(0);
        }
    }


    /**
     * 该请求来源于，请求外网暴露的端口，外网通过netty服务端转发来到这里的
     * 请求内部代理服务，建立netty客户端，请求访问本地的服务，获取返回结果
     */
    private void processConnected(ChannelHandlerContext ctx, Message receiveMessage) {
        Channel channel = ctx.channel();
        String channelId = receiveMessage.getHeader().getChannelId();
        int openTcpPort = receiveMessage.getHeader().getOpenTcpPort();
        int index = ports.indexOf(openTcpPort);
        try {
            IntranetClient intranetClient = new IntranetClient().connect(proxyAddress.get(index)
                    , proxyPort.get(index), ctx, channelId);
            ClientManager.add2ChannelMap(channel, intranetClient);
        } catch (Exception e) {
            e.printStackTrace();
            Message message = new Message();
            MessageHeader header = message.getHeader();
            if (index == -1) {
                log.error("Client ports config:{}  current open tcp port:{}", JSON.toJSONString(ports), openTcpPort);
                header.setDescription("Current msg port is null!");
            }
            header.setType(MessageType.DISCONNECTED);
            header.setChannelId(receiveMessage.getHeader().getChannelId());
            channel.writeAndFlush(message);
        }
    }

    public void processData(Message message) {
        String channelId = message.getHeader().getChannelId();
        ChannelHandlerContext context = ClientManager.ID_SERVICE_CHANNEL_MAP.get(channelId);
        if (Objects.isNull(context)) {
            log.info("===================等待代理客户端建立连接============================");
            //fix 加锁，可能代理客户端还未连接上就收到了数据包
            long l = System.currentTimeMillis();
            ClientManager.lock(channelId);
            ChannelHandlerContext newObj = ClientManager.ID_SERVICE_CHANNEL_MAP.get(channelId);
            if (Objects.isNull(newObj)) {
                log.error("No proxy client was found by id : {}", channelId);
                log.info("超时:{},仍未等到代理客户端成功建立连接:", (System.currentTimeMillis() - l) + "ms");
            } else {
                newObj.writeAndFlush(message.getData());
            }
        } else {
            context.writeAndFlush(message.getData());
        }
    }

    /**
     * 与代理服务端连接的用户客户端断开连接，处理资源，以及断开内网代理客户端
     */
    private void processDisconnected(Channel channel, Message message) {
        ChannelHandlerContext context = ClientManager.ID_SERVICE_CHANNEL_MAP.get(message.getHeader().getChannelId());
        if (Objects.nonNull(context)) {
            context.close();
            ClientManager.removeChannelMapByProxyClient(channel, message.getHeader().getChannelId());
        }
    }


    /**
     * 维持内网连接,6h执行一次
     */
    public void heartPkg(LocalProxyHandler localProxyHandler) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    // localProxyHandler.getCtx().writeAndFlush("heart pkg");
                } catch (Exception e) {
                    log.warn("time  warning ..................");
                }
            }
        }, 1000 * 60 * 60 * 6, 1000 * 60 * 60 * 6);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.WRITER_IDLE) {
                Message message = new Message();
                message.getHeader().setType(MessageType.KEEPALIVE);
                ctx.writeAndFlush(message);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
