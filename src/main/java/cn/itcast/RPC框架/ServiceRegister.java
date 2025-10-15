package cn.itcast.RPC框架;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.itcast.config.Config;
import com.alibaba.fastjson2.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class ServiceRegister {
    public static Map<String,Class<?>> serviceMap = new ConcurrentHashMap<>();
    public static Map<Class<?>,String> interfaceMap = new ConcurrentHashMap<>();
    public static Map<Class<?>, List<String>> hostMap = new ConcurrentHashMap<>();
    public static Map<Class<?>,List<Integer>> portMap = new ConcurrentHashMap<>();
    public static Map<Class<?>,Object> serviceObjectMap = new ConcurrentHashMap<>();
    public static void register(String interfaceName,String implClass1,String host,int port) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        //根据名称获取类名
        if (StrUtil.isNotBlank(implClass1)){
            Class<?> implClass = Class.forName(implClass1);
            //扫描这个包，找到接口的实现类
            Class<?>[] interfaces = implClass.getInterfaces();
            if(interfaces.length >1){
                throw new RuntimeException("接口只能有一个实现类");
            }
            if(interfaces.length == 0){
                throw new RuntimeException("接口不能没有实现类");
            }
            for(Class<?> clazz : interfaces){
                if(clazz.isInterface()){
                    serviceObjectMap.put(clazz,implClass.newInstance());
                    break;
                }
            }
            serviceMap.put(interfaceName,implClass);
            interfaceMap.put(implClass,interfaceName);
            // 修复服务注册的数据结构问题
            List<String> list = hostMap.get(implClass);
            if(list == null){
                list = new ArrayList<>();  // 使用可变List
                list.add(host);
                hostMap.put(implClass, list);
            } else {
                list.add(host);
            }

            List<Integer> list1 = portMap.get(implClass);
            if(list1 == null){
                list1 = new ArrayList<>();
                list1.add(port);
                portMap.put(implClass, list1);
            }else{
                list1.add(port);
            }
        }

    }
    public static Object getInstance(Class<?> clazz){
        return serviceObjectMap.get(clazz);
    }
    public static Class<?> lookup(String interfaceName){
        return serviceMap.get(interfaceName);
    }
    private static CorsConfig corsConfig() {
        return CorsConfigBuilder.forAnyOrigin()
                .allowedRequestMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS)
                .allowedRequestHeaders("*")
                .build();
    }
    private static String getInterfaceName(Class<?> clazz){
        return interfaceMap.get(clazz);
    }
    //下线服务
    public static void remove(String interfaceName,String host,int port){
        //先移除hostmap和portmap里面的，如果移除完之后集合为空，说明有关服务全部下线了，再删除servicemap和interfacemap
        Class<?> aClass = serviceMap.get(interfaceName);
        if(aClass != null){
            List<String> list = hostMap.get(aClass);
            list.remove(host);
            List<Integer> list1 = portMap.get(aClass);
            list1.remove(port);
            if(list.isEmpty() && list1.isEmpty()){
                serviceMap.remove(interfaceName);
                interfaceMap.remove(aClass);
                serviceObjectMap.remove(aClass);
            }
        }
    }
    public static List<String> serviceList(){
        return serviceMap.keySet().stream().toList();
    }
    //注册中心还需要不断监测服务的状态，及时更新下线的服务
    public static void registerCenter(String registerCenterHost,int registerCenterPort){
        new Thread(new RegisterCenterWorker(registerCenterHost,registerCenterPort)).start();
    }
}
@Slf4j
class RegisterCenterWorker implements Runnable{
    private String registerCenterHost;
    private int registerCenterPort;
    public RegisterCenterWorker(String registerCenterHost,int registerCenterPort){
        this.registerCenterHost = registerCenterHost;
        this.registerCenterPort = registerCenterPort;
    }
    static {
        try {
        //从配置文件中读取并且注册服务
        String[] serviceNames = Config.getServiceNames();
        for(int i = 0; i < serviceNames.length; i++){
                String serviceClass = Config.getServiceClasses()[i];
                Class<?> aClass = Class.forName(serviceClass);
                ServiceRegister.serviceMap.put(serviceNames[i],aClass);
                ServiceRegister.interfaceMap.put(aClass,serviceNames[i]);
            int[] servicePorts = Config.getServicePorts();
            //把数组转换成集合
            List<Integer> list1 = Arrays.stream(servicePorts).boxed().collect(Collectors.toList());
            ServiceRegister.portMap.put(aClass,list1);
            String[] serviceAddresses = Config.getServiceAddresses();
            List<String> list = Arrays.stream(serviceAddresses).toList();
            ServiceRegister.hostMap.put(aClass,list);
        }
        } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
    }
    }
    @Override
    public void run() {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup(8);
        log.info("启动注册中心...");
        //配置一下每个服务的端口，地址，名称和类名，然后这里统一注册，之后新增删除,调用之类的通过http请求辨识服务种类进行对应的操作

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(new ChannelInitializer<io.netty.channel.socket.SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel  ch) throws Exception {
                    ch.pipeline().addLast(new HttpServerCodec());
                    ch.pipeline().addLast(new HttpObjectAggregator(65536));
                    ch.pipeline().addLast(new HttpContentCompressor());
                    ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
                            // 处理HTTP请求
                            String uri = request.uri();

                            String method = request.method().name();
                            //如果url是/register，那么就执行下面这段逻辑
                            if(uri.equals("/register")&& method.equals("POST")){
                                ByteBuf content1 = request.content();
                                String content2 = content1.toString(CharsetUtil.UTF_8);
                                JSONObject data = JSONObject.parseObject(content2);
                                String interfaceName = data.getString("interfaceName");
                                String host = data.getString("host");
                                int port = data.getInteger("port");
                                String implClassName = data.getString("implClassName");
                                ServiceRegister.register(interfaceName,implClassName,host,port);
                                log.debug("注册中心接收到服务端注册请求: {} {} {}", interfaceName, host, port);
                                String responseBody = "success";
                                ByteBuf content = Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8);
                                FullHttpResponse response = new DefaultFullHttpResponse(
                                        HttpVersion.HTTP_1_1,
                                        HttpResponseStatus.OK,
                                        content
                                );
                                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
                                ctx.writeAndFlush(response);
                                ctx.close();
                                log.debug("服务端注册成功: {} {} {}", interfaceName, host, port);
                            }else if(uri.equals("/unregister")&& method.equals("POST")){
                                ByteBuf content1 = request.content();
                                String content2 = content1.toString(CharsetUtil.UTF_8);
                                JSONObject jsonObject = JSONObject.parseObject(content2);
                                JSONObject data = jsonObject.getJSONObject("data");
                                String interfaceName = data.getString("interfaceName");
                                String host = data.getString("host");
                                int port = data.getInteger("port");
                                ServiceRegister.remove(interfaceName,host,port);
                                log.debug("注册中心接收到服务端注销请求: {} {} {}", interfaceName, host, port);
                            }else if(uri.equals("/list")&& method.equals("GET")){
                                String responseBody = "success";
                                ByteBuf content = Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8);
                                FullHttpResponse response = new DefaultFullHttpResponse(
                                        HttpVersion.HTTP_1_1,
                                        HttpResponseStatus.OK,
                                        content
                                );
                                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
                                ctx.writeAndFlush(response);
                                ctx.close();
                                log.debug("服务端返回服务列表: {}", ServiceRegister.serviceList());
                            } else if (uri.equals("/executeService")&& method.equals("POST")) {
                                //我需要获取对应的服务名称，然后调用对应的服务
                                ByteBuf content1 = request.content();
                                String rpcRequest = content1.toString(CharsetUtil.UTF_8);
                                PRCRequest bean = JSONUtil.toBean(rpcRequest, PRCRequest.class);
                                //这一步本来是想在分布式环境下通过ip和port获取服务，但是现在全部是在本地运行，所以这里直接获取服务类
                                String interfaceName = bean.getInterfaceName();
                                Class<?> lookup = ServiceRegister.lookup(interfaceName);
                                //获取代理服务，这里的地址之后肯定是要变得，现在已经完全写死了......
                                ProxyUtil proxyUtil = new ProxyUtil("localhost", 8080);
                                Object proxy = proxyUtil.getProxy(bean, lookup);
                                //将对象写回
                                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                                        HttpVersion.HTTP_1_1,
                                        HttpResponseStatus.OK,
                                        Unpooled.copiedBuffer(JSONUtil.toJsonStr(proxy), CharsetUtil.UTF_8)
                                );
                                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                ctx.writeAndFlush(response);
                                log.debug("服务端返回数据: {}", proxy);
                                ctx.close();
                            }
                        }
                    });
                    //心跳检测每个服务(之后分布式可以拓展)
                    ch.pipeline().addLast(new IdleStateHandler(30, 0, 0));
                    // ChannelDuplexHandler 可以同时作为入站和出站处理器
                    ch.pipeline().addLast(new ChannelDuplexHandler() {
                        // 用来触发特殊事件
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception{
                            IdleStateEvent event = (IdleStateEvent) evt;
                            // 触发了读空闲事件
                            if (event.state() != IdleState.READER_IDLE) {
                                log.debug("已经 5s 没有读到数据了");
                                //说明服务已经下线，下线这个服务`

                                ctx.channel().close();
                            }
                        }
                    });
                }
            });
            ChannelFuture sync = serverBootstrap.bind(this.registerCenterHost, this.registerCenterPort).sync();
            sync.channel().closeFuture().sync();
            log.info("注册中心启动成功");
        }catch (Exception e){
            log.error("注册中心启动失败",e);
            System.exit(1);
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            log.info("注册中心已关闭");
        }
    }
}

