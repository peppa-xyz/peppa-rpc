package com.peppa;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * 提供bootstrap单例
 * todo: 这里会有什么问题
 * @author: peppa
 * @create: 2024-05-15 15:09
 **/
@Slf4j
public class NettyBootstrapInitializer {

    private static final Bootstrap bootstrap = new Bootstrap();

    static {
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                // 选择初始化一个什么样的channel
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {

                                String resutl = msg.toString(io.netty.util.CharsetUtil.UTF_8);
                                // 从全局的挂起的请求终寻找与之匹配的待处理的 completableFuture
                                CompletableFuture<Object> completableFuture = RpcBootstrap.PENDING_REQUEST.get(1L);
                                completableFuture.complete(resutl);
                            }
                        });
                    }
                });
    }

    private NettyBootstrapInitializer() {
    }

    public static Bootstrap getBootstrap() {
        return bootstrap;
    }
}
