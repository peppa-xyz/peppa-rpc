package com.peppa.discovery.impl;

import com.peppa.Constant;
import com.peppa.ServiceConfig;
import com.peppa.discovery.AbstractRegistry;
import com.peppa.exceptions.DiscoveryException;
import com.peppa.utils.NetUtils;
import com.peppa.utils.zookeeper.ZookeeperNode;
import com.peppa.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author: peppa
 * @create: 2024-05-15 13:09
 **/
@Slf4j
public class ZookeeperRegistry extends AbstractRegistry {

    //
    private ZooKeeper zooKeeper;

    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }


    public ZookeeperRegistry(String host, int timeOut) {
        this.zooKeeper = ZookeeperUtils.createZookeeper(host, timeOut);
    }

    @Override
    public void register(ServiceConfig<?> service) {
        // 配置service，将来调用get方法时，方便生成代理对象
        //服务节点的名称
        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterface().getName();
        //这个节点应该是一个持久节点

        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        // 创建本机的临时节点，ip:port
        //todo: 后续处理端口问题
        String node = parentNode + "/" + NetUtils.getIp() + ":" + 8088;
        if (!ZookeeperUtils.exists(zooKeeper, node, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(node, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.EPHEMERAL);
        }

        if (log.isDebugEnabled()) {
            log.debug("服务{},已经被注册", service.getInterface().getName());
        }
    }

    /**
     * 注册中心的核心目的是什么？拉取合适的服务列表
     *
     * @param serviceName 服务的名称
     * @return 服务列表
     */
    @Override
    public InetSocketAddress lookup(String serviceName) {
        // 1、找到服务对应的节点
        String serviceNode = Constant.BASE_PROVIDERS_PATH + "/" + serviceName;

        // 2、从zk中获取他的子节点, 192.168.12.123:2151
        List<String> children = ZookeeperUtils.getChildren(zooKeeper, serviceNode, null);
        // 获取了所有的可用的服务列表
        List<InetSocketAddress> inetSocketAddresses = children.stream().map(ipString -> {
            String[] ipAndPort = ipString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.parseInt(ipAndPort[1]);
            return new InetSocketAddress(ip, port);
        }).toList();

        if (inetSocketAddresses.isEmpty()) {
            throw new DiscoveryException("未发现任何可用的服务主机.");
        }

        return inetSocketAddresses.get(0);

    }
}
