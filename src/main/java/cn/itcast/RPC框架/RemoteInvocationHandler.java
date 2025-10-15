package cn.itcast.RPC框架;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

@Slf4j
public class RemoteInvocationHandler implements InvocationHandler {
    private String host;
    private int port;
    private PRCRequest request;
    private Class<?> interfaceClass;
    private CircuitBreaker circuitBreaker;
    private RetryHandler retryHandler;
    public RemoteInvocationHandler(String host, int port, PRCRequest request, Class<?> interfaceClass) {
        this.host = host;
        this.port = port;
        this.request = request;
        this.interfaceClass = interfaceClass;
        // 初始化熔断器和重试机制
        this.circuitBreaker = new CircuitBreaker(5, 10000, 3);
        this.retryHandler = new RetryHandler(3, 1000);
    }
    @Override
    public  Object  invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 熔断器检查
        if (!circuitBreaker.canExecute()) {
            throw new RuntimeException("服务熔断中，无法执行调用");
        }
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameterValue = request.getParameterValue();
        Class<?> returnType = request.getReturnType();
        log.info("methodName: {}, parameterTypes: {}, parameterValue: {}, returnType: {}", methodName, parameterTypes, parameterValue, returnType);
        //进行方法的调用，找到对应的方法，传入对应的参数，返回对应的结果
        //获取方法列表
        if(method.getDeclaringClass()==Object.class){
            return method.invoke(this,args);
        }
        try {
            Object result = retryHandler.executeWithRetry(() -> {
                Object instance = ServiceRegister.getInstance(interfaceClass);
                if (instance == null) {
                    throw new RuntimeException("实例未找到");
                }
                Method targetMethod;
                try {
                    targetMethod = interfaceClass.getMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("方法未找到");
                }
                try {
                    return targetMethod.invoke(instance, parameterValue);
                } catch (Exception e) {
                    throw new RuntimeException("方法调用异常");
                }
            });
            // 记录成功
            circuitBreaker.recordSuccess();
            return result;
        }catch (Exception e){
            // 熔断器记录失败
            circuitBreaker.recordFailure();
            throw new RuntimeException("调用失败: "+e.getMessage());
        }
    }
}
