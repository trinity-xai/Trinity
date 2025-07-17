package edu.jhuapl.trinity;

import edu.jhuapl.trinity.audio.JukeBox;
import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.javafx.components.panes.VideoPane;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.javafx.components.radial.MainNavMenu;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.AudioEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.EffectEvent;
import edu.jhuapl.trinity.javafx.events.FullscreenEvent;
import edu.jhuapl.trinity.javafx.events.MissionTimerXEvent;
import edu.jhuapl.trinity.javafx.events.ZeroMQEvent;
import edu.jhuapl.trinity.utils.Configuration;
import edu.jhuapl.trinity.utils.fun.Glitch;
import edu.jhuapl.trinity.utils.fun.Pixelate;
import edu.jhuapl.trinity.utils.fun.Pixelate.PixelationMode;
import edu.jhuapl.trinity.utils.fun.VHSScanline;
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

public class App extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    static Configuration theConfig;
    static Pane pathPane;
    static Scene theScene;
    static VideoPane theVideo = null;
    static boolean matrixEnabled = false;
    boolean matrixShowing = false;

    Pane desktopPane;
    StackPane centerStack;
    MainNavMenu mainNavMenu;
    //make transparent so it doesn't interfere with subnode transparency effects
    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
    Timeline intro;
    CircleProgressIndicator circleSpinner;
    AnimatedText animatedConsoleText;
    Pixelate pixelate;
    Glitch glitch;
    VHSScanline vhsscanline;
    
    @Override
    public void start(Stage stage) throws IOException {
        //@DEBUG SMP load time
        //System.out.println("App.start() begin... ");
        //long startTime = System.nanoTime();
        LOG.info("Starting JavaFX rendering...");
        centerStack = new StackPane();
        centerStack.setBackground(transBack);
        LOG.info("Building Scene Stack...");
        BorderPane bp = new BorderPane(centerStack);
        bp.setBackground(transBack);
        bp.getStyleClass().add("trinity-pane");
        Scene scene = new Scene(bp, 1920, 1080, Color.BLACK);
        stage.setScene(scene);
        LOG.info("Styling Scene and Stage...");
        stage.setTitle("Trinity XAI");
        //Set icon for stage for fun
        stage.getIcons().add(new Image(getClass().getResource("icons/stageicon.png").toExternalForm()));
        //disable the default fullscreen exit mechanism
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        //Setup a custom key handlers
        stage.setFullScreenExitHint("Press ALT+ENTER to alternate Fullscreen Mode.");
        stage.setMaximized(true); //default... but could be overridden later by commandline params
        //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = StyleResourceProvider.getResource("covalent.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        stage.show();

        //add just the dark necessaties...
        JukeBox jukeBox = new JukeBox(scene);
        scene.addEventHandler(AudioEvent.PLAY_MUSIC_TRACK, jukeBox);
        scene.addEventHandler(AudioEvent.RELOAD_MUSIC_FILES, jukeBox);
        scene.addEventHandler(AudioEvent.MUSIC_FILES_RELOADED, jukeBox);
        scene.addEventHandler(AudioEvent.ENABLE_MUSIC_TRACKS, jukeBox);
        scene.addEventHandler(AudioEvent.SET_MUSIC_VOLUME, jukeBox);
        scene.addEventHandler(AudioEvent.ENABLE_FADE_TRACKS, jukeBox);
        scene.addEventHandler(AudioEvent.CYCLE_MUSIC_TRACKS, jukeBox);
        scene.addEventHandler(AudioEvent.CURRENTLY_PLAYING_TRACK, jukeBox);

        desktopPane = new Pane(); //transparent layer that just holds floating panes
        desktopPane.setPickOnBounds(false); //prevent it from blocking mouse clicks to sublayers
        //@HACK This ultra dirty hack lets me easily add floating panes from anywhere
        pathPane = desktopPane;
        theScene = scene;
        //</HACK>
        Font font = new Font("Consolas", 30);
        animatedConsoleText = new AnimatedText(" ", font, Color.GREEN, AnimatedText.ANIMATION_STYLE.TYPED);
        animatedConsoleText.setStyle("    -fx-font-size: 30;");
        StackPane.setAlignment(animatedConsoleText, Pos.BOTTOM_LEFT);
        animatedConsoleText.setTranslateY(-100);
        circleSpinner = new CircleProgressIndicator();
        circleSpinner.setLabelLater("...Working...");
        circleSpinner.defaultOpacity = 1.0;
        circleSpinner.setOpacity(0.0); ///instead of setVisible(false)
        centerStack.getChildren().addAll(desktopPane, circleSpinner, animatedConsoleText);

        LOG.info("Adding Application Event handlers... ");
        scene.addEventHandler(KeyEvent.KEY_RELEASED, e -> keyReleased(stage, e));
        scene.addEventHandler(FullscreenEvent.SET_FULLSCREEN, e -> stage.setFullScreen(e.setFullscreen));
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

        LOG.info("Building menu system...");
        mainNavMenu = new MainNavMenu(scene);
        StackPane.setAlignment(mainNavMenu, Pos.BOTTOM_RIGHT);
        mainNavMenu.hideRadialMenu();
        mainNavMenu.setTranslateX(-mainNavMenu.getInnerRadius());
        mainNavMenu.setTranslateY(-mainNavMenu.getInnerRadius());

        LOG.info("User Interface Lit...");
        AppAsyncManager async = new AppAsyncManager(scene, centerStack, desktopPane,
            circleSpinner, getParameters().getNamed());
        async.setOnSucceeded(s -> intro());
        async.setOnFailed(s -> intro());
        //@DEBUG SMP load time
        //Utils.printTotalTime(startTime);
        Thread.startVirtualThread(async);
        pixelate = new Pixelate(
            bp,
            Double.valueOf(scene.getWidth()).intValue(),
            Double.valueOf(scene.getHeight()).intValue(),
            16,
            true,
            2000
        );
        pixelate.setPixelateTime(800);
        pixelate.setMode(PixelationMode.RANDOM_BLOCKS);
        pixelate.setCenterOnY(false);
        pixelate.setBlockCount(20);
        pixelate.setBlockSizeRange(10, 50);

    }

    private void intro() {
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
            new KeyFrame(Duration.seconds(5.1), new KeyValue(mainNavMenu.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(5.1), e -> {
                centerStack.getChildren().add(pixelate.getCanvas());
                //pixelate.start();
            })    
        );
        intro.play();
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
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.Q)) {
            shutdown(false);
        }
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.S)) {
            stage.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_SPECIALEFFECTS_PANE));
        }
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.C)) {
            stage.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_COCOVIEWER_PANE));
        }
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
                new ApplicationEvent(ApplicationEvent.SHOW_IMAGE_INSPECTION, true));
        }
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.H)) {
            stage.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_HYPERDRIVE_PANE));
        }
        if (e.isAltDown() && e.isControlDown() && e.getCode().equals(KeyCode.M)) {
            stage.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_JUKEBOX_PANE));
        }
        if (e.isAltDown() && e.getCode().equals(KeyCode.N)) {
            matrixShowing = !matrixShowing;
            if (matrixShowing) {
                stage.getScene().getRoot().fireEvent(
                    new EffectEvent(EffectEvent.START_DIGITAL_RAIN));
            } else {
                stage.getScene().getRoot().fireEvent(
                    new EffectEvent(EffectEvent.STOP_DIGITAL_RAIN));
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
            pixelate.stop();
            pixelate.setMode(PixelationMode.FULL_SURFACE);

        animatedConsoleText.getScene().getRoot().fireEvent(
            new ZeroMQEvent(ZeroMQEvent.ZEROMQ_TERMINATE_CONNECTION, null));
        animatedConsoleText.getScene().getRoot().fireEvent(
            new MissionTimerXEvent(MissionTimerXEvent.MISSION_TIMER_SHUTDOWN));
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
                new KeyFrame(Duration.seconds(1.0), kv -> pixelate.animatePixelSize(8, 100, 2000, false, false)),   
//                new KeyFrame(Duration.seconds(2.5), kv -> new KeyValue(centerStack.opacityProperty(), 1.0)),       
//                new KeyFrame(Duration.seconds(3.1), kv -> new KeyValue(centerStack.opacityProperty(), 0.0)),       
                new KeyFrame(Duration.seconds(3.25), kv -> System.exit(0)));
            outtro.play();
        }
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
