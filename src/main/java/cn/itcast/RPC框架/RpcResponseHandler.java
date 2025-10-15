package cn.itcast.RPC框架;

import cn.itcast.message.RpcResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            //这里面是调用的结果，我们现在取出来并且展示到控制台
        ByteBuf content = msg.content();
        //获取其中的请求体内容
        String json = content.toString();
        log.debug("服务器返回的数据:{}", json);
    }
}
