package cn.itcast.RPC框架;

import cn.hutool.json.JSONUtil;
import cn.itcast.config.Config;
import cn.itcast.protocol.ProcotolFrameDecoder;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;

@Slf4j
public class RPCServer {
    //先想好有哪些服务，但是他要支持动态扩容，那就很难受了
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup(8);
        log.info("启动服务器...");
        log.info("端口号：{}", Config.getServerPort());
        //启动注册中心
        new Thread(new RegisterCenterWorker(Config.getRegisterCenterHost(),Config.getRegisterCenterPort())).start();
        log.info("注册中心地址：{}", Config.getRegisterCenterHost());
        log.debug("注册中心正在启动");
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(new ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //接收http请求
                    // 服务器端HTTP处理器配置示例
                    ch.pipeline().addLast(new HttpServerCodec());
                    ch.pipeline().addLast(new HttpObjectAggregator(65536));
                    ch.pipeline().addLast(new HttpContentCompressor());
                    ch.pipeline().addLast(new CorsHandler(corsConfig()));
                    ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
                            // 处理HTTP请求
                            //这里判断请求是要干嘛的，然后调用注册中心的方法，新增，删除实例或和调用实例
                            String uri = request.uri();
                            String method = request.method().name();
                            if(method.equals("POST")&& uri.equals("/api/poi")){
                                ByteBuf content1 = request.content();
                                String content2 = content1.toString(CharsetUtil.UTF_8);
                                JSONObject jsonObject = JSONObject.parseObject(content2);
                                JSONObject data = jsonObject.getJSONObject("poiList");
                                JSONArray poiList = data.getJSONArray("pois");
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < poiList.size(); i++) {
                                    JSONObject poi = poiList.getJSONObject(i);
                                    String name = poi.getString("name");
                                    String address = poi.getString("address");
                                    sb.append(name).append(": ").append(address).append("\n");
                                    log.debug("name: {}, address: {}", name, address);
                                }
                                log.debug("请求体{}", content2);

                                log.debug("接收到HTTP请求: {} {}", method, uri);

                                // 创建响应
//                            String responseBody = "<html><body><h1>Hello from Netty!</h1><p>content: " + content2 + "</p></body></html>";
                                //搞一个json形式的响应体
                                // 在构建响应体之前，对字符串进行JSON转义
                                String dataString = sb.toString()
                                        .replace("\\", "\\\\")  // 转义反斜杠
                                        .replace("\"", "\\\"")  // 转义双引号
                                        .replace("\n", "\\n")   // 转义换行符
                                        .replace("\r", "\\r")   // 转义回车符
                                        .replace("\t", "\\t");  // 转义制表符
                                String responseBody="{\"code\":200,\"message\":\"success\",\"data\":\""+dataString+"\"}";
                                ByteBuf content = Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8);

                                FullHttpResponse response = new DefaultFullHttpResponse(
                                        HttpVersion.HTTP_1_1,
                                        HttpResponseStatus.OK,
                                        content
                                );
                                //服务端接收到客户端请求之后，向注册中心发送http请求，然后将结果再返回给客户端。。。。
                                //客户端那边连接的是服务端地址，这里的http请求要发往注册中心，所以地址是完全不同的，需要注意。。。
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
                                            log.debug("服务端接收到客户端注册请求: {} {} {}", interfaceName, host, port);
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
                                            //todo 新建一个http请求发给注册中心
                                            //......
                                            ServiceRegister.remove(interfaceName,host,port);
                                            log.debug("服务端接收到客户端注销请求: {} {} {}", interfaceName, host, port);
                                        }else if(uri.equals("/list")&& method.equals("GET")){
                                            String responseBody = "success";
                                            ByteBuf content = Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8);
                                            //todo 新建一个http请求发给注册中心
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
                                            //todo 新建一个http请求发给注册中心


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

                                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

                                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                            }

                        }
                    });

                    // 用来判断是不是 读空闲时间过长，或 写空闲时间过长
                    // 5s 内如果没有收到 channel 的数据，会触发一个 IdleState#READER_IDLE 事件
                    ch.pipeline().addLast(new IdleStateHandler(5, 0, 0));
                    // ChannelDuplexHandler 可以同时作为入站和出站处理器
                    ch.pipeline().addLast(new ChannelDuplexHandler() {
                        // 用来触发特殊事件
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception{
                            IdleStateEvent event = (IdleStateEvent) evt;
                            // 触发了读空闲事件
                            if (event.state() != IdleState.READER_IDLE) {
                                log.debug("已经 5s 没有读到数据了");
                                ctx.channel().close();
                            }
                        }
                    });
                }
            });
            Channel channel = serverBootstrap.bind("localhost", Config.getServerPort()).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("server error", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
    private static CorsConfig corsConfig() {
        return CorsConfigBuilder.forAnyOrigin()
                .allowedRequestMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS)
                .allowedRequestHeaders("*")
                .build();
    }

}

