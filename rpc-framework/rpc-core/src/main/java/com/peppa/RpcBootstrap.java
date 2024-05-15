package com.peppa;

import com.peppa.discovery.Registry;
import com.peppa.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
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

    // 维护已经发布且暴露的服务列表 key-> interface的全限定名  value -> ServiceConfig
    public final static Map<String, ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
        return this;
    }


}
