package edu.jhuapl.trinity;

import edu.jhuapl.trinity.data.Dimension;
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.coco.CocoObject;
import edu.jhuapl.trinity.data.files.ClusterCollectionFile;
import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.data.files.ManifoldDataFile;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.MatrixOverlay;
import edu.jhuapl.trinity.javafx.components.panes.AnalysisLogPane;
import edu.jhuapl.trinity.javafx.components.panes.CocoViewerPane;
import edu.jhuapl.trinity.javafx.components.panes.HyperdrivePane;
import edu.jhuapl.trinity.javafx.components.panes.ImageInspectorPane;
import edu.jhuapl.trinity.javafx.components.panes.JukeBoxPane;
import edu.jhuapl.trinity.javafx.components.panes.NavigatorPane;
import edu.jhuapl.trinity.javafx.components.panes.PixelSelectionPane;
import edu.jhuapl.trinity.javafx.components.panes.Shape3DControlPane;
import edu.jhuapl.trinity.javafx.components.panes.TextPane;
import edu.jhuapl.trinity.javafx.components.panes.TrajectoryTrackerPane;
import edu.jhuapl.trinity.javafx.components.panes.VideoPane;
import edu.jhuapl.trinity.javafx.components.panes.WaveformPane;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.components.timeline.Item;
import edu.jhuapl.trinity.javafx.components.timeline.MissionTimerX;
import edu.jhuapl.trinity.javafx.components.timeline.MissionTimerXBuilder;
import edu.jhuapl.trinity.javafx.components.timeline.TimelineAnimation;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.AudioEvent;
import edu.jhuapl.trinity.javafx.events.EffectEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.GaussianMixtureEvent;
import edu.jhuapl.trinity.javafx.events.HitEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.MissionTimerXEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import edu.jhuapl.trinity.javafx.events.SearchEvent;
import edu.jhuapl.trinity.javafx.events.SemanticMapEvent;
import edu.jhuapl.trinity.javafx.events.ShapleyEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import edu.jhuapl.trinity.javafx.events.ZeroMQEvent;
import edu.jhuapl.trinity.javafx.handlers.FeatureVectorEventHandler;
import edu.jhuapl.trinity.javafx.handlers.GaussianMixtureEventHandler;
import edu.jhuapl.trinity.javafx.handlers.HitEventHandler;
import edu.jhuapl.trinity.javafx.handlers.ManifoldEventHandler;
import edu.jhuapl.trinity.javafx.handlers.RestEventHandler;
import edu.jhuapl.trinity.javafx.handlers.SearchEventHandler;
import edu.jhuapl.trinity.javafx.handlers.SemanticMapEventHandler;
import edu.jhuapl.trinity.javafx.handlers.ShapleyEventHandler;
import edu.jhuapl.trinity.javafx.javafx3d.Hyperspace3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.Hypersurface3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.javafx.javafx3d.Projections3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.ProjectorPane;
import edu.jhuapl.trinity.javafx.javafx3d.RetroWavePane;
import edu.jhuapl.trinity.messages.CommandTask;
import edu.jhuapl.trinity.messages.MessageProcessor;
import edu.jhuapl.trinity.messages.ZeroMQFeedManager;
import edu.jhuapl.trinity.messages.ZeroMQSubscriberConfig;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.Configuration;
import edu.jhuapl.trinity.utils.PCAConfig;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.VisibilityMap;
import edu.jhuapl.trinity.utils.umap.Umap;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static edu.jhuapl.trinity.App.theConfig;


/**
 * @author Sean Phillips
 */
