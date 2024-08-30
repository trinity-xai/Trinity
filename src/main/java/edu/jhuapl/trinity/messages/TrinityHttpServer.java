package edu.jhuapl.trinity.messages;

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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Scene;
/**
 * @author phillsm1
 */
public class TrinityHttpServer implements Runnable {

    private static final String HTTP_HOST = "0.0.0.0";
    private static final int HTTP_PORT = 8080;
    Scene scene;
    
    public TrinityHttpServer(Scene scene) {
        this.scene = scene;
    }

    @Override
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
                        ch.pipeline().addLast(new TrinityServerHandler(scene));
                    }
                });
            ChannelFuture future = bootstrap.bind(HTTP_HOST, HTTP_PORT).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            Logger.getLogger(TrinityHttpServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static class TrinityServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        Scene scene;
        MessageProcessor processor;
        public TrinityServerHandler(Scene scene) {
            this.scene = scene;
            processor = new MessageProcessor(scene);            
        }
        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            String rawContent = request.content().toString(CharsetUtil.UTF_8);
            final String responseMessage = "Hello from Trinity!";
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(responseMessage, CharsetUtil.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            ctx.writeAndFlush(response);

            injectMessage(rawContent);
            System.out.println("Received Message via Netty...");
            
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            Logger.getLogger(TrinityServerHandler.class.getName()).log(Level.SEVERE, null, cause);
            ctx.close();
        }
        public void injectMessage(String messageBody) {
            try {
                processor.process(messageBody);
            } catch (IOException ex) {
                //Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Malformed JSON from injectMessage");
            }
        }        
    }

}
