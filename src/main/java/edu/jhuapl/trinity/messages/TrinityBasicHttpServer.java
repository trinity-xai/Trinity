package edu.jhuapl.trinity.messages;

import com.sun.net.httpserver.HttpServer;
import javafx.scene.Scene;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrinityBasicHttpServer implements Runnable {

    public static final String DEFAULT_HTTP_HOST = "0.0.0.0";
    public static final int DEFAULT_HTTP_PORT = 8080;


    private static final Logger LOGGER = LoggerFactory.getLogger(TrinityBasicHttpServer.class);
    private final CountDownLatch stopSignal = new CountDownLatch(1);
    private final Scene scene;

    public TrinityBasicHttpServer(Scene scene) {
        this.scene = scene;
    }

    @Override
    public void run() {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(DEFAULT_HTTP_HOST, DEFAULT_HTTP_PORT), 1024);
        } catch (IOException e) {
            LOGGER.error("Trinity HTTP Server failed to start.", e);
            throw new RuntimeException(e);
        }

        // Set up the context and the handler
        server.createContext("/", new TrinityBasicHttpHandler(scene));

        // Set the executor to use virtual threads
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Start the server
        server.start();

        // Wait until a stop signal is received
        try {
            stopSignal.await();
        } catch (InterruptedException e) {
            LOGGER.warn("Trinity HTTP Server Stopped.");
        } finally {
            // Stop the server...
            server.stop(0);
        }
    }

}
