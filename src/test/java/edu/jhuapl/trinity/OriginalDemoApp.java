package edu.jhuapl.trinity;

import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.data.Trial;
import edu.jhuapl.trinity.javafx.components.MatrixOverlay;
import edu.jhuapl.trinity.javafx.components.panes.FactorAnalysisControlPane;
import edu.jhuapl.trinity.javafx.components.panes.FactorAnalysisVisibilityPane;
import edu.jhuapl.trinity.javafx.components.panes.FactorRadarPlotPane;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.FactorAnalysisEvent;
import edu.jhuapl.trinity.javafx.events.FullscreenEvent;
import edu.jhuapl.trinity.javafx.events.SelectTrajectory3DEvent;
import edu.jhuapl.trinity.javafx.events.VisibilityEvent;
import edu.jhuapl.trinity.javafx.events.VisibilityObject;
import edu.jhuapl.trinity.javafx.javafx3d.FactorAnalysis3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.Trajectory3D;
import edu.jhuapl.trinity.javafx.javafx3d.TrajectoryState;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
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
import lit.litfx.controls.covalent.PathPane;
import lit.litfx.controls.covalent.events.CovalentPaneEvent;
import lit.litfx.controls.output.AnimatedText;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Optional;
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
@Deprecated
public class OriginalDemoApp extends Application {

    Pane desktopPane;
    StackPane centerStack;
    //make transparent so it doesn't interfere with subnode transparency effects
    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
    Timeline intro;
    Pane controlsDesktopPane;
    MatrixOverlay matrixOverlay;
    FactorAnalysis3DPane gpfa3Dpane;
    FactorAnalysis3DPane umap3Dpane;
    FactorRadarPlotPane frpPane;
    FactorAnalysisControlPane facpPane;
    FactorAnalysisVisibilityPane faoPane;
    static Pane pathPane;
    static Scene theScene;

    double defaultSceneWidth = 1920;
    double defaultSceneHeight = 1080;
    AnimatedText introText;

    ArrayList<Trial> trialList;
    //    ArrayList<SpikeTrain> spikeTrainList;
    int currentXFactor = 0;
    int currentYFactor = 1;
    int currentZFactor = 2;

    SimpleDoubleProperty currentScaleProperty = new SimpleDoubleProperty(500.0);
    SimpleDoubleProperty currentSpikeScaleProperty = new SimpleDoubleProperty(10.0);

    Group day1group = new Group();
    Group day8group = new Group();
    Group day19group = new Group();
    Group day36group = new Group();
    Group day43amgroup = new Group();
    Group day43pmgroup = new Group();

    Group day1PointGroup = new Group();
    Group day8PointGroup = new Group();
    Group day19PointGroup = new Group();
    Group day36PointGroup = new Group();
    Group day43amPointGroup = new Group();
    Group day43pmPointGroup = new Group();
    VisibilityObject vo = null;

