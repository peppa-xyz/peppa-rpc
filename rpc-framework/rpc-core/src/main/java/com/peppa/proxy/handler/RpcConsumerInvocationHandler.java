package com.peppa.proxy.handler;

import com.peppa.NettyBootstrapInitializer;
import com.peppa.RpcBootstrap;
import com.peppa.discovery.Registry;
import com.peppa.exceptions.DiscoveryException;
import com.peppa.exceptions.NetworkException;
import com.peppa.transport.message.RequestPayload;
import com.peppa.transport.message.RpcRequest;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 该类封装了客户端通信的基础逻辑，每一个代里对象的远程调用过程都封装在了invoke方法中
 * 1. 发现可用服务    2. 建立连接    3. 发送请求  4. 接收响应
 *
 * @author: peppa
 * @create: 2024-05-15 16:43
 **/
@Slf4j
public class RpcConsumerInvocationHandler implements InvocationHandler {

    // 此处需要一个注册中心和一个接口
    private final Registry registry;
    private final Class<?> interfaceRef;

    public RpcConsumerInvocationHandler(Registry registry, Class<?> interfaceRef) {
        this.registry = registry;
        this.interfaceRef = interfaceRef;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//        log.info("method:{}", method.getName());
//        log.info("args:{}", args);

        // 1. 发现服务，从注册中心，寻找一个可用的服务
        // 传入服务的名字,返回ip+port
        InetSocketAddress address = registry.lookup(interfaceRef.getName());
        if (log.isDebugEnabled()) {
            log.info("服务调用方，发现服务：{}", address);
        }

        // 2. 使用netty连接服务器，发送调用的 服务的名字+方法的名字+参数列表，得到结果

        // q: 整个连接过程放在这里不合适，这样会导致每次调用都会产生一个新的netty连接
        // a: 解决方案？ 缓存channel，如果缓存中没有，则创建新的连接，并进行缓存
                /*
                // 定义线程池，EventLoopGroup
                NioEventLoopGroup group = new NioEventLoopGroup();

                // 启动一个客户端需要一个辅助类，bootstrap
                Bootstrap bootstrap = new Bootstrap();
                try {
                    bootstrap = bootstrap.group(group)
                            .remoteAddress(address)
                            // 选择初始化一个什么样的channel
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel socketChannel) throws Exception {
                                    socketChannel.pipeline().addLast(null);
                                }
                            });

                    // 尝试连接服务器
                    ChannelFuture channelFuture = bootstrap.connect().sync();

                    // 获取channel，并且写出数据
                    channelFuture.channel().writeAndFlush(Unpooled.copiedBuffer("hello netty".getBytes(StandardCharsets.UTF_8)));

                    // 阻塞程序，等到接受消息
                    channelFuture.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        group.shutdownGracefully().sync();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                */
        // 尝试获取一个可用的通道
        Channel channel = getAvailableChannel(address);
        if (log.isDebugEnabled()) {
            log.debug("获取了和【{}】建立的连接通道，准备发送数据", address);
        }

        // 3. 封装报文
        RequestPayload requestPayload = RequestPayload.builder()
                .interfaceName(interfaceRef.getName())
                .methodName(method.getName())
                .parametersType(method.getParameterTypes())
                .parametersValue(args)
                .returnType(method.getReturnType())
                .build();

        // todo 需要对请求id和各种类型做处理
        RpcRequest rpcRequest = RpcRequest.builder()
                .requestId(1L)
                .compressType((byte) 1)
                .requestType((byte) 1)
                .serializeType((byte) 1)
                .requestPayload(requestPayload)
                .build();


        // 4. 将报文写出后，异步获取返回结果
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        //  completableFuture 暴露出去
        RpcBootstrap.PENDING_REQUEST.put(1L, completableFuture);

        // 这里这几 writeAndFlush 写出一个请求，这个请求的实例就会进入pipeline执行出站的一系列操作
        // 我们可以想象得到，第一个出站程序一定是将 rpcRequest --> 二进制的报文
        channel.writeAndFlush(rpcRequest).addListener((ChannelFutureListener) promise -> {
            // 当前的promise的返回结果是 writeAndFlush 的返回结果
            // 一旦数据被写出去，这个promise就结束了
            // 但是我们需要的是服务端给我们的返回值
            // 所以应该将 completableFuture 挂起并暴露，并且在服务提供方做出响应的时候调用 complete 方法
//                    if (promise.isDone()) {
//                        completableFuture.complete(promise.getNow());
//                    } else
            if (!promise.isSuccess()) {
                completableFuture.completeExceptionally(promise.cause());
            }
        });


        // 如果没有地方处理这个 completableFuture，那么这个 completableFuture 就会一直阻塞等待complete方法的执行
        // q: 我们需要在哪里处理这个 completableFuture 呢？
        // a: pipeline的最终handler去处理结果
        return completableFuture.get(3, TimeUnit.SECONDS);
    }

    /**
     * 根据地址获取一个可用的channel
     *
     * @param address
     * @return
     */
    private Channel getAvailableChannel(InetSocketAddress address) {
        // 尝试从全局的缓存中获取一个通道
        Channel channel = RpcBootstrap.CHANNEL_CACHE.get(address);

        if (channel == null) {
            // 创建一个新的channel，并缓存

            // 同步方式获取channel
//                    channel = NettyBootstrapInitializer.getBootstrap()
//                            .connect(address).await().channel();

            //异步的方式获取channel
            CompletableFuture<Channel> channelFuture = new CompletableFuture<>();
            NettyBootstrapInitializer.getBootstrap().connect(address).addListener(
                    (ChannelFutureListener) promise -> {
                        if (promise.isDone()) {
                            if (log.isDebugEnabled()) {
                                log.info("已经和【{}】服务器建立了连接", address);
                            }
                            channelFuture.complete(promise.channel());
                        } else if (!promise.isSuccess()) {
                            channelFuture.completeExceptionally(promise.cause());
                        }
                    });
            // 阻塞等待
            try {
                channel = channelFuture.get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("获取channel时发生异常", e);
                throw new DiscoveryException(e);
            }

            RpcBootstrap.CHANNEL_CACHE.put(address, channel);
        }

        if (channel == null) {
            log.error("获取或建立与【{}】通道时发生异常", address);
            throw new NetworkException("获取channel时发生异常");
        }

        return channel;
    }
}
