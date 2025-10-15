package cn.itcast.模拟框架接受前端请求并返回;

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
public class Server {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup(8);
        log.info("启动服务器...");
        log.info("端口号：{}", Config.getServerPort());

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
                            String uri = request.uri();
                            String method = request.method().name();
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

                            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

                            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
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
