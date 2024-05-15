package com.peppa.discovery;

import com.peppa.ServiceConfig;

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
}
