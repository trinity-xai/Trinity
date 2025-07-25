package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Text;
import javafx.util.Duration;
import lit.litfx.controls.covalent.PathPane;
import lit.litfx.controls.covalent.events.CovalentPaneEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Sean Phillips
 */
public class LitPathPane extends PathPane {
    private static final Logger LOG = LoggerFactory.getLogger(LitPathPane.class);
    Scene scene;
    Pane parent;
    boolean animating = false;
    boolean fadeEnabled = true;
    private boolean opaqueEnabled = false;
    double fadeSideInset = -5;
    double hoverTopInset = -2;
    double hoverSideInset = -3;
    double effectsFitWidth = 40;
    public Background opaqueBackground;
    public Background defaultBackground;
    public Background background;
    public Color fillPreStartColor = Color.CADETBLUE;
    public Color fillStartColor = Color.TRANSPARENT;
    public Color fillMiddleColor = Color.CYAN;
    public Color fillEndColor = Color.TRANSPARENT;
    public Color fillPostEndColor = Color.VIOLET;
    public LinearGradient lg;
    public Stop stop1, stop2, stop3;
    public SimpleDoubleProperty percentComplete = new SimpleDoubleProperty(0.0);
    private Timeline gradientTimeline;
    private double currentGradientMillis = 465; //This number was picked by Josh
    private long lastInsideMillis = 0;
    public static long enteredWaitTimeMillis = 5000;
    Label opaqueLabel, fadeLabel;
    Border activeBorder, hoverBorder;

