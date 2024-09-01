package edu.jhuapl.trinity.messages;

import edu.jhuapl.trinity.javafx.events.RestEvent;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import javafx.scene.Scene;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;

/**
 * @author phillsm1
 */
public class TrinityHttpServer implements Runnable, EventHandler<RestEvent> {

    private static final Logger LOGGER = Logger.getLogger(TrinityServerHandler.class.getName());
    private static final String HTTP_HOST = "0.0.0.0";
    private static final int HTTP_PORT = 8080;
    private final Scene scene;
    private TrinityServerHandler trinityServerHandler;

    public TrinityHttpServer(Scene scene) {
        this.scene = scene;
        trinityServerHandler = new TrinityServerHandler(scene);
    }

    @Override
    public void run() {
        EventLoopGroup parentGroup = new NioEventLoopGroup();
        EventLoopGroup childGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap().group(parentGroup, childGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new HttpServerCodec());
                    ch.pipeline().addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
                    ch.pipeline().addLast(trinityServerHandler);
                }
            });
            ChannelFuture future = bootstrap.bind(HTTP_HOST, HTTP_PORT).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } finally {
            childGroup.shutdownGracefully();
            parentGroup.shutdownGracefully();
        }
    }

    @Override
    public void handle(RestEvent t) {
        if(t.getEventType() == RestEvent.START_RESTSERVER_PROCESSING) {
            trinityServerHandler.processingMessages = true;
        } else if(t.getEventType() == RestEvent.STOP_RESTSERVER_PROCESSING) {
            trinityServerHandler.processingMessages = false;
        }
    }

    public static class TrinityServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private static final Logger LOGGER = Logger.getLogger(TrinityServerHandler.class.getName());

        private final MessageProcessor processor;
        public boolean processingMessages = true;
        
        public TrinityServerHandler(Scene scene) {
            this.processor = new MessageProcessor(scene);
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            // Check HTTP Method
            if (request.method() != HttpMethod.POST) {
                handleInvalidMethodResponse(ctx, request);
                return;
            }
            //Don't process if we have paused processing
            if(!processingMessages) {
                handleErrorResponse(ctx, request, "Not Receiving.");
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

}
