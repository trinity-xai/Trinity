package edu.jhuapl.trinity.javafx.components.panes;

/*-
 * #%L
 * trinity-2023.09.29
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
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.media.MediaPlayer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.util.Duration;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Amplitude over time style waveform viz
 * @author Birdasaur
 */
public class WaveformCanvasOverlayPane extends CanvasOverlayPane {
    private static final Logger LOGGER = Logger.getLogger(WaveformCanvasOverlayPane.class.getName());
    private static final double WAVEFORM_HEIGHT_COEFFICIENT = 1.3;
    private static final int BUFFER_SIZE = 4096;
    private double timerXPosition;

    /** 2D graphics context of this canvas */
    private GraphicsContext graphicsContext;

    /** Service to calculate the waveform data */
    private WaveformVisualizationService service;
    /** Waveform data */
    private float[] waveformData;
    /** Background color */
    private Color backgroundColor;
    /** Foreground color */
    private Color foregroundColor;
    /** Graphics context fill color */
    private Color transparentForegroundColor;
    
    public WaveformCanvasOverlayPane(boolean debugBorder, boolean clearCanvas) {
        timerXPosition = 0;
        setDebugBorder(debugBorder);
        setClearCanvasOnResize(clearCanvas);
        graphicsContext = this.getCanvas().getGraphicsContext2D();
        service = new WaveformVisualizationService();
        backgroundColor = Color.web("#252525");
        foregroundColor = Color.ORANGERED;
        transparentForegroundColor = foregroundColor.deriveColor(1, 1, 1, 0.3);

        // Fix the resolution in case the width changes
        widthProperty().addListener(((observable, oldValue, newValue) -> {
            if (waveformData == null) {
                clearWaveform();
            } else {
                if (service != null) {
                    service.processAudioAmplitudes();
                }
                Platform.runLater(()-> {
                paintWaveform();
                });
            }
            if (service != null) {
//                service.updateProgressByTime();
            }            
        }));

        // Fix the resolution in case the height changes
        heightProperty().addListener(((observable, oldValue, newValue) -> {
            if (waveformData == null) {
                clearWaveform();
            } else {
                if (service != null) {
                    service.processAudioAmplitudes();
                }
                Platform.runLater(()-> {
                paintWaveform();
                });
            }
            if (service != null) {
//                service.updateProgressByTime();
            }            
        }));
    }

    /**
     * Start the service that will display the waveform visualization of specified audioFile
     * @param audioFile audio file
     */
    public void startVisualization(File audioFile) {
        service.start(audioFile);
    }
    /**
     * Paint the progress
     */
    public void paintProgress() {
        // Draw a semi transparent Rectangle
        graphicsContext.setFill(transparentForegroundColor);
        graphicsContext.fillRect(0, 0, getTimerXPosition(), getHeight());

        // Draw a vertical line
        graphicsContext.setStroke(Color.WHITE);
        graphicsContext.strokeLine(getTimerXPosition(), 0, getTimerXPosition(), getHeight());
    }
    /**
     * Display a horizontal line on the canvas
     */
    public void clearWaveform() {
        // Draw a background rectangle
        graphicsContext.setFill(getBackgroundColor());
        graphicsContext.fillRect(0, 0, getWidth(), getHeight());

        // Draw a horizontal line
        graphicsContext.setStroke(getForegroundColor());
        graphicsContext.strokeLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
    }

    /**
     * Display the waveform on the canvas
     */
    public void paintWaveform() {
        // Draw a background rectangle
        graphicsContext.setFill(getBackgroundColor());
        graphicsContext.fillRect(0, 0, getWidth(), getHeight());
//        System.out.println("getWidth(): " + getWidth());

        // Draw the waveform
        graphicsContext.setStroke(getForegroundColor());
        if (waveformData != null) {
            for (int i = 0; i < waveformData.length; i++) {
                int value = (int) (waveformData[i] * getHeight());
                int y1 = (int) ((getHeight() - 2 * value) / 1.5);
                int y2 = y1 + 2 * value;
                graphicsContext.strokeLine(i, y1, i, y2);
            }
        } else {
            graphicsContext.strokeLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
        }
    }
    public void resetMedia() {
        if(null != service) {
            service.resetMedia();
        }
    }

    public void pauseMedia() {
        if(null != service) {
            service.pauseMedia();
        }
    }

    public void playMedia() {
        if(null != service) {
            service.playMedia();
        }
    }    

    /**
     * @return the backgroundColor
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @param backgroundColor the backgroundColor to set
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * @return the foregroundColor
     */
    public Color getForegroundColor() {
        return foregroundColor;
    }