    /**
     * Helper utility for loading a common FXML based Controller which assumes
     * an anchorpane node which is returned wrapped as a BorderPane
     *
     * @param controllerLocation The path to the FXML file to load. e.g.
     *                           "/com/somepackage/fxml/controller.fxml"
     * @return BorderPane the userContent
     */
    public static BorderPane createContent(String controllerLocation) {
        //make transparent so it doesn't interfere with subnode transparency effects
        Background transBack = new Background(new BackgroundFill(
            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
        FXMLLoader loader = new FXMLLoader(LitPathPane.class.getResource(controllerLocation));
        loader.setLocation(LitPathPane.class.getResource(controllerLocation));
        BorderPane sgRoot;
        try {
            AnchorPane anchorPane = loader.load();
            sgRoot = new BorderPane(anchorPane);
            sgRoot.setBackground(transBack);
        } catch (IOException ex) {
            sgRoot = new BorderPane(new Text("Unable to load FXML Controller: " + controllerLocation));
            LOG.error(null, ex);
        }
        return sgRoot;
    }

    public void slideInPane() {
        //https://stackoverflow.com/questions/48893282/javafx-apply-perspective-transformation-on-a-node-given-a-perspective-transform?noredirect=1&lq=1
        PerspectiveTransform pt = new PerspectiveTransform();
        pt.setUlx(getWidth());
        pt.setUly(getHeight() * 0.5);
        pt.setUrx(getWidth() + 1.0);
        pt.setUry(getHeight() * 0.5);

        pt.setLrx(getWidth() + 1.0);
        pt.setLry(getHeight() * 0.5);
        pt.setLlx(getWidth());
        pt.setLly(getHeight() * 0.5);
        setEffect(pt);

        Duration showPointDuration = Duration.seconds(0.75);
        Duration midPointDuration = Duration.seconds(0.75);
        Duration endPointDuration = Duration.seconds(1.00);

        Timeline timeline = new Timeline(
            new KeyFrame(showPointDuration, e -> show()),

            //animation to midpoint
            new KeyFrame(midPointDuration, new KeyValue(pt.ulxProperty(), getWidth() * 0.75)),
            new KeyFrame(midPointDuration, new KeyValue(pt.ulyProperty(), 0.0)),
            new KeyFrame(midPointDuration, new KeyValue(pt.urxProperty(), getWidth())),
            new KeyFrame(midPointDuration, new KeyValue(pt.uryProperty(), getHeight() * 0.333)),

            new KeyFrame(midPointDuration, new KeyValue(pt.lrxProperty(), getWidth())),
            new KeyFrame(midPointDuration, new KeyValue(pt.lryProperty(), getHeight() * 0.666)),
            new KeyFrame(midPointDuration, new KeyValue(pt.llxProperty(), getWidth() * 0.75)),
            new KeyFrame(midPointDuration, new KeyValue(pt.llyProperty(), getHeight())),

            //animation to actual size
            new KeyFrame(endPointDuration, new KeyValue(pt.ulxProperty(), 0.0)),
            new KeyFrame(endPointDuration, new KeyValue(pt.ulyProperty(), 0.0)),
            new KeyFrame(endPointDuration, new KeyValue(pt.urxProperty(), getWidth())),
            new KeyFrame(endPointDuration, new KeyValue(pt.uryProperty(), 0.0)),

            new KeyFrame(endPointDuration, new KeyValue(pt.lrxProperty(), getWidth())),
            new KeyFrame(endPointDuration, new KeyValue(pt.lryProperty(), getHeight())),
            new KeyFrame(endPointDuration, new KeyValue(pt.llxProperty(), 0.0)),
            new KeyFrame(endPointDuration, new KeyValue(pt.llyProperty(), getHeight()))
        );
        animating = true;
        timeline.play();
        timeline.setOnFinished(e -> {
            animating = false;
            setEffect(null);
        });
    }

    public LitPathPane(Scene scene, Pane parent, int width, int height, Pane userContent, String mainTitleText, String mainTitleText2, double borderTimeMs, double contentTimeMs) {
        super(scene, parent, width, height, userContent, mainTitleText, mainTitleText2, borderTimeMs, contentTimeMs);
        this.scene = scene;
        this.parent = parent;
        // must be set to prevent user from resizing too small.
        setMinWidth(300);
        setMinHeight(200);
        setEffects();
        mainContentBorderFrame.widthProperty().addListener(cl -> {
            if (!animating) {
                contentPane.setPrefWidth(mainContentBorderFrame.getWidth() - 100);
            }
        });
        mainContentBorderFrame.heightProperty().addListener(cl -> {
            if (!animating) {
                contentPane.setPrefHeight(mainContentBorderFrame.getHeight() - 100);
            }
        });
        opaqueBackground = new Background(new BackgroundFill(
            Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY));
        defaultBackground = mainContentBorderFrame.getBackground();
    }

    public void setEffects() {
        ImageView fadeImageView = ResourceUtils.loadIcon("fade", effectsFitWidth);
        fadeLabel = new Label("Fadeout", fadeImageView);
        fadeLabel.setContentDisplay(ContentDisplay.TOP);

        ImageView opaqueImageView = ResourceUtils.loadIcon("opaque", effectsFitWidth);
        opaqueLabel = new Label("Opaque", opaqueImageView);
        opaqueLabel.setContentDisplay(ContentDisplay.TOP);
        activeBorder = new Border(new BorderStroke(
            Color.CYAN, BorderStrokeStyle.DOTTED,
            CornerRadii.EMPTY, new BorderWidths(1), new Insets(0, fadeSideInset, 0, fadeSideInset))
        );
        hoverBorder = new Border(new BorderStroke(
            Color.WHITE, BorderStrokeStyle.SOLID,
            CornerRadii.EMPTY, new BorderWidths(1),
            new Insets(hoverTopInset, hoverSideInset, hoverTopInset, hoverSideInset))
        );

        HBox effectsHBox = new HBox(20, fadeLabel, opaqueLabel);

        AnchorPane.setBottomAnchor(effectsHBox, -24.0);
        AnchorPane.setRightAnchor(effectsHBox, 40.0);
        this.mainContentBorderFrame.getChildren().add(effectsHBox);

        Glow glow = new Glow(0.9);
        background = new Background(new BackgroundFill(
            Color.CYAN.deriveColor(1, 1, 1, 0.1),
            CornerRadii.EMPTY, new Insets(0, fadeSideInset, 0, fadeSideInset)));

        fadeLabel.setOnMouseEntered(e -> fadeLabel.setBorder(hoverBorder));
        fadeLabel.setOnMouseExited(e -> {
            if (fadeEnabled)
                fadeLabel.setBorder(activeBorder);
            else
                fadeLabel.setBorder(null);
        });
        fadeLabel.setEffect(glow);
        fadeLabel.setBackground(background);
        fadeLabel.setOnMouseClicked(e -> {
            fadeEnabled = !fadeEnabled;
            if (fadeEnabled) {
                fadeLabel.setEffect(glow);
                fadeLabel.setBackground(background);
                fadeLabel.setBorder(activeBorder);
            } else {
                fadeLabel.setEffect(null);
                fadeLabel.setBackground(null);
                fadeLabel.setBorder(null);
            }
        });

        opaqueLabel.setOnMouseEntered(e -> opaqueLabel.setBorder(hoverBorder));
        opaqueLabel.setOnMouseExited(e -> {
            if (isOpaqueEnabled())
                opaqueLabel.setBorder(activeBorder);
            else
                opaqueLabel.setBorder(null);
        });
        opaqueLabel.setEffect(glow);
        opaqueLabel.setOnMouseClicked(e -> {
            setOpaqueEnabled(!isOpaqueEnabled());
        });

        this.scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, e -> {
            if (e.pathPane == this)
                parent.getChildren().remove(this);
        });
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> this.toFront());
        gradientTimeline = setupGradientTimeline();
        //transparency fade effects...
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (fadeEnabled) {
                fade(100, 0.8);
                if (System.currentTimeMillis() - lastInsideMillis > enteredWaitTimeMillis) {
                    gradientTimeline.setCycleCount(1);
                    gradientTimeline.setAutoReverse(false);
                    gradientTimeline.playFromStart();
                }
            } else
                contentPane.setOpacity(0.8);
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            lastInsideMillis = System.currentTimeMillis();
            if (fadeEnabled) {
                fade(100, 0.3);
                this.outerFrame.setFill(Color.TRANSPARENT);
            } else
                contentPane.setOpacity(0.8);
        });
    }

    public Timeline setupGradientTimeline() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(30), new KeyValue(percentComplete, 0.0)),
            new KeyFrame(Duration.millis(currentGradientMillis), new KeyValue(percentComplete, 1.0))
        );
        timeline.setOnFinished(e -> {
            percentComplete.set(0);
            setGradientByComplete(); //will reset the gradient but not set fill
            outerFrame.setFill(Color.TRANSPARENT);
        });
        percentComplete.addListener(l -> updateGradient());
        return timeline;
    }

    public void setGradientByComplete() {
        Stop preStopClear = new Stop(percentComplete.get() - 0.15, Color.TRANSPARENT);
        Stop preStop = new Stop(percentComplete.get() - 0.1, fillPreStartColor);
        stop1 = new Stop(percentComplete.get() - 0.01, fillStartColor);
        stop2 = new Stop(percentComplete.get(), fillMiddleColor);
        stop3 = new Stop(percentComplete.get() + 0.01, fillEndColor);
        Stop postStop = new Stop(percentComplete.get() + 0.1, fillPostEndColor);
        Stop postStopClear = new Stop(percentComplete.get() + 0.15, Color.TRANSPARENT);
        ArrayList<Stop> stops = new ArrayList<>();
        stops.add(preStopClear);
        stops.add(preStop);
        stops.add(stop1);
        stops.add(stop2);
        stops.add(stop3);
        stops.add(postStop);
        stops.add(postStopClear);
        lg = new LinearGradient(
            0.5, 1.0, 0.5, 0.0, true, CycleMethod.NO_CYCLE, stops);
    }

    public void updateGradient() {
        setGradientByComplete();
        this.outerFrame.setFill(lg);
    }

    public void fade(double timeMS, double toValue) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeMS), contentPane);
        fadeTransition.setToValue(toValue);
        fadeTransition.setOnFinished(e -> contentPane.setOpacity(toValue));
        fadeTransition.play();
    }

    /**
     * @return the opaqueEnabled
     */
    public boolean isOpaqueEnabled() {
        return opaqueEnabled;
    }

    /**
     * @param opaqueEnabled the opaqueEnabled to set
     */
    public void setOpaqueEnabled(boolean opaqueEnabled) {
        this.opaqueEnabled = opaqueEnabled;
        if (isOpaqueEnabled()) {
            Glow glow = new Glow(0.9);
            opaqueLabel.setEffect(glow);
            opaqueLabel.setBackground(background);
            opaqueLabel.setBorder(activeBorder);
            mainContentBorderFrame.setBackground(opaqueBackground);
        } else {
            opaqueLabel.setEffect(null);
            opaqueLabel.setBackground(null);
            opaqueLabel.setBorder(null);
            mainContentBorderFrame.setBackground(defaultBackground);
        }
    }
}
