package com.swust.client;

import com.alibaba.fastjson.JSON;
import com.swust.common.protocol.Message;
import com.swust.common.protocol.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author : LiuMing
 * 2019/11/4 13:42
 * 客户端 handler
 */
@Slf4j
@ChannelHandler.Sharable
public class FileSyncClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private String monitorPath = "";

    public FileSyncClientHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        ctx.executor().scheduleAtFixedRate(() -> {
            Channel channel = ctx.channel();
            //每1M分块
            FileUtil.FileInfo fileInfo = FileUtil.blockFile(new File(monitorPath), 1024 * 1024);

            ctx.writeAndFlush(JSON.toJSONBytes(fileInfo));

        }, 1000, 1, TimeUnit.MINUTES);


        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int dataLen = msg.readInt();

        String data = msg.toString(msg.readerIndex(), dataLen, StandardCharsets.UTF_8);

        List<TransFileData> transFileDataList = JSON.parseArray(data, TransFileData.class);

        //回写缺失文件
        transFileDataList.forEach(transFileData -> {
            File file = new File(transFileData.getFilePath());
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                int start = transFileData.getStart();
                int end = transFileData.getEnd();
                byte[] bytes = new byte[end - start];
                raf.read(bytes, start, bytes.length);
                transFileData.setData(bytes);
                ctx.writeAndFlush(JSON.toJSONBytes(transFileData));
            } catch (Exception e) {

            }
        });

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

    @Data
    public static class TransFileData {
        private String filePath;
        private int start;
        private int end;
        private byte[] data;
    }
}
