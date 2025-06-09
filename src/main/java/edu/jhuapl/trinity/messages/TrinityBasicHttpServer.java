package edu.jhuapl.trinity.messages;

import com.sun.net.httpserver.HttpServer;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class TrinityBasicHttpServer implements Runnable {

    public static final String DEFAULT_HTTP_HOST = "0.0.0.0";
    public static final int DEFAULT_HTTP_PORT = 8080;


    private static final Logger LOG = LoggerFactory.getLogger(TrinityBasicHttpServer.class);
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
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new CommandTerminalEvent("Trinity HTTP Receiver failed to start.",
                        new Font("Consolas", 20), Color.RED));
            });
            LOG.error("Trinity HTTP Server failed to start.", e);
            throw new RuntimeException(e);
        }

        // Set up the context and the handler
        server.createContext("/", new TrinityBasicHttpHandler(scene));

        // Set the executor to use virtual threads
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Start the server
        server.start();

        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new CommandTerminalEvent("Trinity HTTP Receiver Active.",
                    new Font("Consolas", 20), Color.GREEN));
        });
        // Wait until a stop signal is received
        try {
            stopSignal.await();
        } catch (InterruptedException e) {
            LOG.warn("Trinity HTTP Server Stopped.");
        } finally {
            // Stop the server...
            server.stop(0);
        }
    }

}