    /**
     * @param foregroundColor the foregroundColor to set
     */
    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
        transparentForegroundColor = foregroundColor.deriveColor(1, 1, 1, 0.3);        
    }

    /**
     * @return the timerXPosition
     */
    public double getTimerXPosition() {
        return timerXPosition;
    }

    /**
     * @param timerXPosition the timerXPosition to set
     */
    public void setTimerXPosition(double timerXPosition) {
        this.timerXPosition = timerXPosition;
    }

    /**
     * This class is in charge of calculating the waveform data from a audio file
     */
    private class WaveformVisualizationService extends Service<Boolean> {
        private MediaPlayer audioPlayer = null;
        private Media media = null;
        /** Audio file */
        private File audioFile;
        /** Amplitudes of audio file */
        private int[] audioAmplitudes;

        WaveformVisualizationService() {
            setOnSucceeded(event -> onSucceeded());
            setOnFailed(event -> onFailed());
        }

        /**
         * Create a task for the current service
         * @return task
         */
        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {
                @Override
                protected Boolean call() {
                    try {
                        if (audioFile != null) {
                            Media m = new Media(audioFile.toURI().toString());
                            setMedia(m);
                            MediaPlayer mp = new MediaPlayer(getMedia());
                            mp.setAutoPlay(false);
                            setAudioPlayer(mp);
                            processAudioFile();
                            Platform.runLater(()-> paintWaveform());
                            audioPlayer.currentTimeProperty().addListener(cl -> {
                                paintWaveform();
                                updateProgressByTime();
                            });                            
                            return true;
                        } else {
                            return false;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return false;
                    }
                }

                /**
                 * Calculate and initialize {@link WaveformVisualizationService#audioAmplitudes} and
                 * {@link WaveformVisualization#waveformData}
                 * @throws IOException if encountered IO error
                 * @throws UnsupportedAudioFileException if encountered invalid audio file
                 */
                private void processAudioFile() throws IOException, UnsupportedAudioFileException {
                    if (audioAmplitudes == null) {
                        calcAudioAmplitudes();
                    }
                    processAudioAmplitudes();
 
                    WaveDecoder decoder;
                    try {
                        double scaling = -1.0;
                        int binSize = 512;
                        decoder = new WaveDecoder( new FileInputStream( audioFile ) );
                        FFT fft = new FFT(binSize, 44100);
                        float[] samples = new float[binSize];
                        float[] spectrum = new float[binSize / 2 + 1];
                        float[] lastSpectrum = new float[binSize / 2 + 1];
                        List<Float> spectralFlux = new ArrayList<Float>();
                        int readSamples = 0;
                        FeatureCollection fc = new FeatureCollection();
                        
                        while ((readSamples = decoder.readSamples(samples)) > 0) {
                            System.out.println( "read " + readSamples + " samples" );
                            fft.forward(samples);
                            System.arraycopy(spectrum, 0, lastSpectrum, 0, spectrum.length);
                            System.arraycopy(fft.getSpectrum(), 0, spectrum, 0, spectrum.length);
                            
                            float flux = 0;
                            for (int i = 0; i < spectrum.length; i++)
                                flux += (spectrum[i] - lastSpectrum[i]);
                            spectralFlux.add(flux);
                            System.out.println("Flux count: " + spectralFlux.size());
                            FeatureVector fv = new FeatureVector();
                            fv.setLabel(media.getSource());
                            fv.setScore(flux);
                            //inverse mirror
                            for (int vectorIndex = spectrum.length-1; vectorIndex > 0; vectorIndex--) {
                                fv.getData().add(spectrum[vectorIndex] * scaling); //add projection scaling
                            }
                            //normal wave after center
                            for (int vectorIndex = 0; vectorIndex < spectrum.length; vectorIndex++) {
                                fv.getData().add(spectrum[vectorIndex] * scaling); //add projection scaling
                            }
                            fc.getFeatures().add(fv);
                        }
                        getScene().getRoot().fireEvent(
                            new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc));
                        Trajectory trajectory = new Trajectory(media.getSource());
                        trajectory.totalStates = fc.getFeatures().size();
                        Trajectory.addTrajectory(trajectory);
                        Trajectory.globalTrajectoryToFeatureCollectionMap.put(trajectory, fc);
                        getScene().getRoot().fireEvent(
                            new TrajectoryEvent(TrajectoryEvent.NEW_TRAJECTORY_OBJECT, trajectory, fc));
                    } catch (Exception ex) {
                        Logger.getLogger(WaveformCanvasOverlayPane.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                /**
                 * Calculate and initialize {@link WaveformVisualizationService#audioAmplitudes}
                 * @throws IOException if encountered IO error
                 * @throws UnsupportedAudioFileException if encountered invalid audio file
                 */
                private void calcAudioAmplitudes() throws IOException, UnsupportedAudioFileException {
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
                    AudioFormat baseFormat = audioInputStream.getFormat();

                    // Encoding
                    AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_UNSIGNED;
                    float sampleRate = baseFormat.getSampleRate();
                    int numChannels = baseFormat.getChannels();

                    AudioFormat decodedFormat = new AudioFormat(encoding, sampleRate, 16,
                            numChannels, (numChannels * 2), sampleRate, false);
                    int available = audioInputStream.available();

                    AudioInputStream decodedAudioInputStream = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);

                    // Create a buffer
                    byte[] buffer = new byte[BUFFER_SIZE];

                    // Get the average to a smaller array
                    int maxArrayLength = 100000;
                    int[] finalAmplitudes = new int[maxArrayLength];
                    int samplesPerPixel = available / maxArrayLength;

                    // Variables to calculate finalAmplitudes array
                    int currentSampleCounter = 0;
                    int arrayCellPosition = 0;
                    float currentCellValue = 0.0f;

                    // Variables for the calculation loop
                    int arrayCellValue;

                    // Read all of the available data in chunks
                    while (decodedAudioInputStream.read(buffer, 0, BUFFER_SIZE) > 0) {
                        for (int i = 0; i < buffer.length - 1; i+= 2) {
                            // Calculate the value
                            arrayCellValue = (int) (((((buffer[i + 1] << 8) | buffer[i] & 0xff) << 16) /32767) * WAVEFORM_HEIGHT_COEFFICIENT);

                            if (currentSampleCounter != samplesPerPixel) {
                                ++currentSampleCounter;
                                currentCellValue += Math.abs(arrayCellValue);
                            } else {
                                if (arrayCellPosition != maxArrayLength) {
                                    finalAmplitudes[arrayCellPosition] = finalAmplitudes[arrayCellPosition + 1] = (int) (currentCellValue / samplesPerPixel);
                                }

                                // Fix the variables
                                currentSampleCounter = 0;
                                currentCellValue = 0;
                                arrayCellPosition += 2;
                            }
                        }
                    }
                    audioAmplitudes = finalAmplitudes;
                }

//                /**
//                 * Calculate and initialize {@link WaveformVisualization#waveformData}
//                 */
//                public void processAudioAmplitudes() {
//                    // The width of the resulting waveform panel
//                    int width = (int) getWidth();
//                    waveformData = new float[width];
//                    int samplesPerPixel = audioAmplitudes.length /width;
//
//                    // Calculate
//                    float nValue;
//                    for (int w = 0; w < width; w++) {
//                        int c = w * samplesPerPixel;
//                        nValue = 0.0f;
//
//                        for (int s = 0; s < samplesPerPixel; s++) {
//                            nValue += (Math.abs(audioAmplitudes[c + s]) / 65536.0f);
//                        }
//
//                        waveformData[w] = nValue / samplesPerPixel;
//                    }
//                }
            };
        }
        /**
         * Calculate and initialize {@link WaveformVisualization#waveformData}
         */
        public void processAudioAmplitudes() {
            // The width of the resulting waveform panel
            int width = (int) getWidth();
            waveformData = new float[width];
            int samplesPerPixel = audioAmplitudes.length /width;

            // Calculate
            float nValue;
            for (int w = 0; w < width; w++) {
                int c = w * samplesPerPixel;
                nValue = 0.0f;

                for (int s = 0; s < samplesPerPixel; s++) {
                    nValue += (Math.abs(audioAmplitudes[c + s]) / 65536.0f);
                }

                waveformData[w] = nValue / samplesPerPixel;
            }
        }
        
        public void resetMedia() {
            if(null != audioPlayer) {
                audioPlayer.seek(Duration.ZERO);
            }
        }

        public void pauseMedia() {
            if(null != audioPlayer) {
                audioPlayer.pause();
            }
        }
        
        public void playMedia() {
            if(null != audioPlayer) {
                audioPlayer.play();
            }
        }
        
        public void updateProgressByTime() {
            if(null != audioPlayer) {
                double width = getWidth();
                double currentTime = audioPlayer.getCurrentTime().toSeconds();
                double duration = audioPlayer.getTotalDuration().toSeconds();
                setTimerXPosition(width * (currentTime / duration));
                paintProgress();                
            }
        }
        
        private void start(File audioFile) {
            this.audioFile = audioFile;
            audioAmplitudes = null;
            restart();
        }

        private void onSucceeded() {
            //LOGGER.info("Visualization service succeeded!");
            paintWaveform();
        }

        private void onFailed() {
            LOGGER.log(Level.SEVERE, "Visualization service failed!");
        }

        /**
         * @return the audioPlayer
         */
        public MediaPlayer getAudioPlayer() {
            return audioPlayer;
        }

        /**
         * @param audioPlayer the audioPlayer to set
         */
        public void setAudioPlayer(MediaPlayer audioPlayer) {
            this.audioPlayer = audioPlayer;
        }

        /**
         * @return the media
         */
        public Media getMedia() {
            return media;
        }

        /**
         * @param media the media to set
         */
        public void setMedia(Media media) {
            this.media = media;
        }
    }
}
