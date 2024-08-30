package edu.jhuapl.trinity.messages;

import io.netty.bootstrap.ServerBootstrap;
import static io.netty.buffer.Unpooled.copiedBuffer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author phillsm1
 */
public class NettyServer implements Runnable {
    public NettyServer() {

    }

    @Override
    public void run() {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new EchoServerHandler());
                        }
                    });
            ChannelFuture future = bootstrap.bind(8080).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            Logger.getLogger(NettyServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    public static class EchoServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            System.out.println("Received from Netty: \n" + msg.toString());
//            ctx.write(response);
            
        }
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            final String responseMessage = "Hello from Trinity!";

            FullHttpResponse response = new DefaultFullHttpResponse(
              HttpVersion.HTTP_1_1,
              HttpResponseStatus.OK,
              copiedBuffer(responseMessage.getBytes())
            );            
//            response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
//              "text/plain");
//            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
//              responseMessage.length());
                                        
            ctx.writeAndFlush(response);
        }
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}