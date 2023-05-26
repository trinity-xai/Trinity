package edu.jhuapl.trinity;

import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.components.CircleProgressIndicator;
import edu.jhuapl.trinity.javafx.components.MatrixOverlay;
import edu.jhuapl.trinity.javafx.components.ProgressStatus;
import edu.jhuapl.trinity.javafx.components.radial.MainNavMenu;
import edu.jhuapl.trinity.javafx.components.timeline.Item;
import edu.jhuapl.trinity.javafx.components.timeline.MissionTimerX;
import edu.jhuapl.trinity.javafx.components.timeline.MissionTimerXBuilder;
import edu.jhuapl.trinity.javafx.components.timeline.TimelineAnimation;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.FullscreenEvent;
import edu.jhuapl.trinity.javafx.events.GaussianMixtureEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.NeuralEvent;
import edu.jhuapl.trinity.javafx.events.SearchEvent;
import edu.jhuapl.trinity.javafx.events.SemanticMapEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.javafx.events.ZeroMQEvent;
import edu.jhuapl.trinity.javafx.handlers.FeatureVectorEventHandler;
import edu.jhuapl.trinity.javafx.handlers.GaussianMixtureEventHandler;
import edu.jhuapl.trinity.javafx.handlers.ManifoldEventHandler;
import edu.jhuapl.trinity.javafx.handlers.NeuralEventHandler;
import edu.jhuapl.trinity.javafx.handlers.SearchEventHandler;
import edu.jhuapl.trinity.javafx.handlers.SemanticMapEventHandler;
import edu.jhuapl.trinity.javafx.javafx3d.Hyperspace3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.Hypersurface3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.Projections3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.RetroWavePane;
import edu.jhuapl.trinity.messages.MessageProcessor;
import edu.jhuapl.trinity.messages.ZeroMQFeedManager;
import edu.jhuapl.trinity.messages.ZeroMQSubscriberConfig;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.Configuration;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.umap.Umap;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import lit.litfx.controls.covalent.events.CovalentPaneEvent;
import lit.litfx.controls.output.AnimatedText;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class App extends Application {

    Pane desktopPane;
    StackPane centerStack;
    MainNavMenu mainNavMenu;
    //make transparent so it doesn't interfere with subnode transparency effects
    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
    Timeline intro;
    MatrixOverlay matrixOverlay;
    RetroWavePane retroWavePane = null;
    Hyperspace3DPane hyperspace3DPane;
    Hypersurface3DPane hypersurface3DPane;
    Projections3DPane projections3DPane;
    CircleProgressIndicator circleSpinner;

    static Configuration theConfig;
    static Pane pathPane;
    static Scene theScene;
    //Command line argument support
    Map<String, String> namedParameters;
    List<String> unnamedParameters;
    AnimatedText animatedConsoleText;

    MessageProcessor processor;
    ZeroMQSubscriberConfig subscriberConfig;
    ZeroMQFeedManager feed;
    ManifoldEventHandler meh;
    FeatureVectorEventHandler fveh;
    GaussianMixtureEventHandler gmeh;
    SemanticMapEventHandler smeh;
    SearchEventHandler seh;
    NeuralEventHandler neh;
    boolean hyperspaceIntroShown = false;
    boolean hypersurfaceIntroShown = false;
    boolean matrixShowing = false;
    boolean enableMatrix = false;
    static boolean matrixEnabled = false;
    MissionTimerX missionTimerX;
    TimelineAnimation timelineAnimation;
    boolean is4k = false;

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("Attempting to read defaults...");
        try {
            System.out.println("Build Date: " + Configuration.getBuildDate());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //theConfig = Configuration.defaultConfiguration();
        System.out.println("Starting JavaFX rendering...");
        centerStack = new StackPane();
        centerStack.setBackground(transBack);
        Font font = new Font("Consolas", 30);
        animatedConsoleText = new AnimatedText(" ", font, Color.GREEN, AnimatedText.ANIMATION_STYLE.TYPED);
        animatedConsoleText.setStyle("    -fx-font-size: 30;");
        StackPane.setAlignment(animatedConsoleText, Pos.BOTTOM_LEFT);
        animatedConsoleText.setTranslateY(-100);
        centerStack.getChildren().add(animatedConsoleText);

        System.out.println("Building Scene Stack...");
        BorderPane bp = new BorderPane(centerStack);
        bp.setBackground(transBack);
        bp.getStyleClass().add("trinity-pane");
        Scene scene = new Scene(bp, Color.BLACK);
        stage.setScene(scene);
        stage.show();

        System.out.println("Styling Scene and Stage...");
        //animatedConsoleText.animate("Styling Scene and Stage...");
        stage.setTitle("Trinity");
        //Set icon for stage for fun
        stage.getIcons().add(new Image(getClass().getResource("icons/stageicon.png").toExternalForm()));
        //disable the default fullscreen exit mechanism
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        //Setup a custom key handlers
        stage.setFullScreenExitHint("Press ALT+ENTER to alternate Fullscreen Mode.");
        scene.addEventHandler(KeyEvent.KEY_RELEASED, e -> keyReleased(stage, e));
        scene.addEventHandler(FullscreenEvent.SET_FULLSCREEN, e -> stage.setFullScreen(e.setFullscreen));
        //Make everything pretty
        String CSS = this.getClass().getResource("/edu/jhuapl/trinity/css/styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = this.getClass().getResource("/edu/jhuapl/trinity/css/covalent.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        desktopPane = new Pane(); //transparent layer that just holds floating panes
        desktopPane.setPickOnBounds(false); //prevent it from blocking mouse clicks to sublayers
        //@HACK This ultra dirty hack lets me easily add floating panes from anywhere
        pathPane = desktopPane;
        theScene = scene;
        matrixEnabled = enableMatrix;
        //</HACK>
        System.out.println("Building menu system...");
        //animatedConsoleText.animate("Building menu system...");
        mainNavMenu = new MainNavMenu(scene);
        StackPane.setAlignment(mainNavMenu, Pos.BOTTOM_RIGHT);
        mainNavMenu.hideRadialMenu();
        mainNavMenu.setTranslateX(-mainNavMenu.getInnerRadius());
        mainNavMenu.setTranslateY(-mainNavMenu.getInnerRadius());

        System.out.println("Constructing 3D subscenes...");
        //animatedConsoleText.animate("Constructing 3D subscenes...");
        hypersurface3DPane = new Hypersurface3DPane(scene);
        hypersurface3DPane.setVisible(false); //start off hidden
        projections3DPane = new Projections3DPane(scene);
        projections3DPane.setVisible(false); //start off hidden
        hyperspace3DPane = new Hyperspace3DPane(scene);

        System.out.println("Registering Event Handlers...");
        //animatedConsoleText.animate("Registering Event Handlers...");
        scene.addEventHandler(FeatureVectorEvent.REQUEST_FEATURE_COLLECTION, event -> {
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(hyperspace3DPane.getAllFeatureVectors());
            hypersurface3DPane.addFeatureCollection(fc);
        });

        scene.addEventHandler(ManifoldEvent.GENERATE_NEW_UMAP, event -> {
            Platform.runLater(() -> {
                ProgressStatus ps = new ProgressStatus("Generating UMAP Settings...", -1);
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
            });
            Umap umap = (Umap) event.object1;
            umap.setThreads(12);
            ManifoldEvent.POINT_SOURCE source = (ManifoldEvent.POINT_SOURCE) event.object2;
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
                //filter to only use visible points
                //@TODO SMP || FeatureLayer.visibilityByIndex(fv.getLayer());
                originalFC.setFeatures(
                    hyperspace3DPane.getAllFeatureVectors().stream().filter(fv -> {
                        return FactorLabel.visibilityByLabel(fv.getLabel());
                    }).toList());
            }
            projections3DPane.projectFeatureCollection(originalFC, umap);

        });

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
            projections3DPane.addFeatureCollection(projectedFC);
        });

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
            projections3DPane.addFeatureCollection(projectedFC);
        });

        scene.addEventHandler(FeatureVectorEvent.EXPORT_FEATURE_COLLECTION, event -> {
            File file = (File) event.object;
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(hyperspace3DPane.getAllFeatureVectors());
            try {
                FeatureCollectionFile fcf = new FeatureCollectionFile(file.getAbsolutePath(), false);
                fcf.featureCollection = fc;
                fcf.writeContent();
            } catch (IOException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
            event.consume();
        });
        hyperspace3DPane.setVisible(false); //start off hidden
        hyperspace3DPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event))
                event.acceptTransferModes(TransferMode.COPY);
            else
                event.consume();
        });
        hyperspace3DPane.addEventHandler(DragEvent.DRAG_DROPPED,
            e -> ResourceUtils.onDragDropped(e, scene));

        scene.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event))
                event.acceptTransferModes(TransferMode.COPY);
            else
                event.consume();
        });
        scene.addEventHandler(DragEvent.DRAG_DROPPED,
            e -> ResourceUtils.onDragDropped(e, scene));

        //Add the base main tools
        //insert before animated console text
        centerStack.getChildren().add(0, hyperspace3DPane);
        centerStack.getChildren().add(0, projections3DPane);
        centerStack.getChildren().add(0, hypersurface3DPane);


        System.out.println("Parsing command line...");
        //animatedConsoleText.animate("Parsing command line...");
        parseCommandLine();
        //ex: --scenario="C:\dev\cameratests" --geometry=1024x768+100+100
        if (null != namedParameters) {
            System.out.println("Checking for geometry arguments...");
            if (namedParameters.containsKey("geometry")) {
                String geometryParamString = namedParameters.get("geometry");
                System.out.println("Attempting custom window geometry using " + geometryParamString);
                try {
                    //example: 200x100+800+800
                    String[] tokens = geometryParamString.split("\\+");
                    String[] sizeTokens = tokens[0].split("x");
                    stage.setWidth(Double.valueOf(sizeTokens[0]));
                    stage.setHeight(Double.valueOf(sizeTokens[1]));
                    stage.setX(Double.valueOf(tokens[1]));
                    stage.setY(Double.valueOf(tokens[2]));
                } catch (Exception ex) {
                    System.out.println("Exception thrown parsing: " + geometryParamString
                        + ". Setting to Maximized.");
                    stage.setMaximized(true);
                }
            } else if (namedParameters.containsKey("fullscreen")) {
                System.out.println("Fullscreen start requested.");
                stage.setFullScreen(true);
            } else {
                System.out.println("Defaulting to maximized.");
                stage.setMaximized(true);
            }
            System.out.println("Checking for special effects requests...");
            if (namedParameters.containsKey("outrun")) {
                System.out.println("Outrun found... enabling RetroWavePane.");
                try {
                    //Add optional RETROWAVE VIEW
                    retroWavePane = new RetroWavePane(scene);
                    retroWavePane.setVisible(true);
                    centerStack.getChildren().add(retroWavePane);
                } catch (Exception ex) {

                }
            }
            if (namedParameters.containsKey("display4k")) {
                System.out.println("Display4k found... adjusting sizing.");
                is4k = true;
            }
            if (namedParameters.containsKey("matrix")) {
                System.out.println("Matrix found... enabling digital rain.");
                enableMatrix = true;
                matrixEnabled = enableMatrix;
            }

            System.out.println("Checking for custom configuration...");
            if (namedParameters.containsKey("config")) {
                String configFile = namedParameters.get("config");
                System.out.println("Configuration file found: " + configFile);
                try {
                    theConfig = new Configuration(configFile);
                } catch (IOException ex) {
                    System.out.println("Exception thrown loading: " + configFile);
                    //Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Loading defaults.");
                    theConfig = Configuration.defaultConfiguration();
                }
            }
        }
        setupMissionTimer(scene);
        //add helper tools and overlays
        circleSpinner = new CircleProgressIndicator();
        circleSpinner.setLabelLater("...Working...");
        circleSpinner.defaultOpacity = 1.0;
        circleSpinner.setOpacity(0.0); ///instead of setVisible(false)

        centerStack.getChildren().remove(animatedConsoleText);
        centerStack.getChildren().addAll(desktopPane, mainNavMenu,
            missionTimerX, circleSpinner);
        centerStack.getChildren().add(animatedConsoleText);

        // fun matrix effect, use Alt + N
        matrixOverlay = new MatrixOverlay(scene, centerStack);
        bp.setOnSwipeUp(e -> {
            if (enableMatrix)
                if (e.getTouchCount() > 2) {
                    MatrixOverlay.on.set(false);
                    matrixOverlay.matrixOff.run();
                    e.consume(); // <-- stops passing the event to next node
                }
        });
        bp.setOnSwipeDown(e -> {
            if (enableMatrix)
                if (e.getTouchCount() > 2) {
                    MatrixOverlay.on.set(true);
                    matrixOverlay.matrixOn.run();
                    e.consume(); // <-- stops passing the event to next node
                }
        });
        scene.addEventHandler(ApplicationEvent.SHOW_BUSY_INDICATOR, e -> {
            if (null == e.object) return;
            circleSpinner.updateStatus((ProgressStatus) e.object);
            circleSpinner.fadeBusy(false);
            circleSpinner.spin(true);
        });
        scene.addEventHandler(ApplicationEvent.HIDE_BUSY_INDICATOR, e -> {
            circleSpinner.spin(false);
            circleSpinner.fadeBusy(true);
        });
        scene.addEventHandler(ApplicationEvent.UPDATE_BUSY_INDICATOR, e -> {
            if (null == e.object) return;
            circleSpinner.updateStatus((ProgressStatus) e.object);
        });

        scene.addEventHandler(ApplicationEvent.SHUTDOWN, e -> shutdown(false));

        scene.addEventHandler(ApplicationEvent.SHOW_PROJECTIONS, e -> {
            if (projections3DPane.isVisible()) {
                projections3DPane.setVisible(false);
                if (null != retroWavePane)
                    retroWavePane.animateShow();
            } else {
                if (null != retroWavePane)
                    retroWavePane.animateHide();
                hyperspace3DPane.setVisible(false);
                hypersurface3DPane.setVisible(false);
                Platform.runLater(() -> {
                    projections3DPane.setVisible(true);
                });
            }
        });

        scene.addEventHandler(ApplicationEvent.SHOW_HYPERSURFACE, e -> {
            if (hypersurface3DPane.isVisible()) {
                hypersurface3DPane.hideFA3D();
                if (null != retroWavePane)
                    retroWavePane.animateShow();
            } else {
                if (null != retroWavePane)
                    retroWavePane.animateHide();
                hyperspace3DPane.setVisible(false);
                if (null != projections3DPane)
                    projections3DPane.setVisible(false);
                fadeOutConsole(500);
                if (!hypersurfaceIntroShown) {
                    hypersurface3DPane.showFA3D();
                    hypersurfaceIntroShown = true;
                } else
                    Platform.runLater(() -> {
                        hypersurface3DPane.setVisible(true);
                    });

            }
        });

        scene.addEventHandler(ApplicationEvent.SHOW_HYPERSPACE, e -> {
            if (hyperspace3DPane.isVisible()) {
                hyperspace3DPane.setVisible(false);
                if (null != retroWavePane)
                    retroWavePane.animateShow();
                fadeOutConsole(500);
            } else {
                hypersurface3DPane.setVisible(false);
                if (null != projections3DPane)
                    projections3DPane.setVisible(false);
                if (null != retroWavePane)
                    retroWavePane.animateHide();
                hyperspace3DPane.setVisible(true);
                if (!hyperspaceIntroShown) {
                    hyperspace3DPane.intro(1000);
                    hyperspaceIntroShown = true;
                }
                fadeInConsole(500);
            }
        });

        gmeh = new GaussianMixtureEventHandler();
        scene.getRoot().addEventHandler(GaussianMixtureEvent.NEW_GAUSSIAN_MIXTURE, gmeh);
        scene.getRoot().addEventHandler(GaussianMixtureEvent.LOCATE_GAUSSIAN_MIXTURE, gmeh);
        scene.getRoot().addEventHandler(GaussianMixtureEvent.NEW_GAUSSIAN_COLLECTION, gmeh);
        gmeh.addGaussianMixtureRenderer(hyperspace3DPane);

        fveh = new FeatureVectorEventHandler(false);
        scene.getRoot().addEventHandler(FeatureVectorEvent.NEW_FEATURE_VECTOR, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.LOCATE_FEATURE_VECTOR, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.RESCAN_FACTOR_LABELS, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.RESCAN_FEATURE_LAYERS, fveh);
        scene.getRoot().addEventHandler(FeatureVectorEvent.NEW_LABEL_CONFIG, fveh);
        fveh.addFeatureVectorRenderer(hyperspace3DPane);

        meh = new ManifoldEventHandler();
        scene.getRoot().addEventHandler(ManifoldEvent.CLEAR_ALL_MANIFOLDS, meh);
        scene.getRoot().addEventHandler(ManifoldEvent.GENERATE_HYPERSPACE_MANIFOLD, meh);
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
//        meh.addManifoldRenderer(hyperspace3DPane);
        meh.addManifoldRenderer(projections3DPane);

        neh = new NeuralEventHandler();
        scene.getRoot().addEventHandler(NeuralEvent.NEW_NEURAL_TRIAL, neh);
        scene.getRoot().addEventHandler(NeuralEvent.NEURAL_TRIAL_LIST, neh);
        neh.addNeuralRenderer(hypersurface3DPane);

        smeh = new SemanticMapEventHandler(false);
        scene.getRoot().addEventHandler(SemanticMapEvent.NEW_SEMANTIC_MAP, smeh);
