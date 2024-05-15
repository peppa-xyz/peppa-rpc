package com.peppa;

import com.peppa.channelHandler.handler.MethodCallHandler;
import com.peppa.channelHandler.handler.RpcMessageDecoder;
import com.peppa.discovery.Registry;
import com.peppa.discovery.RegistryConfig;
import com.peppa.transport.message.RpcRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RpcBootstrap {
    // RpcBootstrap是个单例，我们希望每个应用程序只有一个实例 (饿汉式)
    private static final RpcBootstrap rpcBootstrap = new RpcBootstrap();

    // 定义一些基础配置项
    private String applicationName;
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private int port = 8088;

    // 注册中心
    private Registry registry;

    // 连接的缓存,如果使用InetSocketAddress这样的类做key,一定要看它是不是重写了toString和equals方法
    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);


    // 维护已经发布且暴露的服务列表 key-> interface的全限定名  value -> ServiceConfig
    public final static Map<String, ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

    // 定义全局对外挂起的 completableFuture
    public final static Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);

    private RpcBootstrap() {
        // 构造启动引导程序时需要做一些什么初始化的事

    }

    public static RpcBootstrap getInstance() {
        return rpcBootstrap;
    }

    /**
     * 设置应用名称
     *
     * @param applicationName 应用名称
     * @return this当前实例
     */
    public RpcBootstrap application(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    /**
     * 设置注册中心配置
     *
     * @param registryConfig
     * @return
     */
    public RpcBootstrap registry(RegistryConfig registryConfig) {
        // 尝试使用 registryConfig 去获取一个注册中心，就像工厂设计模式
        this.registry = registryConfig.getRegistry();
        return this;
    }

    /**
     * @param protocolConfig 协议配置
     * @return this当前实例
     */
    public RpcBootstrap protocol(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
        if (log.isDebugEnabled()) {
            log.debug("当前工程使用了:{}协议进行序列化。", protocolConfig.toString());
        }
        return this;
    }

    /**
     * ---------------------------服务提供方的相关api---------------------------------
     */

    /**
     * 发布服务，将接口-》实现，注册到服务中心
     *
     * @param service 封装的需要发布的服务
     * @return this当前实例
     */
    public RpcBootstrap publish(ServiceConfig<?> service) {
        // 抽象了注册中心的概念
        registry.register(service);

        // 1、当服务调用方，通过接口、方法名、具体的方法参数列表发起调用，提供方怎么知道使用哪一个实现
        // (1) new 一个  （2）spring beanFactory.getBean(Class)  (3) 自己维护映射关系
        SERVERS_LIST.put(service.getInterface().getName(), service);
        return this;
    }

    /**
     * 批量发布服务
     *
     * @param services
     * @return
     */
    public RpcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service : services) {
            this.publish(service);
        }
        return this;
    }

    /**
     * 启动netty服务
     */
    public void start() {
        // 1、创建eventLoop，老板只负责处理请求，之后会将请求分发至worker
        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup(10);
        try {

            // 2、需要一个服务器引导程序
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 3、配置服务器
            serverBootstrap = serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new LoggingHandler(LogLevel.INFO))
                                    .addLast(new RpcMessageDecoder())
                                    // 根据请求进行方法调用
                                    .addLast(new MethodCallHandler());
                        }
                    });

            // 4、绑定端口
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            try {
                boss.shutdownGracefully().sync();
                worker.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * ---------------------------服务调用方的相关api---------------------------------
     */

    /**
     * @param reference
     * @return
     */
    public RpcBootstrap reference(ReferenceConfig<?> reference) {
        // 在这个方法里我们是否可以拿到相关的配置项-注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // 1、获取注册中心
        reference.setRegistry(registry);

        return this;
    }


}
