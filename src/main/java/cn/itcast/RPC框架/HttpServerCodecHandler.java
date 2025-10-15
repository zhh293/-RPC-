package cn.itcast.RPC框架;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpServerCodecHandler extends CombinedChannelDuplexHandler<HttpRequestDecoder, HttpResponseEncoder> {

    public HttpServerCodecHandler() {
        this(4096, 8192, 8192);
    }


    public HttpServerCodecHandler(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
        this(maxInitialLineLength, maxHeaderSize, maxChunkSize, false);
    }


    public HttpServerCodecHandler(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders) {
        super(new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders),
                new HttpResponseEncoder());
    }


    public HttpRequestDecoder decoder() {
        return inboundHandler();
    }


    public HttpResponseEncoder encoder() {
        return outboundHandler();
    }

    /**
     * @see CombinedChannelDuplexHandler#handlerAdded(ChannelHandlerContext)
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ChannelPipeline pipeline = ctx.pipeline();
        /*if (pipeline.get(SingleLineFormatte.class) != null) {
            throw new IllegalStateException(
                    "A HTTP server codec must not be added to a pipeline that already contains a " +
                            SingleLineFormatter.class.getSimpleName());
        }*/
        pipeline.addBefore(ctx.name(), null, new HttpServerCodecHandler());
        super.handlerAdded(ctx);
    }
}
