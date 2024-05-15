package com.peppa.discovery;

import com.peppa.ServiceConfig;

import java.net.InetSocketAddress;

/**
 * 思考注册中心，应该具有什么样的能力
 *
 * @author: peppa
 * @create: 2024-05-15 13:05
 **/
public interface Registry {

    /**
     * 注册服务
     *
     * @param serviceConfig 服务的配置内容
     */
    void register(ServiceConfig<?> serviceConfig);

    /**
     * 从注册中心拉取一个可用的服务
     * @param name 服务的名称
     * @return ip:port
     */
    InetSocketAddress lookup(String name);
}