//        scene.getRoot().addEventHandler(SemanticMapEvent.LOCATE_FEATURE_VECTOR, smeh);
        scene.getRoot().addEventHandler(SemanticMapEvent.NEW_SEMANTICMAP_COLLECTION, smeh);
        smeh.addFeatureVectorRenderer(hyperspace3DPane);
        smeh.addSemanticMapRenderer(hypersurface3DPane);

        seh = new SearchEventHandler();
        scene.getRoot().addEventHandler(SearchEvent.FILTER_BY_TERM, seh);
        scene.getRoot().addEventHandler(SearchEvent.FILTER_BY_SCORE, seh);
        scene.getRoot().addEventHandler(SearchEvent.CLEAR_ALL_FILTERS, seh);
        seh.addFeatureVectorRenderer(hyperspace3DPane);

        scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, covalentEvent -> {
            desktopPane.getChildren().remove(covalentEvent.pathPane);
        });
        System.out.println("Establishing Messaging Feed...");
        //animatedConsoleText.animate("Establishing Messaging Feed...");
        processor = new MessageProcessor(scene);
        subscriberConfig = new ZeroMQSubscriberConfig(
            "ZeroMQ Subscriber", "Testing ZeroMQFeedManager.",
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
        scene.addEventHandler(CommandTerminalEvent.FOLLOWUP, e -> {
            animatedConsoleText.setVisible(true);
            animatedConsoleText.setOpacity(1.0);

            final int originalLength = animatedConsoleText.getText().length();
            final IntegerProperty i = new SimpleIntegerProperty(originalLength);
            Timeline timeline = new Timeline();
            String animatedString = animatedConsoleText.getText() + e.text;
            KeyFrame keyFrame1 = new KeyFrame(Duration.millis(animatedConsoleText.getAnimationTimeMS()), event -> {
                if (i.get() > animatedString.length()) {
                    timeline.stop();
                } else {
                    animatedConsoleText.setText(animatedString.substring(0, i.get()));
                    i.set(i.get() + 1);
                }
            });
            timeline.getKeyFrames().addAll(keyFrame1);
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
        });

        scene.addEventHandler(CommandTerminalEvent.NOTIFICATION, e -> {
            animatedConsoleText.setAnimationTimeMS(15);  //default is 30ms
            animatedConsoleText.setFont(e.font);
            animatedConsoleText.setFill(e.color);
            animatedConsoleText.setStroke(e.color);
            animatedConsoleText.setText("> ");
            animatedConsoleText.setVisible(true);
            animatedConsoleText.setOpacity(1.0);
            animatedConsoleText.animate("> " + e.text);
        });
        scene.addEventHandler(CommandTerminalEvent.FADE_OUT, e -> fadeOutConsole(e.timeMS));
        System.out.println("User Interface Lit...");
        //animatedConsoleText.animate("User Inteface Lit...");
        intro();
        missionTimerX.updateTime(0, "1");
    }

    private void setupMissionTimer(Scene scene) {
        double width = 900;
        double height = 200;
        double iconFitWidth = 30;
        if (is4k) {
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

    private void fadeOutConsole(long timeMS) {
        animatedConsoleText.setAnimationTimeMS(30);  //default is 30ms
        FadeTransition fade = new FadeTransition(Duration.millis(timeMS), animatedConsoleText);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(f -> animatedConsoleText.setText("> "));
        fade.play();
    }

    private void fadeInConsole(long timeMS) {
        animatedConsoleText.setAnimationTimeMS(30);  //default is 30ms
        FadeTransition fade = new FadeTransition(Duration.millis(timeMS), animatedConsoleText);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setOnFinished(f -> animatedConsoleText.setText("> "));
        fade.play();
    }

    private void intro() {
        Platform.runLater(() -> {
            animatedConsoleText.setOpacity(1.0);
            animatedConsoleText.setText("> ");
            animatedConsoleText.setVisible(true);
            intro = new Timeline(
                new KeyFrame(Duration.seconds(0.1), e -> animatedConsoleText.animate(">Trinity")),
                new KeyFrame(Duration.seconds(2.0), e -> animatedConsoleText.animate(">Hyperdimensional Visualization")),
                new KeyFrame(Duration.seconds(3.5), new KeyValue(animatedConsoleText.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(5.0), new KeyValue(animatedConsoleText.opacityProperty(), 0.0)),
                new KeyFrame(Duration.seconds(5.1), e -> animatedConsoleText.setVisible(false)),
                new KeyFrame(Duration.seconds(5.1), e -> animatedConsoleText.animate(" "))
            );
            intro.play();
        });
    }

    /**
     * Key handler for various keyboard shortcuts
     */
    private void keyReleased(Stage stage, KeyEvent e) {
        //Enter/Exit fullscreen
        if (e.isAltDown() && e.getCode().equals(KeyCode.ENTER)) {
            stage.setFullScreen(!stage.isFullScreen());
        }
        //Terminate the app by entering an exit animation
        if (e.isControlDown() && e.isShiftDown() &&
            e.getCode().equals(KeyCode.C)) {
            shutdown(false);
        }
        if (e.isAltDown() && e.getCode().equals(KeyCode.N)) {
            matrixShowing = !matrixShowing;
            if (!matrixShowing)
                intro();
        }
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Application Shutdown commenced...");
        shutdown(true);
    }

    private void shutdown(boolean now) {
        if (null != missionTimerX)
            missionTimerX.stop();
        if (null != feed)
            feed.disconnect(true); //if no connection does nothing
        if (now)
            System.exit(0);
        else {
            animatedConsoleText.setOpacity(0.0);
            animatedConsoleText.setVisible(true);
            animatedConsoleText.setText(">");
            if (null != intro)
                intro.stop();
            Timeline outtro = new Timeline(
                new KeyFrame(Duration.seconds(0.1), new KeyValue(animatedConsoleText.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(0.1), kv -> animatedConsoleText.animate(">Kill Signal Received. Terminating...")),
                new KeyFrame(Duration.seconds(2.0), kv -> System.exit(0)));
            outtro.play();
        }
    }

    private void parseCommandLine() {
        Parameters parameters = getParameters();

        namedParameters = parameters.getNamed();
        unnamedParameters = parameters.getUnnamed();

        if (!namedParameters.isEmpty()) {
            System.out.println("NamedParameters :");
            namedParameters.entrySet().forEach(entry -> {
                System.out.println("\t" + entry.getKey() + " : " + entry.getValue());
            });
        }
        if (!unnamedParameters.isEmpty()) {
            System.out.println("UnnamedParameters :");
            unnamedParameters.forEach(System.out::println);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Static Accessors">

    /**
     * Dirty Hack to allow components to share a common configuration
     *
     * @return The current config
     */
    public static Configuration getConfig() {
        if (null == theConfig)
            theConfig = Configuration.defaultConfiguration();
        return theConfig;
    }

    /**
     * Dirty Hack to allow dialogs to fire messages
     *
     * @return The actual scene that spawned the dialog
     */
    public static Scene getAppScene() {
        return theScene;
    }

    /**
     * Dirty Hack to allow floating PathPane objects to be added from anywhere
     *
     * @return The center stack pane that holds everything
     */
    public static Pane getAppPathPaneStack() {
        return pathPane;
    }

    /**
     *
     */
    public static boolean isMatrixEnabled() {
        return matrixEnabled;
    }
    //</editor-fold>

    /**
     * Ye olde main()
     *
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }
}