public class AppAsyncManager extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(AppAsyncManager.class);
    Scene scene;
    Pane desktopPane;
    StackPane centerStack;
    Map<String, String> namedParameters;
    CircleProgressIndicator progress;

    MessageProcessor processor;
    ZeroMQSubscriberConfig subscriberConfig;
    ZeroMQFeedManager feed;

    Hyperspace3DPane hyperspace3DPane;
    Hypersurface3DPane hypersurface3DPane;
    ProjectorPane projectorPane;
    Projections3DPane projections3DPane;
    TrajectoryTrackerPane trajectoryTrackerPane;
    TextPane textConsolePane;
    JukeBoxPane jukeBoxPane;
    VideoPane videoPane;
    NavigatorPane navigatorPane;
    CocoViewerPane cocoViewerPane;
    WaveformPane waveformPane;
    Shape3DControlPane shape3DControlPane;
    AnalysisLogPane analysisLogPane;
    PixelSelectionPane pixelSelectionPane;
    ImageInspectorPane imageInspectorPane;
    HyperdrivePane hyperdrivePane;
    MatrixOverlay matrixOverlay;
    RetroWavePane retroWavePane = null;

    boolean hyperspaceIntroShown = false;
    boolean hypersurfaceIntroShown = false;
    boolean enableHttp = false;

    ManifoldEventHandler meh;
    FeatureVectorEventHandler fveh;
    ShapleyEventHandler sheh;
    GaussianMixtureEventHandler gmeh;
    SemanticMapEventHandler smeh;
    SearchEventHandler seh;
    HitEventHandler heh;
    RestEventHandler reh;

    MissionTimerX missionTimerX;
    TimelineAnimation timelineAnimation;

    public AppAsyncManager(Scene scene, StackPane centerStack, Pane desktopPane, CircleProgressIndicator progress, Map<String, String> namedParameters) {
        this.scene = scene;
        this.namedParameters = namedParameters;
        this.progress = progress;
        this.desktopPane = desktopPane;
        this.centerStack = centerStack;
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
            ProgressStatus ps = new ProgressStatus("Loading Trinity GUI...", -1);
            ps.fillStartColor = Color.AZURE;
            ps.fillEndColor = Color.LIME;
            ps.innerStrokeColor = Color.AZURE;
            ps.outerStrokeColor = Color.LIME;

            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        //hardcoded count. need to update counts for every progress.setLabelLater
        double total = 32.0;
        double current = 0.0;
        Thread.sleep(100);
        progress.setPercentComplete(current++ / total);
        LOG.info("Attempting to read defaults...");
        try {
            LOG.info("Build Date: {}", Configuration.getBuildDate());
        } catch (Exception ex) {
            LOG.error(null, ex);
        }
        LOG.info("Parsing command line...");
        progress.setPercentComplete(current++ / total);
        progress.setLabelLater("...Parsing Command Line...");
        parseParameters();
        //ex: --scenario="C:\dev\cameratests" --geometry=1024x768+100+100
        boolean jukeBox = false;
        if (null != namedParameters) {
            LOG.info("Checking for geometry arguments...");
            if (namedParameters.containsKey("geometry")) {
                String geometryParamString = namedParameters.get("geometry");
                LOG.info("Attempting custom window geometry using {}", geometryParamString);
                try {
                    Platform.runLater(() -> {
                        //example: 200x100+800+800
                        String[] tokens = geometryParamString.split("\\+");
                        String[] sizeTokens = tokens[0].split("x");
                        ((Stage) scene.getWindow()).setWidth(Double.parseDouble(sizeTokens[0]));
                        ((Stage) scene.getWindow()).setHeight(Double.parseDouble(sizeTokens[1]));
                        ((Stage) scene.getWindow()).setX(Double.parseDouble(tokens[1]));
                        ((Stage) scene.getWindow()).setY(Double.parseDouble(tokens[2]));
                    });
                } catch (NumberFormatException ex) {
                    LOG.info("Exception thrown parsing: {}. Setting to Maximized.", geometryParamString);
                    Platform.runLater(() -> ((Stage) scene.getWindow()).setMaximized(true));
                }
            } else if (namedParameters.containsKey("fullscreen")) {
                LOG.info("Fullscreen start requested.");
                Platform.runLater(() -> ((Stage) scene.getWindow()).setFullScreen(true));
            } else {
                LOG.info("Defaulting to maximized.");
                Platform.runLater(() -> ((Stage) scene.getWindow()).setMaximized(true));
            }
            LOG.info("Checking for special effects requests...");
            jukeBox = namedParameters.containsKey("jukebox");
            if (jukeBox) {
                LOG.info("jukebox found... enabling music.");
                Platform.runLater(() -> {
                    scene.getRoot().fireEvent(new AudioEvent(AudioEvent.ENABLE_MUSIC_TRACKS, true));
                });
            }
            boolean surveillanceEnabled = namedParameters.containsKey("surveillance");
            if (surveillanceEnabled) {
                LOG.info("Surveillance found... enabling Camera.");
            }
            if (namedParameters.containsKey("outrun")) {
                LOG.info("Outrun found... enabling RetroWavePane.");
                try {
                    //Add optional RETROWAVE VIEW
                    retroWavePane = new RetroWavePane(scene, surveillanceEnabled);
                    Platform.runLater(() -> {
                        retroWavePane.setVisible(true);
                        centerStack.getChildren().add(retroWavePane);
                    });
                } catch (Exception ex) {
                    LOG.error(null, ex);
                }
            }
            LOG.info("Checking for custom configuration...");
            if (namedParameters.containsKey("config")) {
                String configFile = namedParameters.get("config");
                LOG.info("Configuration file found: {}", configFile);
                try {
                    theConfig = new Configuration(configFile);
                } catch (IOException ex) {
                    LOG.info("Exception thrown loading: {}", configFile);
                    LOG.info("Loading defaults.");
                    theConfig = Configuration.defaultConfiguration();
                }
            }
            LOG.info("Checking for enabling HTTP processor...");
            if (namedParameters.containsKey("http")) {
                String httpValue = namedParameters.get("http");
                LOG.info("HTTP found: {}", httpValue);
                enableHttp = Boolean.parseBoolean(httpValue);
            }
        }
        LOG.info("Setting up Matrix Digital Rain...");
        // fun matrix effect, use Alt + N
        progress.setLabelLater("...Digital Rain...");
        matrixOverlay = new MatrixOverlay(scene, centerStack);
        scene.addEventHandler(EffectEvent.START_DIGITAL_RAIN, e -> {
            MatrixOverlay.on.set(true);
            matrixOverlay.matrixOn.run();
        });
        scene.addEventHandler(EffectEvent.STOP_DIGITAL_RAIN, e -> {
            MatrixOverlay.on.set(false);
            matrixOverlay.matrixOff.run();
        });
        centerStack.setOnSwipeUp(e -> {
            if (e.getTouchCount() > 2) { //three finger swipe
                MatrixOverlay.on.set(false);
                matrixOverlay.matrixOff.run();
                e.consume(); // <-- stops passing the event to next node
            }
        });
        centerStack.setOnSwipeDown(e -> {
            if (e.getTouchCount() > 2) { //three finger swipe
                MatrixOverlay.on.set(true);
                matrixOverlay.matrixOn.run();
                e.consume(); // <-- stops passing the event to next node
            }
        });

        progress.setTopLabelLater("Constructing 3D Subscenes");
        LOG.info("Constructing 3D subscenes...");
        progress.setLabelLater("...Projections3D...");
        progress.setPercentComplete(current++ / total);
        projections3DPane = new Projections3DPane(scene);
        projections3DPane.setVisible(false); //start off hidden
        Platform.runLater(() -> centerStack.getChildren().add(0, projections3DPane));

        progress.setLabelLater("...Hypersurface...");
        progress.setPercentComplete(current++ / total);
        hypersurface3DPane = new Hypersurface3DPane(scene);
        hypersurface3DPane.setVisible(false); //start off hidden
        Platform.runLater(() -> centerStack.getChildren().add(0, hypersurface3DPane));

        progress.setLabelLater("...Hyperspace...");
        progress.setPercentComplete(current++ / total);
        hyperspace3DPane = new Hyperspace3DPane(scene);
        hyperspace3DPane.setVisible(false); //start off hidden
        Platform.runLater(() -> centerStack.getChildren().add(0, hyperspace3DPane));

        progress.setLabelLater("...Analysis Projector...");
        progress.setPercentComplete(current++ / total);
        projectorPane = new ProjectorPane();
        projectorPane.setVisible(false); //start off hidden
        Platform.runLater(() -> centerStack.getChildren().add(0, projectorPane));

        progress.setTopLabelLater("Loading 2D Tools...");
        LOG.info("Loading 2D Tools...");
        progress.setLabelLater("...Content Navigator...");
        progress.setPercentComplete(current++ / total);
        navigatorPane = new NavigatorPane(scene, desktopPane);
        progress.setLabelLater("...Analysis Log...");
        progress.setPercentComplete(current++ / total);
        analysisLogPane = new AnalysisLogPane(scene, desktopPane);
        progress.setLabelLater("...Pixel Selector...");
        progress.setPercentComplete(current++ / total);
        pixelSelectionPane = new PixelSelectionPane(scene, desktopPane);
        progress.setLabelLater("...Image Inspector...");
        progress.setPercentComplete(current++ / total);
        imageInspectorPane = new ImageInspectorPane(scene, desktopPane);
        progress.setLabelLater("...Hyperdrive...");
        progress.setPercentComplete(current++ / total);
        hyperdrivePane = new HyperdrivePane(scene, desktopPane);
        progress.setLabelLater("...Empty Vision...");
        progress.setPercentComplete(current++ / total);
        videoPane = new VideoPane(scene, desktopPane);
        App.theVideo = videoPane;

        progress.setTopLabelLater("Initializing Event Handlers");
        LOG.info("Text Console ");
        progress.setLabelLater("...Application Events...");
        progress.setPercentComplete(current++ / total);
        scene.addEventHandler(ApplicationEvent.SHOW_TEXT_CONSOLE, e -> {
            if (null == textConsolePane) {
                textConsolePane = new TextPane(scene, desktopPane);
                textConsolePane.setOpaqueEnabled(true);
            }
            if (!desktopPane.getChildren().contains(textConsolePane)) {
                desktopPane.getChildren().add(textConsolePane);
                textConsolePane.slideInPane();
            } else {
                textConsolePane.show();
            }
            if (null != e.object) {
                Platform.runLater(() -> textConsolePane.setText((String) e.object));
            }
        });
        LOG.info("Video Pane ");
        scene.addEventHandler(ApplicationEvent.SHOW_VIDEO_PANE, e -> {
            if (null == videoPane) {
                videoPane = new VideoPane(scene, desktopPane);
                App.theVideo = videoPane;
            }
            if (!desktopPane.getChildren().contains(videoPane)) {
                desktopPane.getChildren().add(videoPane);
                videoPane.setTranslateX(desktopPane.getWidth() / 2.0
                    - videoPane.getBoundsInLocal().getWidth() / 2.0);
                videoPane.setTranslateY(desktopPane.getHeight() / 2.0
                    - videoPane.getBoundsInLocal().getHeight() / 2.0);
                videoPane.slideInPane();
            } else {
                videoPane.show();
            }
            if (null != e.object) {
                String title = (String) e.object;
                videoPane.mainTitleTextProperty.set(title);
            }
            if (null != e.object2) {
                String caption = (String) e.object2;
                videoPane.mainTitleText2Property.set(caption);
            }
            videoPane.setVideo();
        });
        LOG.info("Content Navigator ");
        scene.addEventHandler(ApplicationEvent.SHOW_NAVIGATOR_PANE, e -> {
            if (null == navigatorPane) {
                navigatorPane = new NavigatorPane(scene, desktopPane);
            }
            if (!desktopPane.getChildren().contains(navigatorPane)) {
                desktopPane.getChildren().add(navigatorPane);
                navigatorPane.slideInPane();
            } else {
                navigatorPane.show();
            }
            if (null != e.object) {
                Platform.runLater(() -> navigatorPane.setImage((Image) e.object));
            }
        });
        LOG.info("COCO Viewer");
        scene.addEventHandler(ApplicationEvent.SHOW_COCOVIEWER_PANE, e -> {
            if (null == cocoViewerPane) {
                cocoViewerPane = new CocoViewerPane(scene, desktopPane);
            }
            if (!desktopPane.getChildren().contains(cocoViewerPane)) {
                desktopPane.getChildren().add(cocoViewerPane);
                cocoViewerPane.slideInPane();
            } else {
                cocoViewerPane.show();
            }
            if (null != e.object) {
                Platform.runLater(() -> {
                    switch (e.object) {
                        case File file -> cocoViewerPane.loadCocoFile(file);
                        case CocoObject cocoObject -> cocoViewerPane.loadCocoObject(cocoObject);
                        default -> {
                        }
                    }
                });
            }
        });
        LOG.info("Analysis Log View");
        scene.addEventHandler(ApplicationEvent.SHOW_ANALYSISLOG_PANE, e -> {
            if (null == analysisLogPane) {
                analysisLogPane = new AnalysisLogPane(scene, desktopPane);
            }
            if (!desktopPane.getChildren().contains(analysisLogPane)) {
                desktopPane.getChildren().add(analysisLogPane);
                analysisLogPane.slideInPane();
            } else {
                analysisLogPane.show();
            }
        });
        LOG.info("Pixel Selection Pane");
        scene.addEventHandler(ApplicationEvent.SHOW_PIXEL_SELECTION, e -> {
            if (null == pixelSelectionPane) {
                pixelSelectionPane = new PixelSelectionPane(scene, desktopPane);
            }
            if (!desktopPane.getChildren().contains(pixelSelectionPane)) {
                desktopPane.getChildren().add(pixelSelectionPane);
                pixelSelectionPane.slideInPane();
            } else {
                pixelSelectionPane.show();
            }
            if (null != e.object) {
                Image image = (Image) e.object;
                pixelSelectionPane.setImage(image);
            }
        });
        LOG.info("Image Inspection Pane");
        scene.addEventHandler(ApplicationEvent.SHOW_IMAGE_INSPECTION, e -> {
            if (null != e.object) {
                Boolean show = (Boolean) e.object;
                if (show) {
                    if (null == imageInspectorPane) {
                        imageInspectorPane = new ImageInspectorPane(scene, desktopPane);
                    }
                    if (!desktopPane.getChildren().contains(imageInspectorPane)) {
                        desktopPane.getChildren().add(imageInspectorPane);
                        imageInspectorPane.slideInPane();
                    } else {
                        imageInspectorPane.show();
                    }
                }
            }
        });
        LOG.info("Waveform View ");
        scene.addEventHandler(ApplicationEvent.SHOW_WAVEFORM_PANE, e -> {
            if (null == waveformPane) {
                waveformPane = new WaveformPane(scene, desktopPane);
            }
            if (!desktopPane.getChildren().contains(waveformPane)) {
                desktopPane.getChildren().add(waveformPane);
                waveformPane.slideInPane();
            } else {
                waveformPane.show();
            }
            if (null != e.object) {
                Platform.runLater(() -> waveformPane.setWaveform((File) e.object));
            }
        });
        LOG.info("Trajectory Tracker ");
        scene.addEventHandler(TrajectoryEvent.SHOW_TRAJECTORY_TRACKER, e -> {
            if (null == trajectoryTrackerPane) {
                trajectoryTrackerPane = new TrajectoryTrackerPane(scene, desktopPane);
            }
            if (!desktopPane.getChildren().contains(trajectoryTrackerPane)) {
                desktopPane.getChildren().add(trajectoryTrackerPane);
                trajectoryTrackerPane.slideInPane();
            } else {
                trajectoryTrackerPane.show();
            }
        });
        LOG.info("Projections ");
        scene.addEventHandler(ApplicationEvent.SHOW_PROJECTIONS, e -> {
            if (projections3DPane.isVisible()) {
                projections3DPane.setVisible(false);
                if (null != retroWavePane) {
                    retroWavePane.animateShow();
                }
            } else {
                if (null != retroWavePane) {
                    retroWavePane.animateHide();
                }
                hyperspace3DPane.setVisible(false);
                hypersurface3DPane.setVisible(false);
                Platform.runLater(() -> {
                    projections3DPane.setVisible(true);
                });
            }
        });

        scene.addEventHandler(ApplicationEvent.AUTO_PROJECTION_MODE, e -> {
            boolean enabled = (boolean) e.object;
            if (enabled) {
                if (null != retroWavePane) {
                    retroWavePane.animateHide();
                }
                hyperspace3DPane.setVisible(false);
                hypersurface3DPane.setVisible(false);
                projections3DPane.setVisible(true);
            }
            projections3DPane.enableAutoProjection(enabled);
            hyperspace3DPane.enableAutoProjection(enabled);
        });
        LOG.info("Hypersurface ");
        scene.addEventHandler(ApplicationEvent.SHOW_HYPERSURFACE, e -> {
            if (hypersurface3DPane.isVisible()) {
                Platform.runLater(() -> hypersurface3DPane.setVisible(false));
                if (null != retroWavePane) {
                    retroWavePane.animateShow();
                }
            } else {
                if (null != retroWavePane) {
                    retroWavePane.animateHide();
                }
                hyperspace3DPane.setVisible(false);
                if (null != projections3DPane) {
                    projections3DPane.setVisible(false);
                }
                //@TODO SMP fadeOutConsole(500);
                //scene.addEventHandler(CommandTerminalEvent.FADE_OUT, e -> fadeOutConsole(e.timeMS));

                if (!hypersurfaceIntroShown) {
                    hypersurface3DPane.showFA3D();
                    hypersurfaceIntroShown = true;
                } else {
                    Platform.runLater(() -> hypersurface3DPane.setVisible(true));
                }
            }
        });
        LOG.info("Hyperspace ");
        scene.addEventHandler(ApplicationEvent.SHOW_HYPERSPACE, e -> {
            if (hyperspace3DPane.isVisible()) {
                hyperspace3DPane.setVisible(false);
                if (null != retroWavePane) {
                    retroWavePane.animateShow();
                }
                //@TODO SMP fadeOutConsole(500);
            } else {
                hypersurface3DPane.setVisible(false);
                if (null != projections3DPane) {
                    projections3DPane.setVisible(false);
                }
                if (null != retroWavePane) {
                    retroWavePane.animateHide();
                }
                hyperspace3DPane.setVisible(true);
                if (!hyperspaceIntroShown) {
                    hyperspace3DPane.intro(1000);
                    hyperspaceIntroShown = true;
                }
                //@TODO SMP fadeInConsole(500);
            }
        });
        scene.addEventHandler(ApplicationEvent.SHOW_SHAPE3D_CONTROLS, e -> {
            Manifold3D manifold3D = (Manifold3D) e.object;
            if (null == shape3DControlPane) {
                shape3DControlPane = new Shape3DControlPane(scene, desktopPane);
            }
            if (null != manifold3D) {
                shape3DControlPane.setShape3D(manifold3D);
            }
            if (!desktopPane.getChildren().contains(shape3DControlPane)) {
                desktopPane.getChildren().add(shape3DControlPane);
                shape3DControlPane.slideInPane();
            } else {
                shape3DControlPane.show();
            }
        });
        scene.addEventHandler(ApplicationEvent.SHOW_PROJECTOR_PANE, e -> {
            boolean visible = !projectorPane.isVisible();
            if (null != e.object)
                visible = (boolean) e.object;
            projectorPane.setVisible(visible);
            if (visible && projectorPane.firstTime == true) {
                projectorPane.firstTime = false;
                projectorPane.scanAndAnimate();
            }
        });
        scene.addEventHandler(ApplicationEvent.SHOW_HYPERDRIVE_PANE, e -> {
            if (!desktopPane.getChildren().contains(hyperdrivePane)) {
                desktopPane.getChildren().add(hyperdrivePane);
                hyperdrivePane.slideInPane();
            } else {
                hyperdrivePane.show();
            }
        });
        jukeBoxPane = new JukeBoxPane(scene, desktopPane);
        jukeBoxPane.setEnableMusic(jukeBox);
        scene.addEventHandler(ApplicationEvent.SHOW_JUKEBOX_PANE, e -> {
            if (null == jukeBoxPane) {
                jukeBoxPane = new JukeBoxPane(scene, desktopPane);
            }
            if (!desktopPane.getChildren().contains(jukeBoxPane)) {
                desktopPane.getChildren().add(jukeBoxPane);
                jukeBoxPane.slideInPane();
            } else {
                jukeBoxPane.show();
            }
        });

        progress.setTopLabelLater("Establishing Messaging Feeds...");
        progress.setLabelLater("...REST Receiver...");
        progress.setPercentComplete(current++ / total);
        LOG.info("Setting up RestEventHandler...");
        reh = new RestEventHandler(scene);
        scene.addEventHandler(RestEvent.START_RESTSERVER_THREAD, reh);
        scene.addEventHandler(RestEvent.TERMINATE_RESTSERVER_THREAD, reh);
        if (enableHttp) //commandline parsed previously
            reh.startHttpService();

        progress.setLabelLater("...Setting up Mission Timer...");
        progress.setPercentComplete(current++ / total);
        LOG.info("Setting up Mission Timer...");
        setupMissionTimer(scene);
        scene.addEventHandler(MissionTimerXEvent.MISSION_TIMER_SHUTDOWN, e -> {
            if (null != missionTimerX) {
                missionTimerX.stop();
            }
        });

        Platform.runLater(() -> {
            int i = centerStack.getChildren().indexOf(progress);
            if (i < 0 && !centerStack.getChildren().isEmpty())
                i = centerStack.getChildren().size() - 1;
            centerStack.getChildren().add(i, missionTimerX);
        });
        missionTimerX.updateTime(0, "1");

        progress.setLabelLater("...ZeroMQ...");
        progress.setPercentComplete(current++ / total);
        LOG.info("Establishing Messaging Feed...");
        processor = new MessageProcessor(scene);
        subscriberConfig = new ZeroMQSubscriberConfig(
            "ZeroMQ Subscriber", "ZeroMQFeedManager.",
            "tcp://localhost:5563", "ALL", "SomeIDValue", 250);
        feed = new ZeroMQFeedManager(2, subscriberConfig, processor);
        scene.getRoot().addEventHandler(ZeroMQEvent.ZEROMQ_ESTABLISH_CONNECTION, e -> {
            subscriberConfig = e.subscriberConfig;
            if (null != subscriberConfig) {
                Task task = new Task() {
                    @Override
                    protected Void call() throws Exception {
                        feed.setConfig(subscriberConfig);
                        feed.startProcessing();
                        return null;
                    }
                };
                Thread thread = new Thread(task);
                thread.setDaemon(true);
                thread.start();
            }
        });
        scene.getRoot().addEventHandler(ZeroMQEvent.ZEROMQ_TERMINATE_CONNECTION, e -> {
            if (null != feed) {
                feed.disconnect(false);
            }
        });
        scene.getRoot().addEventHandler(ZeroMQEvent.ZEROMQ_START_PROCESSING, e -> {
            feed.setEnableProcessing(true);
        });
        scene.getRoot().addEventHandler(ZeroMQEvent.ZEROMQ_STOP_PROCESSING, e -> {
            feed.setEnableProcessing(false);
        });
        LOG.info("Checking to auto-enable ZeroMQ Listener...");
        if (null != namedParameters && namedParameters.containsKey("zeromq")) {
            String zeroMQ = namedParameters.get("zeromq");
            if (null != zeroMQ) {
                LOG.info("ZeroMQ Status: {}", zeroMQ);
            }
            Platform.runLater(() -> scene.getRoot().fireEvent(
                new ZeroMQEvent(ZeroMQEvent.ZEROMQ_ESTABLISH_CONNECTION, subscriberConfig)));
        }

        progress.setTopLabelLater("Registering Request Handlers...");
        LOG.info("Request Handlers...");
        progress.setLabelLater("...REQUEST_FEATURE_COLLECTION...");
        progress.setPercentComplete(current++ / total);
        scene.addEventHandler(FeatureVectorEvent.REQUEST_FEATURE_COLLECTION, event -> {
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(hyperspace3DPane.getAllFeatureVectors());
            hypersurface3DPane.addFeatureCollection(fc, false);
        });

        progress.setLabelLater("...GENERATE_NEW_UMAP...");
        progress.setPercentComplete(current++ / total);
        scene.addEventHandler(ManifoldEvent.GENERATE_NEW_UMAP, event -> {
            Platform.runLater(() -> {
                ProgressStatus ps = new ProgressStatus("Generating UMAP Settings...", -1);
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
            });
            Umap umap = AnalysisUtils.getDefaultUmap();
            if (null != event.object1)
                umap = (Umap) event.object1;
            else if (null != projections3DPane.latestUmap)
                umap = projections3DPane.latestUmap;
            umap.setThreads(24);
            ManifoldEvent.POINT_SOURCE source = ManifoldEvent.POINT_SOURCE.HYPERSPACE;
            if (null != event.object2)
                source = (ManifoldEvent.POINT_SOURCE) event.object2;
            FeatureCollection originalFC = getFeaturesBySource(source);
            projections3DPane.projectFeatureCollection(originalFC, umap);
        });

        progress.setLabelLater("...GENERATE_NEW_PCA...");
        progress.setPercentComplete(current++ / total);
        scene.addEventHandler(ManifoldEvent.GENERATE_NEW_PCA, event -> {
            Platform.runLater(() -> {
                ProgressStatus ps = new ProgressStatus("Projecting PCA...", -1);
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
            });
            PCAConfig config = (PCAConfig) event.object1;
            ManifoldEvent.POINT_SOURCE source = (ManifoldEvent.POINT_SOURCE) event.object2;
            FeatureCollection originalFC = getFeaturesBySource(source);
            projections3DPane.projectFeatureCollection(originalFC, config);
        });

        progress.setLabelLater("...PROJECT_SURFACE_GRID...");
        progress.setPercentComplete(current++ / total);
        scene.addEventHandler(FeatureVectorEvent.PROJECT_SURFACE_GRID, event -> {
            FeatureCollection originalFC = new FeatureCollection();
            originalFC.setFeatures(hypersurface3DPane.getAllFeatureVectors());
            double[][] data = AnalysisUtils.fitUMAP(originalFC);
            FeatureCollection projectedFC = FeatureCollection.fromData(data);
            for (int i = 0; i < originalFC.getFeatures().size(); i++) {
                FeatureVector origFV = originalFC.getFeatures().get(i);
                projectedFC.getFeatures().get(i).setLabel(origFV.getLabel());
                projectedFC.getFeatures().get(i).setScore(origFV.getScore());
                projectedFC.getFeatures().get(i).setImageURL(origFV.getImageURL());
            }
            if (null != originalFC.getDimensionLabels()) {
                projectedFC.setDimensionLabels(originalFC.getDimensionLabels());
                projections3DPane.setDimensionLabels(originalFC.getDimensionLabels());
            }
            projections3DPane.setHyperDimensionFeatures(originalFC);
            projections3DPane.addFeatureCollection(projectedFC, false);
        });

        progress.setLabelLater("...PROJECT_FEATURE_COLLECTION...");
        progress.setPercentComplete(current++ / total);
        scene.addEventHandler(FeatureVectorEvent.PROJECT_FEATURE_COLLECTION, event -> {
            FeatureCollection originalFC = new FeatureCollection();
            originalFC.setFeatures(hyperspace3DPane.getAllFeatureVectors());
            double[][] data = AnalysisUtils.fitUMAP(originalFC);
            FeatureCollection projectedFC = FeatureCollection.fromData(data);
            for (int i = 0; i < originalFC.getFeatures().size(); i++) {
                FeatureVector origFV = originalFC.getFeatures().get(i);
                projectedFC.getFeatures().get(i).setLabel(origFV.getLabel());
                projectedFC.getFeatures().get(i).setScore(origFV.getScore());
                projectedFC.getFeatures().get(i).setImageURL(origFV.getImageURL());
            }
            if (null != originalFC.getDimensionLabels()) {
                projectedFC.setDimensionLabels(originalFC.getDimensionLabels());
                projections3DPane.setDimensionLabels(originalFC.getDimensionLabels());
            }
            projections3DPane.setHyperDimensionFeatures(originalFC);
            projections3DPane.addFeatureCollection(projectedFC, false);
        });

        progress.setLabelLater("...EXPORT_FEATURE_COLLECTION...");
        progress.setPercentComplete(current++ / total);
        scene.addEventHandler(FeatureVectorEvent.EXPORT_FEATURE_COLLECTION, event -> {
            File file = (File) event.object;
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(hyperspace3DPane.getAllFeatureVectors());
            try {
                FeatureCollectionFile fcf = new FeatureCollectionFile(file.getAbsolutePath(), false);
                fcf.featureCollection = fc;
                fcf.writeContent();
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
            event.consume();
        });

        progress.setTopLabelLater("Connecting Dedicated Event Handlers...");
        LOG.info("Data Handlers ");
        progress.setLabelLater("...GaussianMixtureEventHandler...");
        progress.setPercentComplete(current++ / total);
        gmeh = new GaussianMixtureEventHandler();
        scene.getRoot().addEventHandler(GaussianMixtureEvent.NEW_GAUSSIAN_MIXTURE, gmeh);
        scene.getRoot().addEventHandler(GaussianMixtureEvent.LOCATE_GAUSSIAN_MIXTURE, gmeh);
        scene.getRoot().addEventHandler(GaussianMixtureEvent.NEW_GAUSSIAN_COLLECTION, gmeh);
        gmeh.addGaussianMixtureRenderer(hyperspace3DPane);

        progress.setLabelLater("...FeatureVectorEventHandler...");
        progress.setPercentComplete(current++ / total);
        fveh = new FeatureVectorEventHandler();
        scene.getRoot().addEventHandler(FeatureVectorEvent.NEW_FEATURE_VECTOR, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.LOCATE_FEATURE_VECTOR, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.RESCAN_FACTOR_LABELS, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.RESCAN_FEATURE_LAYERS, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.NEW_LABEL_CONFIG, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.CLEAR_ALL_FEATUREVECTORS, fveh);
        fveh.addFeatureVectorRenderer(hyperspace3DPane);

        progress.setLabelLater("...ManifoldEventHandler...");
        progress.setPercentComplete(current++ / total);
        meh = new ManifoldEventHandler();
        scene.getRoot().addEventHandler(ManifoldEvent.NEW_UMAP_CONFIG, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.NEW_MANIFOLD_CLUSTER, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.NEW_MANIFOLD_DATA, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.EXPORT_MANIFOLD_DATA, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.CLEAR_ALL_MANIFOLDS, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.GENERATE_PROJECTION_MANIFOLD, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_SET_SCALE, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_ROTATE_X, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_ROTATE_Y, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_ROTATE_Z, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_FILL_DRAWMODE, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_LINE_DRAWMODE, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_FRONT_CULLFACE, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_BACK_CULLFACE, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_NONE_CULLFACE, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_SHOW_CONTROL, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_SHOW_WIREFRAME, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_SHOW_WIREFRAME, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_DIFFUSE_COLOR, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_SPECULAR_COLOR, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.MANIFOLD_WIREFRAME_COLOR, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.FIND_PROJECTION_CLUSTERS, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.NEW_CLUSTER_COLLECTION, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.NEW_PROJECTION_VECTOR, meh);
        meh.addManifoldRenderer(projections3DPane);
        scene.getRoot().addEventHandler(ManifoldEvent.FIND_HYPERSURFACE_CLUSTERS, e -> {
            hypersurface3DPane.findClusters((ManifoldEvent.ProjectionConfig) e.object1);
        });

        progress.setLabelLater("...ShapleyEventHandler...");
        progress.setPercentComplete(current++ / total);
        sheh = new ShapleyEventHandler();
        scene.getRoot().addEventHandler(ShapleyEvent.NEW_SHAPLEY_VECTOR, sheh);
        scene.getRoot().addEventHandler(ShapleyEvent.NEW_SHAPLEY_COLLECTION, sheh);
        sheh.addShapleyVectorRenderer(hypersurface3DPane);

        heh = new HitEventHandler(desktopPane);
        scene.getRoot().addEventHandler(HitEvent.PROJECTILE_HIT_SHAPE, heh);
        scene.getRoot().addEventHandler(HitEvent.TRACKING_PROJECTILE_EVENTS, heh);

        progress.setLabelLater("...SemanticMapEventHandler...");
        progress.setPercentComplete(current++ / total);
        smeh = new SemanticMapEventHandler(false);
        scene.getRoot().addEventHandler(SemanticMapEvent.NEW_SEMANTIC_MAP, smeh);
        scene.getRoot().addEventHandler(SemanticMapEvent.NEW_SEMANTICMAP_COLLECTION, smeh);
        smeh.addFeatureVectorRenderer(hyperspace3DPane);
        smeh.addSemanticMapRenderer(hypersurface3DPane);

        progress.setLabelLater("...SearchEventHandler...");
        progress.setPercentComplete(current++ / total);
        seh = new SearchEventHandler();
        scene.getRoot().addEventHandler(SearchEvent.FILTER_BY_TERM, seh);
        scene.getRoot().addEventHandler(SearchEvent.FILTER_BY_SCORE, seh);
        scene.getRoot().addEventHandler(SearchEvent.CLEAR_ALL_FILTERS, seh);
        scene.getRoot().addEventHandler(SearchEvent.FIND_BY_QUERY, seh);
        scene.getRoot().addEventHandler(SearchEvent.QUERY_EMBEDDINGS_RESPONSE, seh);
        seh.addFeatureVectorRenderer(hyperspace3DPane);

        progress.setTopLabelLater("Adding Drag & Drop Support...");
        LOG.info("Adding Drag & Drop Support...");
        progress.setLabelLater("...Center Stack...");
        progress.setPercentComplete(current++ / total);
        //some helpful handling for the main view
        centerStack.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        centerStack.addEventHandler(DragEvent.DRAG_DROPPED, e -> {
            String dropType = ResourceUtils.detectDropType(e);
            if (!dropType.contentEquals("UNKNOWN")) {
                if (ResourceUtils.promptUserOnCommand(dropType)) {
                    try {
                        switch (dropType) {
                            case "Hyperspace" -> CommandTask.execute(scene, "VIEW_HYPERSPACE", 0, null);
                            case "Hypersurface" -> CommandTask.execute(scene, "VIEW_HYPERSURFACE", 0, null);
                            case "Projections" -> CommandTask.execute(scene, "VIEW_PROJECTIONS", 0, null);
                        }
                    } catch (InterruptedException ex) {
                        LOG.error(null, ex);
                    }
                }
            }
            ResourceUtils.onDragDropped(e, scene);
        });

        progress.setLabelLater("...3D subscenes...");
        progress.setPercentComplete(current++ / total);
        hyperspace3DPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        hyperspace3DPane.addEventHandler(DragEvent.DRAG_DROPPED,
            e -> ResourceUtils.onDragDropped(e, scene));
        hypersurface3DPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        hypersurface3DPane.addEventHandler(DragEvent.DRAG_DROPPED,
            e -> ResourceUtils.onDragDropped(e, scene));

        projections3DPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) { // && projections3DPane.autoProjectionProperty.get()) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        projections3DPane.addEventHandler(DragEvent.DRAG_DROPPED, e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    try {
                        if (projections3DPane.autoProjectionProperty.get()) {
                            if (FeatureCollectionFile.isFeatureCollectionFile(file)) {
                                FeatureCollectionFile fcFile = new FeatureCollectionFile(
                                    file.getAbsolutePath(), true);
                                fveh.scanLabelsAndLayers(fcFile.featureCollection.getFeatures());
                                projections3DPane.transformFeatureCollection(fcFile.featureCollection);
                                e.consume();
                            }
                        }
                        if (ClusterCollectionFile.isClusterCollectionFile(file)) {
                            ClusterCollectionFile ccFile = new ClusterCollectionFile(file.getAbsolutePath(), true);
                            scene.getRoot().fireEvent(
                                new ManifoldEvent(ManifoldEvent.NEW_CLUSTER_COLLECTION, ccFile.clusterCollection));
                            e.consume();
                        }
                        if (ManifoldDataFile.isManifoldDataFile(file)) {
                            ManifoldDataFile mdFile = new ManifoldDataFile(file.getAbsolutePath(), true);
                            Platform.runLater(() -> scene.getRoot().fireEvent(
                                new ManifoldEvent(ManifoldEvent.NEW_MANIFOLD_DATA, mdFile.manifoldData)));
                            e.consume();
                        }
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    }
                }
            }
        });

        progress.setTopLabelLater("Finished async load.");
        progress.setLabelLater("User Interface Is Lit");
        progress.setPercentComplete(1.0);

        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
        });
        System.out.println("Finished async load.");
        return null;
    }

    private void parseParameters() {
        if (!namedParameters.isEmpty()) {
            LOG.info("NamedParameters :");
            namedParameters.entrySet().forEach(entry -> {
                LOG.info("\t{} : {}", entry.getKey(), entry.getValue());
            });
        }
    }

    private FeatureCollection getFeaturesBySource(ManifoldEvent.POINT_SOURCE source) {
        FeatureCollection originalFC = new FeatureCollection();
        if (source == ManifoldEvent.POINT_SOURCE.HYPERSURFACE) {
            originalFC = FeatureCollection.fromData(hypersurface3DPane.dataGrid);
            for (int i = 0; i < originalFC.getFeatures().size() - 1; i++) {
                FeatureVector actualFV = hypersurface3DPane.getAllFeatureVectors().get(i);
                originalFC.getFeatures().get(i).setLabel(actualFV.getLabel());
                originalFC.getFeatures().get(i).setScore(actualFV.getScore());
                originalFC.getFeatures().get(i).setImageURL(actualFV.getImageURL());
            }
        } else {
            int size = hyperspace3DPane.getAllFeatureVectors().size();
            List<FeatureVector> visibleFeatures = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                if (FactorLabel.visibilityByLabel(
                    hyperspace3DPane.getAllFeatureVectors().get(i).getLabel())
                    && VisibilityMap.visibilityByIndex(i)) {
                    visibleFeatures.add(hyperspace3DPane.getAllFeatureVectors().get(i));
                }
            }
            originalFC.setFeatures(visibleFeatures);
        }
        ArrayList<String> labels = new ArrayList<>();
        for (Dimension d : Dimension.getDimensions()) {
            labels.add(d.labelString);
        }
        originalFC.setDimensionLabels(labels);
        return originalFC;
    }

    private void setupMissionTimer(Scene scene) {
        double width = 900;
        double height = 200;
        double iconFitWidth = 30;
        if (namedParameters.containsKey("display4k")) {
            width = 1800;
            height = 400;
            iconFitWidth = 60;
        }
        missionTimerX = MissionTimerXBuilder.create()
            .title("Trinity Mission Time")
            .startTime(0)
            .timeFrame(java.time.Duration.ofMinutes(14).getSeconds())
            .maxSize(width, height)
            .prefSize(width, height)
            .iconFitWidth(iconFitWidth)
            .padding(Insets.EMPTY)
            .ringColor(Color.STEELBLUE.deriveColor(1, 1, 1, 0.333))
            .backgroundColor(Color.BLACK.deriveColor(1, 1, 1, 0.333))
            .itemInnerColor(Color.CYAN.deriveColor(1, 1, 1, 0.75))
            .clockColor(Color.ALICEBLUE)
            .ringBackgroundColor(Color.BLACK.deriveColor(1, 1, 1, 0.333))
            .titleColor(Color.ALICEBLUE)
            .build();
        StackPane.setAlignment(missionTimerX, Pos.BOTTOM_CENTER);
        missionTimerX.setPickOnBounds(false);
        missionTimerX.updateTime(0, "1");
        scene.addEventHandler(TimelineEvent.TIMELINE_SET_VISIBLE, e -> missionTimerX.setVisible((boolean) e.object));
        scene.addEventHandler(TimelineEvent.TIMELINE_CLEAR_ITEMS, e -> missionTimerX.clearItems());
        scene.addEventHandler(TimelineEvent.TIMELINE_ADD_ITEMS, e -> missionTimerX.addItems((List<Item>) e.object));
        scene.addEventHandler(TimelineEvent.TIMELINE_SET_INITIALTIME, e -> missionTimerX.setInitialTime((LocalDateTime) e.object));
        missionTimerX.setVisible(false);
        scene.addEventHandler(TimelineEvent.TIMELINE_UPDATE_ANIMATIONTIME, e -> {
            missionTimerX.updateTime(timelineAnimation.getTimeFromStart(),
                timelineAnimation.getStringCurrentPropRate());
        });
        // EVENT HANDLER TO CHANGE THE PROPAGATION RATE ON SCREEN
        scene.addEventHandler(TimelineEvent.TIMELINE_GET_CURRENT_PROP_RATE, e -> {
            missionTimerX.updatePropRate(timelineAnimation.getStringCurrentPropRate());
        });
        //let timelineAnimation control missionTimerX
        missionTimerX.internalClock = false; //Disable internal ticking...
        missionTimerX.start();
        timelineAnimation = new TimelineAnimation();
        timelineAnimation.setScene(scene);
        timelineAnimation.start();
    }
}
