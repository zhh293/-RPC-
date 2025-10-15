package cn.itcast.RPC框架;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;

@Slf4j
public class ProxyUtil {
    private String host;
    private int port;
    public ProxyUtil(String host, int port) {
        this.host = host;
        this.port = port;
    }
    public  <T> T getProxy(PRCRequest request, Class<T> interfaceClass) {
        log.info("getProxy: {}", interfaceClass.getName());
        log.info("即将调用host为{},port为{}的远程服务", host, port);
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, new RemoteInvocationHandler(host, port,request,interfaceClass ));
    }
}
