package edu.jhuapl.trinity.utils.loaders;

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

import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.data.audio.FFT;
import edu.jhuapl.trinity.data.audio.WaveDecoder;
import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * @author Sean Phillips
 */
public class AudioLoader extends Task {
    Scene scene;
    File file;

    public AudioLoader(Scene scene, File file) {
        this.scene = scene;
        this.file = file;
        setOnSucceeded(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        setOnFailed(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        setOnCancelled(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
    }

    @Override
    protected Void call() throws Exception {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Processing Audio File...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });

        WaveDecoder decoder;
        try {
            double scaling = -1.0;
            int binSize = 512;
            decoder = new WaveDecoder( new FileInputStream( file ) );
            FFT fft = new FFT(binSize, 44100);
            float[] samples = new float[binSize];
            float[] spectrum = new float[binSize / 2 + 1];
            float[] lastSpectrum = new float[binSize / 2 + 1];
            FeatureCollection fc = new FeatureCollection();

            while ((decoder.readSamples(samples)) > 0) {
                fft.forward(samples);
                System.arraycopy(spectrum, 0, lastSpectrum, 0, spectrum.length);
                System.arraycopy(fft.getSpectrum(), 0, spectrum, 0, spectrum.length);

                float flux = 0;
                for (int i = 0; i < spectrum.length; i++)
                    flux += (spectrum[i] - lastSpectrum[i]);

                FeatureVector fv = new FeatureVector();
                fv.setLabel(file.getPath());
                fv.setScore(flux);
                //inverse mirror
                for (int vectorIndex = spectrum.length-1; vectorIndex > 1; vectorIndex--) {
                    fv.getData().add(spectrum[vectorIndex] * scaling); //add projection scaling
                }
                //normal wave after center
                for (int vectorIndex = 0; vectorIndex < spectrum.length; vectorIndex++) {
                    fv.getData().add(spectrum[vectorIndex] * scaling); //add projection scaling
                }
                fc.getFeatures().add(fv);
            }

            Platform.runLater(()-> {
                scene.getRoot().fireEvent(
                    new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc));
            });
            Trajectory trajectory = new Trajectory(file.getPath());
            trajectory.totalStates = fc.getFeatures().size();
            Trajectory.addTrajectory(trajectory);
            Trajectory.globalTrajectoryToFeatureCollectionMap.put(trajectory, fc);
            Platform.runLater(()-> {
                scene.getRoot().fireEvent(
                    new TrajectoryEvent(TrajectoryEvent.NEW_TRAJECTORY_OBJECT, trajectory, fc));
            });
            Thread.sleep(250);
            Platform.runLater(()-> {            
                scene.getRoot().fireEvent(
                    new CommandTerminalEvent("Unrolling frequencies to hypersurface...",
                    new Font("Consolas", 20), Color.GREEN));
                scene.getRoot().fireEvent(
                    new FeatureVectorEvent(FeatureVectorEvent.REQUEST_FEATURE_COLLECTION));
            });
        } catch (Exception ex) {
            Logger.getLogger(AudioLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
