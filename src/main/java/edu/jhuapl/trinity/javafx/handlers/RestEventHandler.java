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
import edu.jhuapl.trinity.messages.TrinityHttpServer;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import javafx.scene.Scene;

/**
 * @author Sean Phillips
 */
public class RestEventHandler implements EventHandler<RestEvent> {
    Scene scene;
    TrinityHttpServer trinityHttpServer = null;
    Thread serverThread = null;
    
    public RestEventHandler(Scene scene) {
        this.scene = scene;
    }
    
    public void startHttpService() {
        if(null == trinityHttpServer && null == serverThread) {
            System.out.println("Creating Trinity HTTP Server...");
            trinityHttpServer = new TrinityHttpServer(scene);
            serverThread = new Thread(trinityHttpServer, "Trinity HTTP Server");
            serverThread.setDaemon(true);
            serverThread.start();        
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new CommandTerminalEvent("Creating Trinity HTTP Server...",
                        new Font("Consolas", 20), Color.GREEN));
            });
        }
    }
    public void stopHttpService() {
        if(null != trinityHttpServer && null != serverThread) {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new CommandTerminalEvent("Stopping Trinity HTTP Server...",
                        new Font("Consolas", 20), Color.YELLOW));
            });
            trinityHttpServer.stop(); //will kill future inside the runnable.
            trinityHttpServer = null;
            serverThread = null;
        }
    }

    @Override
    public void handle(RestEvent t) {
        if(t.getEventType() == RestEvent.START_RESTSERVER_THREAD) {
            if(null == trinityHttpServer && null == serverThread)
                startHttpService();
        } else if(t.getEventType() == RestEvent.TERMINATE_RESTSERVER_THREAD) {
            if(null != trinityHttpServer && null != serverThread)
                stopHttpService();
        }
    }
}
