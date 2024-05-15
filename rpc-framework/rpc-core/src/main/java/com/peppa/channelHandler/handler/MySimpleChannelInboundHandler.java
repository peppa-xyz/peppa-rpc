package com.peppa.channelHandler.handler;

import com.peppa.RpcBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.CompletableFuture;

/**
 * 这是一个用来测试的handler
 * @author: peppa
 * @create: 2024-05-15 17:15
 **/
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
        String resutl = msg.toString(io.netty.util.CharsetUtil.UTF_8);
        // 从全局的挂起的请求终寻找与之匹配的待处理的 completableFuture
        CompletableFuture<Object> completableFuture = RpcBootstrap.PENDING_REQUEST.get(1L);
        completableFuture.complete(resutl);
    }
}
