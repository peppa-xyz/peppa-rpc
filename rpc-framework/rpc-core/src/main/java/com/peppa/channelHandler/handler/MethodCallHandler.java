package com.peppa.channelHandler.handler;

import com.peppa.RpcBootstrap;
import com.peppa.ServiceConfig;
import com.peppa.transport.message.RequestPayload;
import com.peppa.transport.message.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author: peppa
 * @create: 2024-05-15 22:00
 **/
public class MethodCallHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static final Logger log = LoggerFactory.getLogger(MethodCallHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        // 1. 获取负载内容
        RequestPayload requestPayload = rpcRequest.getRequestPayload();

        // 2. 根据负载内容进行方法调用
        Object result = callTargetMethod(requestPayload);

        // 3. 封装响应


        // 4. 写出响应
        channelHandlerContext.channel().writeAndFlush(null);


    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String interfaceName = requestPayload.getInterfaceName();
        String methodName = requestPayload.getMethodName();
        Class<?>[] parameterTypes = requestPayload.getParametersType();
        Object[] parametersValue = requestPayload.getParametersValue();

        // 寻找合适的实现类完成方法调用
        ServiceConfig<?> serviceConfig = RpcBootstrap.SERVERS_LIST.get(interfaceName);

        Object refImpl = serviceConfig.getRef();

        // 通过反射调用 1. 获取方法对象  2. 执行invoke方法
        Object returnValue = null;
        try {
            Class<?> aClass = refImpl.getClass();
            Method method = aClass.getMethod(methodName, parameterTypes);
            returnValue = method.invoke(refImpl, parametersValue);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            log.error("调用服务【{}】的方法【{}】时出现异常", interfaceName, methodName, e);
            throw new RuntimeException(e);
        }
        return returnValue;
    }
}
