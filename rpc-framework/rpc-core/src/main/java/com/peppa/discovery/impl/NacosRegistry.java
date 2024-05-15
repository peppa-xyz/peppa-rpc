package com.peppa.discovery.impl;

import com.peppa.ServiceConfig;
import com.peppa.discovery.AbstractRegistry;

import java.net.InetSocketAddress;

/**
 * @author: peppa
 * @create: 2024-05-15 13:26
 **/
public class NacosRegistry extends AbstractRegistry {
    public NacosRegistry(String host, int timeOut) {
        super();
    }

    @Override
    public void register(ServiceConfig<?> serviceConfig) {

    }

    @Override
    public InetSocketAddress lookup(String name) {
        return null;
    }
}
