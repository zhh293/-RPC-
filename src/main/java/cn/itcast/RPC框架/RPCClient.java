package cn.itcast.RPC框架;

import cn.hutool.json.JSONUtil;
import cn.itcast.client.handler.RpcResponseMessageHandler;
import cn.itcast.message.RpcRequestMessage;
import cn.itcast.protocol.MessageCodecSharable;
import cn.itcast.protocol.ProcotolFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Slf4j
public class RPCClient {
    private static ThreadPoolExecutor threadPoolExecutor;
    private  static loadbalance loadbalance;
    public static void main(String[] args) {
        threadPoolExecutor = new ThreadPoolExecutor(4, 16, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
        loadbalance = new loadbalance();
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        RpcResponseHandler RPC_HANDLER = new RpcResponseHandler();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                //连接一旦建立就触发
                @Override
                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                    log.debug("连接已经建立");
                    //展示当前服务器用有哪些服务
                    DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/list");
                    request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                    request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
                    ctx.writeAndFlush(request).addListener(promise -> {
                        if (promise.isSuccess()) {
                            log.debug("请求成功");
                        } else {
                            log.error("请求失败", promise.cause());
                        }
                    });
                }
                @Override
                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                    log.debug("连接已经断开");
                }

                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //加入http的各种处理器
                    ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(new HttpServerCodec());
                    ch.pipeline().addLast(new HttpObjectAggregator(65535));
                    ch.pipeline().addLast(new HttpContentCompressor());
                    ch.pipeline().addLast(RPC_HANDLER);
                }
            });
            loadbalance.Server server = loadbalance.selectServerLeastConnections();
            log.info("当前服务器为：{}",server);
            String object = server.getAddress();
            //按照冒号分割
            String[] split = object.toString().split(":");
            String ip = split[0];
            int port = Integer.parseInt(split[1]);
            Channel channel = bootstrap.connect(ip, port).sync().channel();
            threadPoolExecutor.execute(() -> {
                //写出一个http请求
                PRCRequest sayHello = new PRCRequest(
                        "cn.itcast.server.service.HelloService",
                        "sayHello",
                        String.class,
                        new Class[]{String.class},
                        new Object[]{"张三"}
                );
                String jsonStr = JSONUtil.toJsonStr(sayHello);
                DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/api/rpc");
                defaultFullHttpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                defaultFullHttpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, defaultFullHttpRequest.content().readableBytes());
                defaultFullHttpRequest.content().writeCharSequence(jsonStr, CharsetUtil.UTF_8);
                ChannelFuture future1 = channel.writeAndFlush(defaultFullHttpRequest).addListener(
                        future -> {
                            if (future.isSuccess()) {
                                log.debug("请求成功");
                            } else {
                                log.error("请求失败", future.cause());
                            }
                        }
                );
            });

            threadPoolExecutor.execute(() -> {
                //注册服务
                DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/register");
                defaultFullHttpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                defaultFullHttpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, defaultFullHttpRequest.content().readableBytes());
                ServiceCommon helloservice =ServiceCommon
                        .builder()
                        .serviceName("helloservice")
                        .serviceClass("cn.itcast.server.service.HelloService")
                        .serviceAddress(ip)
                        .servicePort(String.valueOf(port))
                        .build();
                defaultFullHttpRequest.content().writeCharSequence(JSONUtil.toJsonStr(helloservice), CharsetUtil.UTF_8);
                ChannelFuture future1 = channel.writeAndFlush(defaultFullHttpRequest).addListener(
                        future -> {
                            if (future.isSuccess()) {
                                log.debug("注册成功");
                            } else {
                                log.error("注册失败", future.cause());
                            }
                        }
                );
            });
            //调用服务
            threadPoolExecutor.execute(() -> {
                PRCRequest sayHello = new PRCRequest(
                        "helloservice",
                        "sayHello",
                        String.class,
                        new Class[]{String.class},
                        new Object[]{"张三"}
                );
                DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/executeService");
                defaultFullHttpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                defaultFullHttpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, defaultFullHttpRequest.content().readableBytes());
                defaultFullHttpRequest.content().writeCharSequence(JSONUtil.toJsonStr(sayHello), CharsetUtil.UTF_8);
                ChannelFuture future1 = channel.writeAndFlush(defaultFullHttpRequest).addListener(
                        future -> {
                            if (future.isSuccess()) {
                                log.debug("请求成功");
                            } else {
                                log.error("请求失败", future.cause());
                            }
                        }
                );
            });


            /*//下面这里可以人为修改，到时再说吧，想想手动输入怎么办
            ChannelFuture future = channel.writeAndFlush(new PRCRequest(
                    "cn.itcast.server.service.HelloService",
                    "sayHello",
                    String.class,
                    new Class[]{String.class},
                    new Object[]{"张三"}
            )).addListener(promise -> {
                if (!promise.isSuccess()) {
                    Throwable cause = promise.cause();
                    log.error("error", cause);
                }
            });*/

            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("client error", e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
