package edu.jhuapl.trinity.javafx.components.panes;

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

import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.loaders.AudioLoader;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Amplitude over time style waveform viz
 *
 * @author Sean Phillips
 */
public class WaveformCanvasOverlayPane extends CanvasOverlayPane {
    private static final Logger LOG = LoggerFactory.getLogger(WaveformCanvasOverlayPane.class);
    public static final double DEFAULT_WAVEFORM_HEIGHT_COEFFICIENT = 1.5;
    private static final int BUFFER_SIZE = 4096;
    public static double AMPLITUDE_SCALE_MAX = 32767.0f; //65536.0f;

    private double coeffScale = DEFAULT_WAVEFORM_HEIGHT_COEFFICIENT;
    private double timerXPosition;

    /**
     * 2D graphics context of this canvas
     */
    private GraphicsContext graphicsContext;

    /**
     * Service to calculate the waveform data
     */
    private WaveformVisualizationService service;
    /**
     * Waveform data
     */
    private float[] waveformData;
    /**
     * Background color
     */
    private Color backgroundColor;
    /**
     * Foreground color
     */
    private Color foregroundColor;
    /**
     * Graphics context fill color
     */
    private Color transparentForegroundColor;

    public WaveformCanvasOverlayPane(boolean debugBorder, boolean clearCanvas) {
        timerXPosition = 0;
        setDebugBorder(debugBorder);
        setClearCanvasOnResize(clearCanvas);
        graphicsContext = this.getCanvas().getGraphicsContext2D();
        service = new WaveformVisualizationService();
        backgroundColor = Color.BLACK;
        foregroundColor = Color.TOMATO;
        transparentForegroundColor = foregroundColor.deriveColor(1, 1, 1, 0.3);

        // Fix the resolution in case the width changes
        widthProperty().addListener(((observable, oldValue, newValue) -> {
            if (waveformData == null) {
                clearWaveform();
            } else {
                if (service != null && null != service.audioAmplitudes) {
                    service.processAudioAmplitudes();
                }
            }
            Platform.runLater(() -> paintWaveform());
        }));

        // Fix the resolution in case the height changes
        heightProperty().addListener(((observable, oldValue, newValue) -> {
            if (waveformData == null) {
                clearWaveform();
            } else {
                if (service != null && null != service.audioAmplitudes) {
                    service.processAudioAmplitudes();
                }
            }
            Platform.runLater(() -> paintWaveform());
        }));
        setMinHeight(200);
        setMinWidth(400);
        Platform.runLater(() -> {
            clearWaveform();
            paintWaveform();
        });

        ImageView clearDataIV = ResourceUtils.loadIcon("clear", 32);
        MenuItem clearDataItem = new MenuItem("Clear Data", clearDataIV);
        clearDataItem.setOnAction(e -> {
            //Platform.runLater(()-> {
            service.audioAmplitudes = null;
            clearWaveform();
        });
        ContextMenu cm = new ContextMenu(clearDataItem);
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);
        cm.setOpacity(0.85);
        this.
            setOnMouseClicked((MouseEvent e) -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    if (!cm.isShowing())
                        cm.show(this.getParent(), e.getScreenX(), e.getScreenY());
                    else
                        cm.hide();
                    e.consume();
                }
            });
        //Allow picking/mouse interaction so handler actually works
        setPickOnBounds(false); // allows you to click to pass through.
        setMouseTransparent(false);
        getCanvas().setPickOnBounds(false);
        getCanvas().setMouseTransparent(false);
    }

    /**
     * Start the service that will display the waveform visualization of specified audioFile
     *
     * @param audioFile audio file
     */
    public void startVisualization(File audioFile) {
        service.start(audioFile);
    }

    /**
     * Recompute the waveform rendering without reprocessing the audio data
     *
     * @param now if true will attempt a redraw on the application thread.
     *            false will wrap the redraw in a Platform.runLater()
     */
    public void updateView(boolean now) {
        if (null != service && null != service.audioAmplitudes) {
            service.processAudioAmplitudes();
        }
        if (now) {
            paintWaveform();
        } else {
            Platform.runLater(() -> paintWaveform());
        }
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
        if (null != service) {
            service.resetMedia();
        }
    }

    public void pauseMedia() {
        if (null != service) {
            service.pauseMedia();
        }
    }

    public void playMedia() {
        if (null != service) {
            service.playMedia();
        }
    }

    public void fftOnMedia() {
        if (null != service) {
            service.fftOnMedia();
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
     * @return the coeffScale
     */
    public double getCoeffScale() {
        return coeffScale;
    }

    /**
     * @param coeffScale the coeffScale to set
     */
    public void setCoeffScale(double coeffScale) {
        this.coeffScale = coeffScale;
    }

    /**
     * This class is in charge of calculating the waveform data from a audio file
     */
    private class WaveformVisualizationService extends Service<Boolean> {
        private final Logger LOG = LoggerFactory.getLogger(WaveformVisualizationService.class);
        private MediaPlayer audioPlayer = null;
        private Media media = null;
        /**
         * Audio file
         */
        private File audioFile;
        /**
         * Amplitudes of audio file
         */
        private int[] audioAmplitudes;

        WaveformVisualizationService() {
            setOnSucceeded(event -> onSucceeded());
            setOnFailed(event -> onFailed());
        }

        /**
         * Create a task for the current service
         *
         * @return task
         */
        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {
                @Override
                protected Boolean call() {
                    try {
                        if (audioFile != null) {
                            LOG.info("Attempting to load {}", audioFile.toURI().toString());
                            Media m = new Media(audioFile.toURI().toString());
                            setMedia(m);
                            //@DEBUG SMP System.out.println("Setting up media player...");
                            MediaPlayer mp = new MediaPlayer(getMedia());
                            mp.setAutoPlay(false);
                            setAudioPlayer(mp);
                            //@DEBUG SMP System.out.println("Attempting to process Audio File...");
                            processAudioFile();
                            //@DEBUG SMP System.out.println("Painting Audio Waveform...");
                            Platform.runLater(() -> paintWaveform());
                            audioPlayer.currentTimeProperty().addListener(cl -> {
                                paintWaveform();
                                updateProgressByTime();
                            });
                            //@DEBUG SMP System.out.println("Waveform Visualization Service started.");
                            return true;
                        } else {
                            LOG.info("Audio File is Null!");
                            return false;
                        }
                    } catch (Exception ex) {
                        LOG.error("Exception", ex);
                        return false;
                    }
                }

                /**
                 * Calculate and initialize {@link WaveformVisualizationService#audioAmplitudes} and
                 * {@link WaveformVisualization#waveformData}
                 * @throws IOException if encountered IO error
                 * @throws UnsupportedAudioFileException if encountered invalid audio file
                 */
                private void processAudioFile() throws Exception, IOException, UnsupportedAudioFileException {
                    if (audioAmplitudes == null) {
                        //@DEBUG SMP System.out.println("calcAudioAmplitudes()...");
                        calcAudioAmplitudes();
                    }
                    //@DEBUG SMP System.out.println("processAudioAmplitudes()...");
                    processAudioAmplitudes();
                }

                /**
                 * Calculate and initialize {@link WaveformVisualizationService#audioAmplitudes}
                 * @throws IOException if encountered IO error
                 * @throws UnsupportedAudioFileException if encountered invalid audio file
                 */
                private void calcAudioAmplitudes() throws Exception, IOException, UnsupportedAudioFileException {
                    //@DEBUG SMP System.out.println("Acquiring Audio Input: " + audioFile.getAbsolutePath());
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
                    //@DEBUG SMP System.out.println("Acquiring Audio Format");
                    AudioFormat baseFormat = audioInputStream.getFormat();

                    // Encoding
                    //@DEBUG SMP System.out.println("Acquiring Audio Encoding...");
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
                    if (samplesPerPixel < 1)
                        samplesPerPixel = 1;
                    // Variables to calculate finalAmplitudes array
                    int currentSampleCounter = 0;
                    int arrayCellPosition = 0;
                    float currentCellValue = 0.0f;

                    // Variables for the calculation loop
                    int arrayCellValue;
                    //@DEBUG SMP System.out.println("Processing Audio Input...");
                    // Read all of the available data in chunks
                    while (decodedAudioInputStream.read(buffer, 0, BUFFER_SIZE) > 0) {
                        for (int i = 0; i < buffer.length - 1; i += 2) {
                            // Calculate the value
                            arrayCellValue = (int) (((((buffer[i + 1] << 8) | buffer[i] & 0xff) << 16) / AMPLITUDE_SCALE_MAX));

                            if (currentSampleCounter != samplesPerPixel) {
                                ++currentSampleCounter;
                                currentCellValue += Math.abs(arrayCellValue);
                            } else {
                                if (arrayCellPosition != maxArrayLength) {
                                    finalAmplitudes[arrayCellPosition] = finalAmplitudes[arrayCellPosition + 1] = (int) (currentCellValue / samplesPerPixel);
                                }
                                // update indices and counters
                                currentSampleCounter = 0;
                                currentCellValue = 0;
                                arrayCellPosition += 2;
                            }
                        }
                    }
                    audioAmplitudes = finalAmplitudes;
                }
            };
        }

        /**
         * Calculate and initialize {@link WaveformVisualization#waveformData}
         */
        public void processAudioAmplitudes() {
            // The width of the resulting waveform panel
            int width = (int) getWidth();
            waveformData = new float[width];
            int samplesPerPixel = audioAmplitudes.length / width;
            // Calculate
            float nValue;
            for (int w = 0; w < width; w++) {
                int c = w * samplesPerPixel;
                nValue = 0.0f;

                for (int s = 0; s < samplesPerPixel; s++) {
                    nValue += (Math.abs(audioAmplitudes[c + s]) / AMPLITUDE_SCALE_MAX)
                        * getCoeffScale();
                }
                waveformData[w] = nValue / samplesPerPixel;
            }
        }

        public void resetMedia() {
            if (null != audioPlayer) {
                audioPlayer.seek(Duration.ZERO);
            }
        }

        public void pauseMedia() {
            if (null != audioPlayer) {
                audioPlayer.pause();
            }
        }

        public void playMedia() {
            if (null != audioPlayer) {
                audioPlayer.play();
            }
        }

        public void fftOnMedia() {
            if (null != audioFile) {
                AudioLoader task = new AudioLoader(getScene(), audioFile);
                Thread thread = new Thread(task);
                thread.setDaemon(true);
                thread.start();
            }
        }

        public void updateProgressByTime() {
            if (null != audioPlayer) {
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
            LOG.error("Visualization service failed!");
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
