package com.swust.server.handler;

import com.alibaba.fastjson.JSON;
import com.swust.common.config.LogUtil;
import com.swust.common.exception.ServerException;
import com.swust.common.handler.CommonHandler;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
import com.swust.server.ExtranetServer;
import com.swust.server.ServerManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author : LiuMing
 * @date : 2019/11/4 10:50
 * @description :   tcp handler
 */
public class TcpServerHandler extends CommonHandler {
    private String password;


    /**
     * 默认读超时上限
     */
    private static final byte DEFAULT_RECONNECTION_LIMIT = 5;
    private static final Map<ChannelHandlerContext, Integer> DEFAULT_COUNT = new HashMap<>();


    public TcpServerHandler(String password) {
        this.password = password;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws java.lang.Exception {
        if (!(msg instanceof Message)) {
            throw new Exception("Unknown message: " + JSON.toJSONString(msg));
        }
        Message message = (Message) msg;
        MessageType type = message.getHeader().getType();
        //客户端注册
        if (type == MessageType.REGISTER) {
            processRegister(ctx.channel(), message);
        } else {
            if (type == MessageType.DISCONNECTED) {
                processDisconnected(message);
            } else if (type == MessageType.DATA) {
                processData(message);
            } else if (type == MessageType.KEEPALIVE) {
                // 心跳包
                DEFAULT_COUNT.put(ctx, 0);
            } else {
                throw new ServerException("Unknown type: " + type);
            }
        }
    }


    /**
     * 处理客户端注册,每个客户端注册成功都会启动一个服务，绑定客户端指定的端口
     *
     * @param channel 与当前服务端保持连接的内网channel
     */
    private void processRegister(Channel channel, Message message) {
        String password = message.getHeader().getPassword();
        if (this.password == null || !this.password.equals(password)) {
            message.getHeader().setSuccess(false).setDescription("密码错误！");
        }

        boolean needRegister = false;
        //客户端指定对外开放的端口
        int port = message.getHeader().getOpenTcpPort();
        ExtranetServer result = ServerManager.hasServer4ChannelMap(channel, port);
        if (result != null) {
            LogUtil.warnLog("存在与当前客户端绑定的代理服务端，代理服务端端口:{}!", port);
            if (result.getChannel().isActive()) {
                LogUtil.infoLog("当前绑定的代理服务端仍然存在，将不处理此次开启代理服务端的请求！");
                return;
            }
            result.getChannel().close();
            LogUtil.warnLog("当前绑定的代理服务端已失活，即将重新开启代理！");
        } else {
            ExtranetServer old = ServerManager.PORT_MAP.get(port);
            if (Objects.isNull(old)) {
                LogUtil.infoLog("不存在与当前客户端绑定的代理服务端，即将开启新代理！msg:{}", JSON.toJSONString(message));
                needRegister = true;
            } else {
                LogUtil.infoLog("不存在与当前客户端绑定的代理服务端，但包含与当前端口{}绑定的代理服务端，直接绑定！msg:{}", port, JSON.toJSONString(message));
                old.getInitializer().setClientChannel(channel);
                ServerManager.add2ChannelMap(channel, old);
            }
        }

        if (needRegister) {
            try {
                ExtranetServer extranetServer = new ExtranetServer().initTcpServer(port, channel);
                if (Objects.isNull(extranetServer)) {
                    LogUtil.errorLog(" start proxy server on port: " + port + "  fail!");
                    return;
                }
                extranetServer.setChannel(channel);
                ServerManager.PORT_MAP.put(port, extranetServer);
                ServerManager.add2ChannelMap(channel, extranetServer);
                message.getHeader().setSuccess(true).setDescription("已开启代理客户端绑定到当前端口！");

                LogUtil.infoLog("Register success, start server on port: " + port);
            } catch (java.lang.Exception e) {
                e.printStackTrace();
                LogUtil.errorLog("Register fail, msg:{} port: ", message, port);
                return;
            }
        } else {
            message.getHeader().setSuccess(true).setDescription("已有绑定当前端口的代理服务端，不需要新开启！");
        }
        message.getHeader().setType(MessageType.REGISTER_RESULT);
        ctx.writeAndFlush(message);
    }

    /**
     * 处理收到转发的内网响应数据包
     */
    private void processData(Message message) {
        ChannelHandlerContext userCtx = ServerManager.findChannelByMsg(message);
        if (Objects.isNull(userCtx)) {
            LogUtil.errorLog("收到内网代理客户端的消息，但是未找到相应与外网代理服务端连接的用户客户端！msg:{}", message.getHeader().toString());
        } else {
            userCtx.writeAndFlush(message.getData());
        }
    }

    /**
     * 断开,先关闭外网暴露的代理，在关闭连接的客户端
     */
    private void processDisconnected(Message message) throws InterruptedException {
        ChannelHandlerContext userCtx = ServerManager.findChannelByMsg(message);
        if (Objects.isNull(userCtx)) {
            LogUtil.warnLog("收到内网客户端断开连接的消息，但未找到与之对应的外网用户客户端，可能已经关闭！");
        } else {
            userCtx.close();
        }
    }

    /**
     * 暴露的外网代理服务端资源清理
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LogUtil.errorLog("内网客户端断开连接..................");
        List<ExtranetServer> servers = ServerManager.CHANNEL_MAP.get(ctx.channel());
        if (Objects.isNull(servers) || servers.size() == 0) {
            LogUtil.errorLog("未找到与当前内网客户端绑定的服务端...........");
        } else {
            LogUtil.infoLog("断开与对应（当前客户端是与相应的代理服务端绑定的）代理服务端连接的所有用户客户端！");
            servers.forEach(server -> server.getGroup().close());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            Integer count = DEFAULT_COUNT.get(ctx);
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                if (Objects.isNull(count)) {
                    count = 0;
                }
                DEFAULT_COUNT.put(ctx, count++);
                if (count > DEFAULT_RECONNECTION_LIMIT) {
                    DEFAULT_COUNT.remove(ctx);
                    LogUtil.errorLog("Read idle  will loss connection. retryNum:{}", count);
                    ctx.close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }


}
