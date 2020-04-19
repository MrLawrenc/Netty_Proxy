package com.swust.server.handler;

import com.swust.common.config.LogUtil;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import com.swust.common.protocol.MessageType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * @author : LiuMing
 * @date : 2019/11/4 13:54
 * @description :   代理服务器的handler，当请求公网暴露的代理端口时，会转发到相应的客户端，
 */
public class RemoteProxyHandler extends ChannelInboundHandlerAdapter {
    /**
     * 当前的netty服务端，转发请求，将来自外网的请求转发到内网，将来自内网的响应响应给外部客户端
     */
    private Channel clientChannel;

    RemoteProxyHandler(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    /**
     * 外部请求外网代理的端口时调用，保存的服务端channel会给内网客户端发送消息 proxyHandler.getCtx().writeAndFlush(message);
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.CONNECTED);
        header.setChannelId(ctx.channel().id().asLongText());
        clientChannel.writeAndFlush(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LogUtil.warnLog("用户与外网代理服务端断开，通知客户端！");
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.DISCONNECTED);
        header.setChannelId(ctx.channel().id().asLongText());
        clientChannel.writeAndFlush(message);
    }

    public static void main(String[] args) throws Exception {
        String cmd = "netstat  -aon|findstr  ";
        String[] c = {};
        Process process = Runtime.getRuntime().exec(cmd + " 64572");
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "GBK"));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line + "\n");
        }
        System.out.println(stringBuilder);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] data = (byte[]) msg;
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.DATA);
        message.setData(data);
        header.setChannelId(ctx.channel().id().asLongText());
        clientChannel.writeAndFlush(message);
    }

    public String execLinux(String cmd) {
        try {
            String[] cmdArray = {"/bin/sh", "-c", cmd};
            Process process = Runtime.getRuntime().exec(cmdArray);
            LineNumberReader br = new LineNumberReader(new InputStreamReader(
                    process.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
