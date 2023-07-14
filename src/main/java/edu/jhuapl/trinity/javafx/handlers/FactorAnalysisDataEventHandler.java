/*-
 * #%L
 * trinity
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

import edu.jhuapl.trinity.data.FactorAnalysisState;
import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.javafx.events.FactorAnalysisDataEvent;
import edu.jhuapl.trinity.javafx.renderers.TrajectoryRenderer;
import javafx.event.EventHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class FactorAnalysisDataEventHandler implements EventHandler<FactorAnalysisDataEvent> {

    public boolean pcaEnabled;
    List<TrajectoryRenderer> renderers;
    HashMap<String, Trajectory> idToTrajectoryMap;

    public FactorAnalysisDataEventHandler(boolean pcaEnabled) {
        this.pcaEnabled = pcaEnabled;
        renderers = new ArrayList<>();
        idToTrajectoryMap = new HashMap<>();
    }

    public void addTrajectoryRenderer(TrajectoryRenderer renderer) {
        renderers.add(renderer);
    }

    public void handleChannelFrameDataEvent(FactorAnalysisDataEvent event) {
        if (event.getEventType().equals(FactorAnalysisDataEvent.NEW_FACTORANALYSIS_STATE)) {
            FactorAnalysisState fas = event.factorAnalysisState;
            if (pcaEnabled) {
//                AnalysisUtils.
            } else {
            }
            if (!idToTrajectoryMap.containsKey(fas.getEntityId())) {
                Trajectory trajectory = convertToTrajectory(fas);
                idToTrajectoryMap.put(fas.getEntityId(), trajectory);

//                Trajectory3D traj3D = JavaFX3DUtils.buildPolyLineFromTrajectory(trialNumber, dayNumber,
//                    trajectory, color, scale, defaultSceneWidth, defaultSceneHeight);

                for (TrajectoryRenderer renderer : renderers) {
//                    renderer.
                }
//                Trajectory trajectory = new Trajectory(channelFrame.getEntityId()
//                    , newStates, newTimes)
            } else {
                //update existing trajectory
            }
        }
//        event.cameraData = null;
    }

    private double getNowAsDouble() {
        return Long.valueOf(Instant.now().toEpochMilli()).doubleValue();
    }

    private Trajectory convertToTrajectory(FactorAnalysisState fas) {
        ArrayList<double[]> states = new ArrayList<>();
        states.add(FactorAnalysisState.mapToStateArray.apply(fas));

        ArrayList<Double> times = new ArrayList<>();
        times.add(getNowAsDouble());

        return new Trajectory(fas.getEntityId(), states, times);
    }

    @Override
    public void handle(FactorAnalysisDataEvent event) {
        handleChannelFrameDataEvent(event);
    }
}
