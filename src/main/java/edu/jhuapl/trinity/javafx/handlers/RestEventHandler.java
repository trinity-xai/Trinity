package edu.jhuapl.trinity.javafx.handlers;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2024 Sean Phillips
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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

    private static final Logger LOGGER = LoggerFactory.getLogger(RestEventHandler.class);

    private final Scene scene;
    private ExecutorService executor;
    private Future<?> serverFuture;


    public RestEventHandler(Scene scene) {
        this.scene = scene;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void startHttpService() {
        if (serverFuture != null && !serverFuture.isDone()) {
            LOGGER.warn("Trinity HTTP Server is already running.");
            return;
        }

        LOGGER.info("Creating Trinity HTTP Server...");
        serverFuture = executor.submit(new TrinityBasicHttpServer(scene));

        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new CommandTerminalEvent("Creating Trinity HTTP Server...",
                    new Font("Consolas", 20), Color.GREEN));
        });
    }

    public void stopHttpService() {
        if (serverFuture == null || serverFuture.isDone()) {
            LOGGER.warn("Trinity HTTP Server is not running.");
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
