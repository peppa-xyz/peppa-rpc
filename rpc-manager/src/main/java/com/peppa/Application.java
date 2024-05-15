package com.peppa;

import com.peppa.utils.zookeeper.ZookeeperNode;
import com.peppa.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;

/**
 * 注册中心的管理页面
 * @author: peppa
 * @create: 2024-05-14 22:35
 **/
@Slf4j
public class Application {
    public static void main(String[] args) throws InterruptedException {
        // 帮我们创建基础目录
        // rpc-metadata   (持久节点)
        //  └─ providers （持久节点）
        //  		└─ service1  （持久节点，接口的全限定名）
        //  		    ├─ node1 [data]     /ip:port
        //  		    ├─ node2 [data]
        //            └─ node3 [data]
        //  └─ consumers
        //        └─ service1
        //             ├─ node1 [data]
        //             ├─ node2 [data]
        //             └─ node3 [data]
        //  └─ config

        // 创建一个zookeeper实例
        ZooKeeper zooKeeper = ZookeeperUtils.createZookeeper();

        // 定义节点和数据
        String basePath = "/rpc-metadata";
        String providerPath = basePath + "/providers";
        String consumersPath = basePath + "/consumers";
        ZookeeperNode baseNode = new ZookeeperNode(basePath, null);
        ZookeeperNode providersNode = new ZookeeperNode(providerPath, null);
        ZookeeperNode consumersNode = new ZookeeperNode(consumersPath, null);

        // 创建节点
        List.of(baseNode, providersNode, consumersNode).forEach(node -> {
            ZookeeperUtils.createNode(zooKeeper,node,null, CreateMode.PERSISTENT);
        });

        // 关闭连接
        ZookeeperUtils.close(zooKeeper);
    }
}
