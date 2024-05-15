package com.peppa;

import com.peppa.discovery.Registry;
import com.peppa.discovery.RegistryConfig;
import com.peppa.exceptions.NetworkException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author: peppa
 * @create: 2024-05-15 10:10
 **/
@Slf4j
public class ReferenceConfig<T> {

    private Class<T> interfaceRef;

    private Registry registry;

    public void setInterface(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    /**
     * 代理设计模式，生成一个api接口的代理对象，helloYrpc.sayHi("你好");
     *
     * @return 代理对象
     */
    public T get() {
        // 此处一定是使用动态代理完成了一些工作
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<T>[] classes = new Class[]{interfaceRef};

        // 使用动态代理生成代理对象
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                log.info("method:{}", method.getName());
                log.info("args:{}", args);

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
                    channel = channelFuture.get(3, TimeUnit.SECONDS);

                    RpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }

                if (channel == null) {
                    log.error("获取或建立与【{}】通道时发生异常", address);
                    throw new NetworkException("获取channel时发生异常");
                }

                // 将数据写出后，异步获取返回结果
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                // todo: 需要将 completableFuture 暴露出去
                RpcBootstrap.PENDING_REQUEST.put(1L, completableFuture);

                channel.writeAndFlush(Unpooled.copiedBuffer("hello".getBytes())).addListener((ChannelFutureListener) promise ->{
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
        });

        return (T) helloProxy;

    }


    public Class<T> getInterface() {
        return interfaceRef;
    }

    public void setInterfaceRef(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }


    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }
}
