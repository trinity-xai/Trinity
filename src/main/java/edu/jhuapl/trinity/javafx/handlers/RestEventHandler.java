package edu.jhuapl.trinity.javafx.handlers;

import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import edu.jhuapl.trinity.messages.TrinityBasicHttpServer;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Sean Phillips
 */
public class RestEventHandler implements EventHandler<RestEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(RestEventHandler.class);

    private final Scene scene;
    private ExecutorService executor;
    private Future<?> serverFuture;


    public RestEventHandler(Scene scene) {
        this.scene = scene;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void startHttpService() {
        if (serverFuture != null && !serverFuture.isDone()) {
            LOG.warn("Trinity HTTP Server is already running.");
            return;
        }

        LOG.info("Creating Trinity HTTP Server...");
        serverFuture = executor.submit(new TrinityBasicHttpServer(scene));

        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new CommandTerminalEvent("Creating Trinity HTTP Server...",
                    new Font("Consolas", 20), Color.GREEN));
        });
    }

    public void stopHttpService() {
        if (serverFuture == null || serverFuture.isDone()) {
            LOG.warn("Trinity HTTP Server is not running.");
            return;
        }
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new CommandTerminalEvent("Stopping Trinity HTTP Server...",
                    new Font("Consolas", 20), Color.YELLOW));
        });

        // Cancel the running task and interrupt if running
        serverFuture.cancel(true);
        // Disable new tasks from being submitted
        executor.shutdown();
        // Cancel currently executing tasks (if any)
        executor.shutdownNow();

        // Reset the executor service for future use
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void handle(RestEvent t) {
        if (t.getEventType() == RestEvent.START_RESTSERVER_THREAD) {
            startHttpService();
        } else if (t.getEventType() == RestEvent.TERMINATE_RESTSERVER_THREAD) {
            stopHttpService();
        }
    }
}
