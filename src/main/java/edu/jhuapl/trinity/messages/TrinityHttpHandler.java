package edu.jhuapl.trinity.messages;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import javafx.scene.Scene;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class TrinityHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger LOGGER = Logger.getLogger(TrinityHttpHandler.class.getName());

    private final MessageProcessor processor;

    public TrinityHttpHandler(Scene scene) {
        this.processor = new MessageProcessor(scene);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        // Check HTTP Method
        if (request.method() != HttpMethod.POST) {
            handleInvalidMethodResponse(ctx, request);
            return;
        }
        // Process Request
        String rawContent = request.content().toString(CharsetUtil.UTF_8);
        boolean success = processMessage(rawContent);

        // Generate the response
        if (success) {
            handleOkResponse(ctx, request);
        } else {
            handleErrorResponse(ctx, request, "Malformed JSON");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.log(Level.SEVERE, null, cause);
        ctx.close();
    }

    private void setCommonHeaders(FullHttpRequest request, FullHttpResponse response) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    }

    private void handleInvalidMethodResponse(ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.copiedBuffer(String.format("{\"error\":\"%s\"}", HttpResponseStatus.NOT_FOUND.reasonPhrase()), CharsetUtil.UTF_8));
        this.setCommonHeaders(request, response);
        ctx.writeAndFlush(response);
    }

    private void handleErrorResponse(ChannelHandlerContext ctx, FullHttpRequest request, String errorMsg) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, Unpooled.copiedBuffer(String.format("{\"error\":\"%s\"}", errorMsg), CharsetUtil.UTF_8));
        this.setCommonHeaders(request, response);
        ctx.writeAndFlush(response);
    }

    private void handleOkResponse(ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(String.format("{\"message\":\"%s\"}", HttpResponseStatus.OK.reasonPhrase()), CharsetUtil.UTF_8));
        this.setCommonHeaders(request, response);
        ctx.writeAndFlush(response);
    }

    public boolean processMessage(String messageBody) {
        try {
            processor.process(messageBody);
            return true;
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Malformed JSON from injectMessage", ex);
            return false;
        }
    }
}
