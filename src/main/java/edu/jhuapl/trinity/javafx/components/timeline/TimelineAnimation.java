package edu.jhuapl.trinity.javafx.components.timeline;

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

import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import javafx.application.Platform;
import javafx.scene.Scene;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class TimelineAnimation implements Runnable {

    public Thread animationThread;  // ANIMATION THREAD
    protected int delay;            // DELAY TIME IN MILLISECONDS FOR THREAD
    public Scene scene;
    public long[] propRates = new long[]{1, 2, 4, 10, 20, 50, 100}; // ARRAY OF ALLOWED PROPAGATION RATES (multiplier)
    public String[] stringPropRates = new String[]{"1x", "2x", "4x", "10x", "20x", "50x", "100x"}; //prop rate strings - human readable version of current rate
    private int propRateIndex; //index into array of proprate strings
    public long stepsFromStart;       // steps FROM THE ORIGINAL START TIME usually in seconds
    //arbitrary max amounts of steps before looping
    public long animationDurationMax = 1650; //900 seconds == 15 minutes
    public boolean animationLoop = true; //resets if currenMin is > animationDurationMax
    public int playDirection; //positive for forward, negative for backwards
    private boolean isPaused = true;        // CHECK TO SEE IF THE THREAD IS PAUSED
    private int stepSize = 1; // PROPAGATION RATE ASSOCIATED WITH ANIMATION
    public int sampleRate = 1; //how many hz should our realtime sleep delay be

    public TimelineAnimation() {
        // SET THE TIME INTERVAL BETWEEN steps IN MILLISECS
        delay = 1000; //work in seconds by default
        propRateIndex = 0; //index into array of proprate strings
        // INITIALIZE THE NUMBER OF MINUTES FROM THE START
        stepsFromStart = 0;
        // INITIALIZE THE PROPAGATION RATE
        stepSize = 1;
        // SET THE PLAY DIRECTION. 1- FORWARD, -1- BACKWARD
        playDirection = 1;
        // SET THE PAUSED BOOLEAN
        isPaused = true;
        // CREATE NEW THREAD
        animationThread = new Thread(this);
        animationThread.setDaemon(true);
    }

    //attaches animation to scene and adds event handlers
    public void setScene(Scene scene) {
        this.scene = scene;

        this.scene.addEventHandler(TimelineEvent.TIMELINE_DECREASE_PROPRATE, e -> {
            decreasePropRate();
        });
        this.scene.addEventHandler(TimelineEvent.TIMELINE_INCREASE_PROPRATE, e -> {
            increasePropRate();
        });
        this.scene.addEventHandler(TimelineEvent.TIMELINE_PAUSE, e -> {
            pauseThread();
        });
        this.scene.addEventHandler(TimelineEvent.TIMELINE_PLAY, e -> {
            continueThread();
        });
        this.scene.addEventHandler(TimelineEvent.TIMELINE_PLAY_BACKWARDS, e -> {
            continueBackwardsThread();
        });
        this.scene.addEventHandler(TimelineEvent.TIMELINE_RESTART, e -> {
            restartProp();
        });
        this.scene.addEventHandler(TimelineEvent.TIMELINE_SET_LOOP, e -> {
            animationLoop = (boolean) e.object;
        });
        this.scene.addEventHandler(TimelineEvent.TIMELINE_SET_ANIMATIONDURATION, e -> {
            animationDurationMax = (long) e.object;
        });
        this.scene.addEventHandler(TimelineEvent.TIMELINE_SET_SAMPLEFRAMERATE, e -> {
            sampleRate = (int) e.object;
        });
    }

    // START THE THREAD
    public void start() {
        animationThread.start();
    }

    // RESTART THE Animation at time 0
    public void restartProp() {
        stepsFromStart = (long) 0;
    }

    // INCREASE PROPAGATION RATE.
    public synchronized void increasePropRate() {
        propRateIndex = (propRateIndex != propRates.length - 1) ? propRateIndex + 1 : propRateIndex;
    }

    // DECREASE PROPAGATION RATE.
    public synchronized void decreasePropRate() {
        propRateIndex = (propRateIndex != 0) ? propRateIndex - 1 : propRateIndex;
    }

    // PAUSE THREAD
    public synchronized void pauseThread() {
        isPaused = true; // DECREASE BY ONE HOUR
        notifyAll();
    }

    // CONTINUE THREAD
    public synchronized void continueThread() {
        isPaused = false; //
        playDirection = 1;
        notifyAll();
    }

    // CONTINUE BACKWARDS THREAD
    public synchronized void continueBackwardsThread() {
        isPaused = false; //
        playDirection = -1;
        notifyAll();
    }

    // GET THE TIME TO START
    public synchronized long getTimeFromStart() {
        notifyAll();
        return stepsFromStart; // DECREASE BY ONE HOUR
    }

    public synchronized String getStringCurrentPropRate() {
        notifyAll();
        return stringPropRates[propRateIndex];
    }

    public void update() {
        // UPDATE THE CURRENT TIME FROM THE START
        stepsFromStart = stepsFromStart + playDirection;
        if (stepsFromStart > animationDurationMax && animationLoop) {
            stepsFromStart = 0;
        }
        if (stepsFromStart <= animationDurationMax) {
            Long sampleIndex = stepsFromStart;
            Platform.runLater(() -> {
//@DEBUG SMP Useful debugging print
//                System.out.println("StepsFromStart: " + stepsFromStart
//                    + " SampleRate: " + sampleRate
//                );
                scene.getRoot().fireEvent(
                    new TimelineEvent(TimelineEvent.TIMELINE_UPDATE_ANIMATIONTIME));
                if (playDirection > 0) {
                    scene.getRoot().fireEvent(
                        new TimelineEvent(TimelineEvent.TIMELINE_STEP_FORWARD, stepSize));
                } else {
                    scene.getRoot().fireEvent(
                        new TimelineEvent(TimelineEvent.TIMELINE_STEP_BACKWARD, stepSize));
                }
                scene.getRoot().fireEvent(
                    new TimelineEvent(TimelineEvent.TIMELINE_SAMPLE_INDEX, sampleIndex.intValue()));
            });
        }
    }

    @Override
    public void run() {
        while (animationThread.isAlive()) {
            if (!isPaused) {
                //System.out.println("Animation playing, direction: " + playDirection);
                update();
            }
            try {
                //System.out.println("Animation sleeping for: " + delay / propRates[propRateInd] + " ms...");
                Thread.sleep((delay / sampleRate) / propRates[propRateIndex]);
            } catch (InterruptedException ex) {
                Logger.getLogger(TimelineAnimation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
