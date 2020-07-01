package com.swust.server.handler;

import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageHeader;
import com.swust.common.protocol.MessageType;
import com.swust.server.ExtranetServer;
import com.swust.server.ServerManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * @author : LiuMing
 * @date : 2019/11/4 13:54
 * 代理服务器的handler，当请求公网暴露的代理端口时，会转发到相应的客户端，
 * 为了能启用多客户端，禁止@ChannelHandler.Sharable注解
 */

public class RemoteProxyHandler extends ChannelInboundHandlerAdapter {
    private final ChannelHandlerContext clientCtx;
    private final ExtranetServer proxyServer;
    private final int port;

    public RemoteProxyHandler(ChannelHandlerContext clientCtx, ExtranetServer proxyServer, int port) {
        this.clientCtx = clientCtx;
        this.proxyServer = proxyServer;
        this.port = port;
    }

    /**
     * 外部请求外网代理的端口时调用，保存的服务端channel会给内网客户端发送消息 proxyHandler.getCtx().writeAndFlush(message);
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ServerManager.USER_CLIENT_CHANNEL.add(ctx);
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.CONNECTED);
        header.setOpenTcpPort(port);
        header.setChannelId(ctx.channel().id().asLongText());
        clientCtx.writeAndFlush(message);

        proxyServer.getGroup().add(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] data = (byte[]) msg;
        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.DATA);
        message.setData(data);
        header.setChannelId(ctx.channel().id().asLongText());
        clientCtx.writeAndFlush(message);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ServerManager.USER_CLIENT_CHANNEL.remove(ctx);
        proxyServer.getGroup().remove(ctx.channel());

        Message message = new Message();
        MessageHeader header = message.getHeader();
        header.setType(MessageType.DISCONNECTED);
        header.setChannelId(ctx.channel().id().asLongText());
        clientCtx.writeAndFlush(message);
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
            stringBuilder.append(line).append("\n");
        }
        System.out.println(stringBuilder);
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
