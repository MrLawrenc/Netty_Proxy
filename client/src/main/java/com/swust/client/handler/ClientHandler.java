package com.swust.client.handler;

import com.alibaba.fastjson.JSON;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author : LiuMing
 * 2019/11/4 13:42
 * 客户端 handler
 */
@Slf4j
public class ClientHandler extends CommonHandler {

    private final List<Integer> ports;
    private final String password;
    private final List<String> proxyAddress;
    private final List<Integer> proxyPort;
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
            processConnected(ctx, message);
        } else if (type == MessageType.DATA) {
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
        log.error("client trigger channel inactive,prepare to reconnect after closing the resource!");
        ClientManager.reset();

        CompletableFuture.runAsync(() -> {
            int sleep = DEFAULT_TRY_SECONDS;
            int count = 0;
            while (count < DEFAULT_TRY_COUNT) {
                try {
                    TimeUnit.SECONDS.sleep(sleep);
                    ClientMain.start();
                    log.info("restart client success!");
                    return;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                sleep <<= 1;
                count++;
                log.info("restart client fail,This is the {} retry,will try again after {}s", count, sleep);
            }
            log.error("the maximum number of retries reached,will exit");
            System.exit(0);
        });
    }

    /**
     * 处理在服务端注册结果
     */
    private void processRegisterResult(Message message) {
        if (message.getHeader().isSuccess()) {
            log.info("the proxy server started successfully,server msg:{}", message.getHeader().getDescription());
        } else {
            log.error("the proxy server failed to open,server msg:{}", message.getHeader().getDescription());
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
                    , proxyPort.get(index), ctx, channelId, ctx.channel().eventLoop());
            ClientManager.add2ChannelMap(channel, intranetClient);
        } catch (Exception e) {
            e.printStackTrace();
            Message message = new Message();
            MessageHeader header = message.getHeader();
            if (index == -1) {
                log.error("client ports config:{}  current open tcp port:{}", JSON.toJSONString(ports), openTcpPort);
                header.setDescription("current msg port is null!");
            }
            header.setType(MessageType.DISCONNECTED);
            header.setChannelId(receiveMessage.getHeader().getChannelId());
            channel.writeAndFlush(message);
        }
    }

    /**
     * 转发代理消息到内网
     */
    public void processData(Message message) {
        String channelId = message.getHeader().getChannelId();
        ChannelHandlerContext context = ClientManager.ID_SERVICE_CHANNEL_MAP.get(channelId);
        if (Objects.isNull(context)) {

            long start = System.currentTimeMillis();
            ClientManager.lock(channelId);
            ChannelHandlerContext newObj = ClientManager.ID_SERVICE_CHANNEL_MAP.get(channelId);
            if (Objects.isNull(newObj)) {
                log.error("no proxy client was found by id({}),will loss this msg,wait cos time:{}ms", channelId, (System.currentTimeMillis() - start));
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("wait proxy client connect success(channel id:{}),wait cos time:{}ms", channelId, System.currentTimeMillis() - start);
                }
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
            log.debug("proxy server closed current client,will close {}",context.channel());
            ClientManager.removeChannelMapByProxyClient(channel, message.getHeader().getChannelId());
        }
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
