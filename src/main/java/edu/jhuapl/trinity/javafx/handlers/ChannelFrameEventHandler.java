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
package edu.jhuapl.trinity.javafx.handlers;

import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.data.messages.ChannelFrame;
import edu.jhuapl.trinity.javafx.events.ChannelFrameDataEvent;
import edu.jhuapl.trinity.javafx.renderers.TrajectoryRenderer;
import javafx.event.EventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class ChannelFrameEventHandler implements EventHandler<ChannelFrameDataEvent> {

    List<TrajectoryRenderer> renderers;
    HashMap<String, Trajectory> idToTrajectoryMap;
    public boolean pcaEnabled;

    public ChannelFrameEventHandler(boolean pcaEnabled) {
        this.pcaEnabled = pcaEnabled;
        renderers = new ArrayList<>();
        idToTrajectoryMap = new HashMap<>();
    }

    public void addTrajectoryRenderer(TrajectoryRenderer renderer) {
        renderers.add(renderer);
    }

    public void handleChannelFrameDataEvent(ChannelFrameDataEvent event) {
        if (event.getEventType().equals(ChannelFrameDataEvent.NEW_CHANNEL_FRAME)) {
            ChannelFrame channelFrame = event.channelFrame;
            if (pcaEnabled) {
//                AnalysisUtils.
            } else {

            }
            if (!idToTrajectoryMap.containsKey(channelFrame.getEntityId())) {
//                channelFrame.
//                Trajectory trajectory = new Trajectory(channelFrame.getEntityId()
//                    , newStates, newTimes)
            }
        }
//        event.cameraData = null;
    }

    @Override
    public void handle(ChannelFrameDataEvent event) {
        handleChannelFrameDataEvent(event);
    }
}
