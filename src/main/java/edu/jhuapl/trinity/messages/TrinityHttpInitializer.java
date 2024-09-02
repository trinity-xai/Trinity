package edu.jhuapl.trinity.messages;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import javafx.scene.Scene;

public class TrinityHttpInitializer extends ChannelInitializer<SocketChannel> {

    private final Scene scene;

    public TrinityHttpInitializer(Scene scene) {
        this.scene = scene;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new HttpServerCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
        ch.pipeline().addLast(new TrinityHttpHandler(scene));
    }
}
