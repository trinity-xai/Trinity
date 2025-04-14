/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.javafx.components.MatrixOverlay;
import edu.jhuapl.trinity.javafx.components.panes.VideoPane;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.javafx.components.radial.MainNavMenu;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FullscreenEvent;
import edu.jhuapl.trinity.javafx.events.MissionTimerXEvent;
import edu.jhuapl.trinity.javafx.events.ZeroMQEvent;
import edu.jhuapl.trinity.javafx.javafx3d.RetroWavePane;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import edu.jhuapl.trinity.utils.Configuration;
import edu.jhuapl.trinity.utils.Utils;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class App extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    static Configuration theConfig;
    static Pane pathPane;
    static Scene theScene;
    static VideoPane theVideo = null;

    Pane desktopPane;
    StackPane centerStack;
    MainNavMenu mainNavMenu;
    //make transparent so it doesn't interfere with subnode transparency effects
    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
    Timeline intro;
    MatrixOverlay matrixOverlay;
    RetroWavePane retroWavePane = null;
    CircleProgressIndicator circleSpinner;
    //Command line argument support
    Map<String, String> namedParameters;
    AnimatedText animatedConsoleText;

    boolean matrixShowing = false;
    boolean enableMatrix = false;
    static boolean matrixEnabled = false;
    boolean is4k = false;
    boolean enableHttp = false;

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("App.start() begin... ");
        long startTime = System.nanoTime();
        LOG.info("Attempting to read defaults...");
        try {
            LOG.info("Build Date: {}", Configuration.getBuildDate());
        } catch (Exception ex) {
            LOG.error(null, ex);
        }
        LOG.info("Starting JavaFX rendering...");
        centerStack = new StackPane();
        centerStack.setBackground(transBack);
        Font font = new Font("Consolas", 30);
        animatedConsoleText = new AnimatedText(" ", font, Color.GREEN, AnimatedText.ANIMATION_STYLE.TYPED);
        animatedConsoleText.setStyle("    -fx-font-size: 30;");
        StackPane.setAlignment(animatedConsoleText, Pos.BOTTOM_LEFT);
        animatedConsoleText.setTranslateY(-100);

        LOG.info("Building Scene Stack...");
        BorderPane bp = new BorderPane(centerStack);
        bp.setBackground(transBack);
        bp.getStyleClass().add("trinity-pane");
        Scene scene = new Scene(bp, Color.BLACK);
        stage.setScene(scene);

        LOG.info("Styling Scene and Stage...");
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
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = StyleResourceProvider.getResource("covalent.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        desktopPane = new Pane(); //transparent layer that just holds floating panes
        desktopPane.setPickOnBounds(false); //prevent it from blocking mouse clicks to sublayers
        //@HACK This ultra dirty hack lets me easily add floating panes from anywhere
        pathPane = desktopPane;
        theScene = scene;
        //</HACK>

        LOG.info("Building menu system...");
        mainNavMenu = new MainNavMenu(scene);
        StackPane.setAlignment(mainNavMenu, Pos.BOTTOM_RIGHT);
        mainNavMenu.hideRadialMenu();
        mainNavMenu.setTranslateX(-mainNavMenu.getInnerRadius());
        mainNavMenu.setTranslateY(-mainNavMenu.getInnerRadius());

        LOG.info("Parsing command line...");
        //animatedConsoleText.animate("Parsing command line...");
        parseCommandLine();
        //ex: --scenario="C:\dev\cameratests" --geometry=1024x768+100+100
        if (null != namedParameters) {
            LOG.info("Checking for geometry arguments...");
            if (namedParameters.containsKey("geometry")) {
                String geometryParamString = namedParameters.get("geometry");
                LOG.info("Attempting custom window geometry using {}", geometryParamString);
                try {
                    //example: 200x100+800+800
                    String[] tokens = geometryParamString.split("\\+");
                    String[] sizeTokens = tokens[0].split("x");
                    stage.setWidth(Double.parseDouble(sizeTokens[0]));
                    stage.setHeight(Double.parseDouble(sizeTokens[1]));
                    stage.setX(Double.parseDouble(tokens[1]));
                    stage.setY(Double.parseDouble(tokens[2]));
                } catch (NumberFormatException ex) {
                    LOG.info("Exception thrown parsing: {}. Setting to Maximized.", geometryParamString);
                    stage.setMaximized(true);
                }
            } else if (namedParameters.containsKey("fullscreen")) {
                LOG.info("Fullscreen start requested.");
                stage.setFullScreen(true);
            } else {
                LOG.info("Defaulting to maximized.");
                stage.setMaximized(true);
            }
            LOG.info("Checking for special effects requests...");
            boolean surveillanceEnabled = namedParameters.containsKey("surveillance");
            if (surveillanceEnabled) {
                LOG.info("Surveillance found... enabling Camera.");
            }

            if (namedParameters.containsKey("outrun")) {
                LOG.info("Outrun found... enabling RetroWavePane.");
                try {
                    //Add optional RETROWAVE VIEW
                    retroWavePane = new RetroWavePane(scene, surveillanceEnabled);
                    retroWavePane.setVisible(true);
                    centerStack.getChildren().add(retroWavePane);
                } catch (Exception ex) {
                    LOG.error(null, ex);
                }
            }
            if (namedParameters.containsKey("display4k")) {
                LOG.info("Display4k found... adjusting sizing.");
                is4k = true;
            }
            if (namedParameters.containsKey("matrix")) {
                LOG.info("Matrix found... enabling digital rain.");
                enableMatrix = true;
                matrixEnabled = enableMatrix;
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

        //add helper tools and overlays
        circleSpinner = new CircleProgressIndicator();
        circleSpinner.setLabelLater("...Working...");
        circleSpinner.defaultOpacity = 1.0;
        circleSpinner.setOpacity(0.0); ///instead of setVisible(false)

        centerStack.getChildren().addAll(desktopPane, 
            //mainNavMenu, 
            circleSpinner);
        centerStack.getChildren().add(animatedConsoleText);
        stage.show();        

        LOG.info("Setting up Matrix Digital Rain...");
        // fun matrix effect, use Alt + N
        matrixOverlay = new MatrixOverlay(scene, centerStack);
        bp.setOnSwipeUp(e -> {
            if (enableMatrix) {
                if (e.getTouchCount() > 2) { //three finger swipe
                    MatrixOverlay.on.set(false);
                    matrixOverlay.matrixOff.run();
                    e.consume(); // <-- stops passing the event to next node
                }
            }
        });
        bp.setOnSwipeDown(e -> {
            if (enableMatrix) {
                if (e.getTouchCount() > 2) { //three finger swipe
                    MatrixOverlay.on.set(true);
                    matrixOverlay.matrixOn.run();
                    e.consume(); // <-- stops passing the event to next node
                }
            }
        });
        LOG.info("Adding Event handlers... ");
        scene.addEventHandler(ApplicationEvent.SHOW_BUSY_INDICATOR, e -> {
            if (null == e.object) {
                return;
            }
            circleSpinner.updateStatus((ProgressStatus) e.object);
            circleSpinner.fadeBusy(false);
            circleSpinner.spin(true);
        });
        scene.addEventHandler(ApplicationEvent.HIDE_BUSY_INDICATOR, e -> {
            circleSpinner.spin(false);
            circleSpinner.fadeBusy(true);
        });
        scene.addEventHandler(ApplicationEvent.UPDATE_BUSY_INDICATOR, e -> {
            if (null == e.object) {
                return;
            }
            circleSpinner.updateStatus((ProgressStatus) e.object);
        });

        scene.addEventHandler(ApplicationEvent.SHUTDOWN, e -> shutdown(false));

        scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, covalentEvent -> {
            desktopPane.getChildren().remove(covalentEvent.pathPane);
        });

        LOG.info("Command Terminal ");
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
        scene.addEventHandler(CommandTerminalEvent.ALERT, e -> {
            animatedConsoleText.setAnimationTimeMS(15);  //default is 30ms
            animatedConsoleText.setFont(e.font);
            animatedConsoleText.setFill(e.color);
            animatedConsoleText.setStroke(e.color);
            animatedConsoleText.setText("> ");
            animatedConsoleText.setVisible(true);
            animatedConsoleText.setOpacity(1.0);
            animatedConsoleText.animate("> " + e.text);
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

        LOG.info("User Interface Lit...");
        AppAsyncManager async = new AppAsyncManager(scene, centerStack, desktopPane, circleSpinner, namedParameters);
        async.setOnSucceeded(s -> intro());
        async.setOnFailed(s -> intro());            
        Utils.printTotalTime(startTime);
        Thread.startVirtualThread(async);
    }
    private void intro() {
//        Platform.runLater(() -> {
            intro = new Timeline(
                new KeyFrame(Duration.seconds(0.0), new KeyValue(mainNavMenu.opacityProperty(), 0.0)),
                new KeyFrame(Duration.seconds(0.1), e -> {
                    circleSpinner.spin(false);
                    circleSpinner.fadeBusy(true);
                    animatedConsoleText.setOpacity(1.0);
                    animatedConsoleText.setText("> ");
                    animatedConsoleText.setVisible(true);
                    centerStack.getChildren().add(centerStack.getChildren().size() - 1, mainNavMenu);
                }),
                new KeyFrame(Duration.seconds(0.5), e -> animatedConsoleText.animate(">Trinity")),
                new KeyFrame(Duration.seconds(2.0), e -> animatedConsoleText.animate(">Hyperdimensional Visualization")),
                new KeyFrame(Duration.seconds(3.5), new KeyValue(animatedConsoleText.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(5.0), new KeyValue(animatedConsoleText.opacityProperty(), 0.0)),
                new KeyFrame(Duration.seconds(5.1), e -> animatedConsoleText.setVisible(false)),
                new KeyFrame(Duration.seconds(5.1), e -> animatedConsoleText.animate(" ")),
                new KeyFrame(Duration.seconds(5.1), new KeyValue(mainNavMenu.opacityProperty(), 1.0))
            );
            intro.play();
//        });
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
    /**
     * Key handler for various keyboard shortcuts
     */
    private void keyReleased(Stage stage, KeyEvent e) {
        //Enter/Exit fullscreen
        if (e.isAltDown() && e.getCode().equals(KeyCode.ENTER)) {
            stage.setFullScreen(!stage.isFullScreen());
        }
        //Terminate the app by entering an exit animation
        if (e.isControlDown() && e.isShiftDown()
            && e.getCode().equals(KeyCode.C)) {
            shutdown(false);
        }
        //@DEBUG SMP
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.A)) {
            stage.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_WAVEFORM_PANE));
        }
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.T)) {
            stage.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_TEXT_CONSOLE));
        }
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.V)) {
            stage.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_VIDEO_PANE,
                    "EMPTY VISION ", "A past never had for a Retrowave Future"));
        }
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.P)) {
            stage.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_PROJECTOR_PANE));
        }
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.I)) {
            stage.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_IMAGE_INSPECTION));
        }
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.H)) {
            stage.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_HYPERDRIVE_PANE));
        }
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.Q)) {
            System.out.println("Requesting REST Is Alive...");
            RestAccessLayer.requestRestIsAlive(stage.getScene());
        }
        if (e.isAltDown() && e.getCode().equals(KeyCode.N)) {
            matrixShowing = !matrixShowing;
            if (matrixShowing) {
                MatrixOverlay.on.set(true);
                matrixOverlay.matrixOn.run();
            } else {
                MatrixOverlay.on.set(false);
                matrixOverlay.matrixOff.run();                
                intro();
            }
        }
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Application Shutdown commenced...");
        shutdown(true);
    }

    private void shutdown(boolean now) {
        animatedConsoleText.getScene().getRoot().fireEvent(
            new ZeroMQEvent(ZeroMQEvent.ZEROMQ_TERMINATE_CONNECTION, null));        
//        if (null != feed) {
//            feed.disconnect(true); //if no connection does nothing
//        }
        animatedConsoleText.getScene().getRoot().fireEvent(
            new MissionTimerXEvent(MissionTimerXEvent.MISSION_TIMER_SHUTDOWN)); 
//        if (null != missionTimerX) {
//            missionTimerX.stop();
//        }
        if (now) {
            System.exit(0);
        } else {
            animatedConsoleText.setOpacity(0.0);
            animatedConsoleText.setVisible(true);
            animatedConsoleText.setText(">");
            if (null != intro) {
                intro.stop();
            }
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
//        unnamedParameters = parameters.getUnnamed();

        if (!namedParameters.isEmpty()) {
            LOG.info("NamedParameters :");
            namedParameters.entrySet().forEach(entry -> {
                LOG.info("\t{} : {}", entry.getKey(), entry.getValue());
            });
        }
//        if (!unnamedParameters.isEmpty()) {
//            LOG.info("UnnamedParameters :");
//            unnamedParameters.forEach(System.out::println);
//        }
    }

    //<editor-fold defaultstate="collapsed" desc="Static Accessors">

    /**
     * Dirty Hack to allow components to share a common configuration
     *
     * @return The current config
     */
    public static Configuration getConfig() {
        if (null == theConfig) {
            theConfig = Configuration.defaultConfiguration();
        }
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

    public static VideoPane getVideoPane() {
        return theVideo;
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