    @Override
    public void start(Stage stage) throws IOException {
//        mainPane = new Pane();
//        mainPane.setBackground(transBack);
//        mainPane.getStyleClass().add("cascada-pane");

        centerStack = new StackPane();
        centerStack.setBackground(transBack);
        BorderPane bp = new BorderPane(centerStack);
        bp.getStyleClass().add("cascada-pane");
        Scene scene = new Scene(bp, defaultSceneWidth, defaultSceneHeight, Color.BLACK);

        desktopPane = new Pane(); //transparent layer that just holds floating panes
        desktopPane.setPickOnBounds(false); //prevent it from blocking mouse clicks to sublayers
        //@HACK This ultra dirty hack lets me easily add floating panes from anywhere
        pathPane = desktopPane;
        theScene = scene;
        App.pathPane = desktopPane;
        App.theScene = scene;
        //</HACK>

//        gpfa3Dpane = new FactorAnalysis3DPane(scene);
//        umap3Dpane = new FactorAnalysis3DPane(scene);
//        HBox hbox = new HBox(gpfa3Dpane, umap3Dpane);
//        centerStack.getChildren().add(new BorderPane(hbox));

        gpfa3Dpane = new FactorAnalysis3DPane(scene);
        centerStack.getChildren().add(gpfa3Dpane);

        trialList = new ArrayList<>();
        gpfa3Dpane.addTrajectoryGroup(day1group, day1PointGroup);
        gpfa3Dpane.addTrajectoryGroup(day8group, day8PointGroup);
        gpfa3Dpane.addTrajectoryGroup(day19group, day19PointGroup);
        gpfa3Dpane.addTrajectoryGroup(day36group, day36PointGroup);
        gpfa3Dpane.addTrajectoryGroup(day43amgroup, day43amPointGroup);
        gpfa3Dpane.addTrajectoryGroup(day43pmgroup, day43pmPointGroup);

        facpPane = new FactorAnalysisControlPane(500, 700);
        faoPane = new FactorAnalysisVisibilityPane(300, 300);
        frpPane = new FactorRadarPlotPane(1600, 250);

        updateByTrials();

        Font font = new Font("Consolas", 80);
        introText = new AnimatedText(" ", font, Color.GREEN, AnimatedText.ANIMATION_STYLE.TYPED);
        introText.setStyle("    -fx-font-size: 80;");
        centerStack.getChildren().add(introText);
        StackPane.setAlignment(introText, Pos.BOTTOM_LEFT);
        introText.setTranslateY(-100);
        centerStack.getChildren().addAll(desktopPane);

        //Make everything pretty
        String CSS = this.getClass().getResource("/edu/jhuapl/trinity/css/styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = this.getClass().getResource("/edu/jhuapl/trinity/css/covalent.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        //disable the default fullscreen exit mechanism
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        //Setup a custom key handlers
        stage.setFullScreenExitHint("Press ALT+ENTER to alternate Fullscreen Mode.");
        scene.addEventHandler(KeyEvent.KEY_RELEASED, e -> keyReleased(scene, stage, e));
        scene.addEventHandler(FullscreenEvent.SET_FULLSCREEN, e -> stage.setFullScreen(e.setFullscreen));

        stage.setScene(scene);
        stage.setTitle("Trinity");
        stage.setFullScreen(true);
        stage.show();

        // fun matrix effect, use Alt + N
        matrixOverlay = new MatrixOverlay(scene, centerStack);

        scene.addEventHandler(ApplicationEvent.SHUTDOWN, e -> shutdown());


        //React whenever someone drags and drops from the palette
        centerStack.setOnDragOver(event -> onDragOver(event));
        centerStack.setOnDragDropped(event -> onDragDropped(event));

        scene.addEventHandler(VisibilityEvent.VISIBILITY_CHANGED, e -> {
            vo = (VisibilityObject) e.eventObject;
            updateVisibility();
        });

        scene.addEventHandler(FactorAnalysisEvent.XFACTOR_SELECTION, e -> {
            clearTrials();
            currentXFactor = (int) e.object; //convert factor id string to number
            updateByTrials();
            updateVisibility();
        });
        scene.addEventHandler(FactorAnalysisEvent.YFACTOR_SELECTION, e -> {
            clearTrials();
            currentYFactor = (int) e.object; //convert factor id string to number
            updateByTrials();
            updateVisibility();
        });
        scene.addEventHandler(FactorAnalysisEvent.ZFACTOR_SELECTION, e -> {
            clearTrials();
            currentZFactor = (int) e.object; //convert factor id string to number
            updateByTrials();
            updateVisibility();
        });
        scene.addEventHandler(FactorAnalysisEvent.SCALE_UPDATED, e -> {
            clearTrials();
            currentScaleProperty.set((int) e.object);
            updateByTrials();
            updateVisibility();
        });

        scene.getRoot().addEventHandler(SelectTrajectory3DEvent.SELECT_TRAJECTORY_3D, e -> {
            Trajectory3D traj3D = (Trajectory3D) e.eventObject;
            facpPane.setTrajectory3D(traj3D);
            Optional<Trial> optTrial = trialList.stream()
                .filter(trial -> trial.trialId.contentEquals(traj3D.trajectory.trialID))
                .findFirst();
            if (optTrial.isPresent()) {
                Trial trial = optTrial.get();
                frpPane.setTrial(trial);
            }
        });
        scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, covalentEvent -> {
            desktopPane.getChildren().remove(covalentEvent.pathPane);
        });

//        scene.addEventHandler(ApplicationEvent.RESTORE_PANES, e-> restorePanes());
        Platform.runLater(() -> {
            intro = new Timeline(
                new KeyFrame(Duration.seconds(0.1), e -> introText.animate("Trinity")),
                new KeyFrame(Duration.seconds(4.0), e -> introText.animate("Brain Machine Interface Visualization")),
                new KeyFrame(Duration.seconds(4.1), new KeyValue(introText.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(7.0), new KeyValue(introText.opacityProperty(), 0.0)),
                new KeyFrame(Duration.seconds(7.1), e -> introText.setVisible(false)),
                new KeyFrame(Duration.seconds(7.1), e -> introText.animate(" "))

                , new KeyFrame(Duration.seconds(5.0), e -> {
                PathPane facpPathPane = new PathPane(scene, desktopPane, 600, 700, facpPane,
                    "Multi-factor Parameters", "Factor Selection", 500.0, 600.0);
                facpPathPane.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEvent -> facpPathPane.toFront());
                desktopPane.getChildren().add(facpPathPane);
                facpPathPane.moveTo(0, 0);
            })
                , new KeyFrame(Duration.seconds(5.0), e -> {
                PathPane faoPathPane = new PathPane(scene, desktopPane, 400, 400, faoPane,
                    "Multi-factor Parameters", "Visibility Options", 400.0, 500.0);
                faoPathPane.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEvent -> faoPathPane.toFront());
                desktopPane.getChildren().add(faoPathPane);
                faoPathPane.moveTo(desktopPane.getWidth() - 400, 0);
            })
//                    , new KeyFrame(Duration.seconds(2.0), e -> {
//                        PathPane frpPathPane = new PathPane(scene, desktopPane, 1650, 275, frpPane,
//                            "Multi-factor Parameters", "Time-oriented RADAR plot", 500.0, 600.0);
//                        frpPathPane.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEvent -> frpPathPane.toFront());
//                        desktopPane.getChildren().add(frpPathPane);
//                        frpPathPane.moveTo(0, desktopPane.getHeight() - 300);
//                    })
            );
//            intro.setDelay(Duration.seconds(2));
            intro.play();
        });
    }

    private void intro() {
        Platform.runLater(() -> {
            introText.setOpacity(1.0);
            introText.setVisible(true);
            intro = new Timeline(
                new KeyFrame(Duration.seconds(0.1), e -> introText.animate("Trinity")),
                new KeyFrame(Duration.seconds(4.0), e -> introText.animate("Brain Machine Interface Visualization")),
                new KeyFrame(Duration.seconds(4.1), new KeyValue(introText.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(6.0), new KeyValue(introText.opacityProperty(), 0.0)),
                new KeyFrame(Duration.seconds(6.1), e -> introText.setVisible(false)),
                new KeyFrame(Duration.seconds(6.1), e -> introText.animate(" "))
            );
            intro.play();
        });
    }

    private void updateVisibility() {
        if (null != vo) {
            updateVisibility(day1group, day1PointGroup, vo);
            updateVisibility(day8group, day8PointGroup, vo);
            updateVisibility(day19group, day19PointGroup, vo);
            updateVisibility(day36group, day36PointGroup, vo);
            updateVisibility(day43amgroup, day43amPointGroup, vo);
            updateVisibility(day43pmgroup, day43pmPointGroup, vo);

            day1group.setVisible(vo.day1Group);
            day1PointGroup.setVisible(vo.day1Group);
            day8group.setVisible(vo.day8Group);
            day8PointGroup.setVisible(vo.day8Group);
            day19group.setVisible(vo.day19Group);
            day19PointGroup.setVisible(vo.day19Group);
            day36group.setVisible(vo.day36Group);
            day36PointGroup.setVisible(vo.day36Group);
            day43amgroup.setVisible(vo.day43amGroup);
            day43amPointGroup.setVisible(vo.day43amGroup);
            day43pmgroup.setVisible(vo.day43pmGroup);
            day43pmPointGroup.setVisible(vo.day43pmGroup);
        }
    }

    private void clearTrials() {
        day1group.getChildren().clear();
        day1PointGroup.getChildren().clear();
        day8group.getChildren().clear();
        day8PointGroup.getChildren().clear();
        day19group.getChildren().clear();
        day19PointGroup.getChildren().clear();
        day36group.getChildren().clear();
        day36PointGroup.getChildren().clear();
        day43amgroup.getChildren().clear();
        day43amPointGroup.getChildren().clear();
        day43pmgroup.getChildren().clear();
        day43pmPointGroup.getChildren().clear();
    }

    private void updateByTrials() {
        double scale = 300.0;
        trialList.clear();
        File day1File = new File("c:\\dev\\trinity\\seqTrainGPFA-day1.txt");
        ArrayList<Trial> day1Trials = Trial.readTrialFile(day1File);
        trialList.addAll(day1Trials);
        updateFactorAnalysis(0, 1, day1Trials, scale, Color.RED);
        updateFactorAnalysis(1, 1, day1Trials, scale, Color.GREEN);
        updateFactorAnalysis(2, 1, day1Trials, scale, Color.BLUE);

        File day8File = new File("c:\\dev\\trinity\\seqTrainGPFA-day2.txt");
        ArrayList<Trial> day8Trials = Trial.readTrialFile(day8File);
        trialList.addAll(day8Trials);
        updateFactorAnalysis(0, 2, day8Trials, scale, Color.RED);
        updateFactorAnalysis(1, 2, day8Trials, scale, Color.GREEN);
        updateFactorAnalysis(2, 2, day8Trials, scale, Color.BLUE);

        File day19File = new File("c:\\dev\\trinity\\seqTrainGPFA-day3.txt");
        ArrayList<Trial> day19Trials = Trial.readTrialFile(day19File);
        trialList.addAll(day19Trials);
        updateFactorAnalysis(0, 3, day19Trials, scale, Color.RED);
        updateFactorAnalysis(1, 3, day19Trials, scale, Color.GREEN);
        updateFactorAnalysis(2, 3, day19Trials, scale, Color.BLUE);

        File day36File = new File("c:\\dev\\trinity\\seqTrainGPFA-day4.txt");
        ArrayList<Trial> day36Trials = Trial.readTrialFile(day36File);
        trialList.addAll(day36Trials);
        updateFactorAnalysis(0, 4, day36Trials, scale, Color.RED);
        updateFactorAnalysis(1, 4, day36Trials, scale, Color.GREEN);
        updateFactorAnalysis(2, 4, day36Trials, scale, Color.BLUE);

        File day43amFile = new File("c:\\dev\\trinity\\seqTrainGPFA-day5.txt");
        ArrayList<Trial> day43amTrials = Trial.readTrialFile(day43amFile);
        trialList.addAll(day43amTrials);
        updateFactorAnalysis(0, 5, day43amTrials, scale, Color.RED);
        updateFactorAnalysis(1, 5, day43amTrials, scale, Color.GREEN);
        updateFactorAnalysis(2, 5, day43amTrials, scale, Color.BLUE);

        File day43pmFile = new File("c:\\dev\\trinity\\seqTrainGPFA-day6.txt");
        ArrayList<Trial> day43pmTrials = Trial.readTrialFile(day43pmFile);
        trialList.addAll(day43pmTrials);
        updateFactorAnalysis(0, 6, day43pmTrials, scale, Color.RED);
        updateFactorAnalysis(1, 6, day43pmTrials, scale, Color.GREEN);
        updateFactorAnalysis(2, 6, day43pmTrials, scale, Color.BLUE);
    }

    private void updateVisibility(Group trajGroup, Group trajPointGroup, VisibilityObject vo) {
        trajGroup.getChildren().forEach(c -> {
            Trajectory3D traj3D = (Trajectory3D) c;
            switch (traj3D.trialNumber) {
                case 0:
                    traj3D.setVisible(vo.restGestureGroup);
                    break;
                case 1:
                    traj3D.setVisible(vo.openGestureGroup);
                    break;
                case 2:
                    traj3D.setVisible(vo.pinchGestureGroup);
                    break;
            }
        });
        trajPointGroup.getChildren().forEach(c -> {
            TrajectoryState trajState = (TrajectoryState) c;
            switch (trajState.trialNumber) {
                case 0:
                    trajState.setVisible(vo.restGestureGroup);
                    break;
                case 1:
                    trajState.setVisible(vo.openGestureGroup);
                    break;
                case 2:
                    trajState.setVisible(vo.pinchGestureGroup);
                    break;
            }
        });
    }

    private void updateFactorAnalysis(int trialNumber, int dayNumber, ArrayList<Trial> trials, double scale, Color color) {
        Trajectory trajectory = trials.get(trialNumber).toTrajectory(
            currentXFactor, currentYFactor, currentZFactor);
        Trajectory3D traj3D = JavaFX3DUtils.buildPolyLineFromTrajectory(trialNumber, dayNumber,
            trajectory, color, scale, defaultSceneWidth, defaultSceneHeight);
        switch (dayNumber) {
            case 1:
                gpfa3Dpane.addTrajectory3D(day1group, day1PointGroup, traj3D);
                break;
            case 2:
                gpfa3Dpane.addTrajectory3D(day8group, day8PointGroup, traj3D);
                break;
            case 3:
                gpfa3Dpane.addTrajectory3D(day19group, day19PointGroup, traj3D);
                break;
            case 4:
                gpfa3Dpane.addTrajectory3D(day36group, day36PointGroup, traj3D);
                break;
            case 5:
                gpfa3Dpane.addTrajectory3D(day43amgroup, day43amPointGroup, traj3D);
                break;
            case 6:
                gpfa3Dpane.addTrajectory3D(day43pmgroup, day43pmPointGroup, traj3D);
                break;
        }
    }

    /**
     * Any time a drag event starts this attempts to process the object.
     *
     * @param event DragEvent.DragOver
     */
    public void onDragOver(DragEvent event) {
        event.acceptTransferModes(TransferMode.COPY);
        return;
//        Dragboard db = event.getDragboard();
//        DataFormat dataFormat = DataFormat.lookupMimeType("application/x-java-file-list");
//        try {
//            if (db.hasFiles() || db.hasContent(dataFormat)){
//                List<File> files = db.getFiles();
//                //workaround for Swing JFXPanel
//                if(db.hasContent(dataFormat)) {
//                    //Swing containers require a registered mime type
//                    //since we don't have that, we need to accept the drag
//                    event.acceptTransferModes(TransferMode.COPY);
//
//                    return;
//                }
//            } else {
//                event.consume();
//            }
//        } catch (Exception ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//            event.consume();
//        }
    }

    /**
     * Any time a drop event occurs this attempts to process the object.
     *
     * @param event DragEvent.DragDropped
     */
    public void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            for (File file : db.getFiles()) {
                try {
                    if (isTrialFile(file)) {
                        //File trialFile = new File("c:\\dev\\export_day1_shifted.txt");
                        trialList = Trial.readTrialFile(file);
//                        updateFactorAnalysis(trialList, 500.0, Color.DODGERBLUE);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(OriginalDemoApp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    public static boolean isTrialFile(File file) throws IOException {
        Optional<String> firstLine = Files.lines(file.toPath()).findFirst();
        return firstLine.isPresent() && firstLine.get().startsWith("TrialID");
    }

    /**
     * Key handler for various keyboard shortcuts
     */
    boolean matrixShowing = false;

    private void keyReleased(Scene scene, Stage stage, KeyEvent e) {
//        System.out.println("KEY_RELEASED handler...");
        //Enter/Exit fullscreen
        if (e.isAltDown() && e.getCode().equals(KeyCode.ENTER)) {
            stage.setFullScreen(!stage.isFullScreen());
        }
        //Terminate the app by entering an exit animation
        if (e.isControlDown() && e.getCode().equals(KeyCode.C)) {
            shutdown();
        }
        if (e.isAltDown() && e.getCode().equals(KeyCode.N)) {
            matrixShowing = !matrixShowing;
            if (!matrixShowing)
                intro();
        }
//        if(e.isAltDown() && e.getCode().equals(KeyCode.H)) {
//            gpfa3Dpane.extrasGroup.getChildren().clear();
//            QuickHull3D hull = new QuickHull3D();
//            //Construct an array of Point3D's based on the factor coordinates
////            gpfa3Dpane.get
////            trialList.stream().filter(trial -> trial.trialId.contentEquals(traj3D.trajectory.trialID))
//            Point3d [] points = new Point3d[0];
//        }
    }

    private void shutdown() {
//        mainPane.getChildren().clear();
//        centerStack.getChildren().removeIf(node -> node != mainPane);
//        emittingBands.set(false);
//        animatedConsoleText.setOpacity(0.0);
//        mainPane.getChildren().add(animatedConsoleText);
//        animatedConsoleText.setX(10); animatedConsoleText.setY(30);
//        animatedConsoleText.setText(">");
//        if(null != intro)
//            intro.stop();
//        Timeline outtro = new Timeline(
//            new KeyFrame(Duration.millis(100), new KeyValue(animatedConsoleText.opacityProperty(), 1.0)),
//            new KeyFrame(Duration.seconds(0.1), kv -> animatedConsoleText.animate(">Kill Signal Received. Terminating...")),
//            new KeyFrame(Duration.seconds(2.0), kv -> System.exit(0)));
//        outtro.play();
        System.exit(0);
    }

    //<editor-fold defaultstate="collapsed" desc="Static Accessors">

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
