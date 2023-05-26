package edu.jhuapl.trinity.data;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
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

import edu.jhuapl.trinity.javafx.events.ChannelFrameDataEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;

import java.util.HashMap;

/**
 * @author Sean Phillips
 */
public class TrajectoryTracker implements EventHandler<ChannelFrameDataEvent> {
    public HashMap<String, Trajectory> entityIDMap;
    public Scene scene;

    public TrajectoryTracker(Scene scene) {
        entityIDMap = new HashMap<>();
        this.scene = scene;
        this.scene.addEventHandler(ChannelFrameDataEvent.NEW_CHANNEL_FRAME, this);
    }

    @Override
    public void handle(ChannelFrameDataEvent t) {
        //@TODO SMP Do stuff
        String currentId = t.channelFrame.getEntityId();
        if (entityIDMap.containsKey(currentId)) {
            //update existing Trajectory
        } else {
            //create new Trajectory
        }
    }

}
