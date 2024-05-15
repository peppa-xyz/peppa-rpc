package com.peppa;

import com.peppa.api.Hello;
import com.peppa.discovery.RegistryConfig;

/**
 * @author peppa
 * @time 2024-05-14 星期二 14:33
 */
public class ConsumerApplication {
    public static void main(String[] args) {
        // 想尽一切办法获取代理对象,使用ReferenceConfig进行封装
        // reference一定有生成代理的模板方法，get()
        ReferenceConfig<Hello> reference = new ReferenceConfig<>();
        reference.setInterface(Hello.class);

        // 代理做了些什么?
        // 1、连接注册中心
        // 2、拉取服务列表
        // 3、选择一个服务并建立连接
        // 4、发送请求，携带一些信息（接口名，参数列表，方法的名字），获得结果
        RpcBootstrap.getInstance()
                .application("first-rpc-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .reference(reference)
                .start()

        System.out.println("++------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Hello hello = reference.get();

        while (true) {
//            try {
//                Thread.sleep(10000);
//                System.out.println("++------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            for (int i = 0; i < 50; i++) {
                String sayHi = hello.sayHello("你好yrpc");
                log.info("sayHi-->{}", sayHi);
            }
        }
    }
}