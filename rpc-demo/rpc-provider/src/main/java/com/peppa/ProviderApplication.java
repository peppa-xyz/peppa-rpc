package com.peppa;

import com.peppa.api.Hello;
import com.peppa.apiImpl.HelloImpl;
import com.peppa.discovery.RegistryConfig;

public class ProviderApplication {
    public static void main(String[] args) {
        // 服务提供方，需要注册服务，启动服务
        // 1、封装要发布的服务
        ServiceConfig<Hello> service = new ServiceConfig<>();
        service.setInterface(Hello.class);
        service.setRef(new HelloImpl());
        // 2、定义注册中心

        // 3、通过启动引导程序，启动服务提供方
        //   （1） 配置 -- 应用的名称 -- 注册中心 -- 序列化协议 -- 压缩方式
        //   （2） 发布服务
        RpcBootstrap.getInstance()
                .application("first-rpc-provider")
                // 配置注册中心
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .protocol(new ProtocolConfig("jdk"))
                .publish(service)
                // 启动服务
                .start();
    }
}
