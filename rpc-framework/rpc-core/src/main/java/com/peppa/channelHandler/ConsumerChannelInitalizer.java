package com.peppa.channelHandler;

import com.peppa.channelHandler.handler.MySimpleChannelInboundHandler;
import com.peppa.channelHandler.handler.RpcMessageEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author: peppa
 * @create: 2024-05-15 17:18
 **/
public class ConsumerChannelInitalizer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
                // netty自带的日志处理器 出战/入站处理器
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                // 自定义的出站处理器 消息编码器
                .addLast(new RpcMessageEncoder())
                // 入站处理器
                .addLast(new MySimpleChannelInboundHandler());
    }
}
