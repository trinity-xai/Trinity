package edu.jhuapl.trinity.messages;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import javafx.scene.Scene;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class TrinityHttpServer implements Runnable {

    public static final String DEFAULT_HTTP_HOST = "0.0.0.0";
    public static final int DEFAULT_HTTP_PORT = 8080;

    private static final Logger LOGGER = Logger.getLogger(TrinityHttpServer.class.getName());
    private final Scene scene;

    public TrinityHttpServer(Scene scene) {
        this.scene = scene;
    }

    @Override
    public void run() {
        EventLoopGroup parentGroup = new NioEventLoopGroup(1);
        EventLoopGroup childGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(parentGroup, childGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new TrinityHttpInitializer(scene));
            ChannelFuture future = b.bind(DEFAULT_HTTP_HOST, DEFAULT_HTTP_PORT).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            LOGGER.log(Level.WARNING, "Trinity HTTP Server Stopped.");
        } finally {
            childGroup.shutdownGracefully();
            parentGroup.shutdownGracefully();
        }
    }

}
