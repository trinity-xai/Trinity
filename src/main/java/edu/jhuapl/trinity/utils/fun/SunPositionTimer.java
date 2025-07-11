package edu.jhuapl.trinity.utils.fun;

import edu.jhuapl.trinity.javafx.events.EffectEvent;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

/**
 *
 * @author Sean Phillips
 */
public class SunPositionTimer extends AnimationTimer {

    private final DoubleProperty arcWidth = new SimpleDoubleProperty(0.5);
    private final DoubleProperty arcHeight = new SimpleDoubleProperty(0.4);
    private final DoubleProperty velocityHz = new SimpleDoubleProperty(0.01); // Cycles/sec
    private ObjectProperty<SunPathMode> pathMode = new SimpleObjectProperty<>(SunPathMode.ELLIPTICAL);

    private double animationStartTime = -1;
    private boolean animating = false;
    private boolean enabled = false;
    private Circle sun;
    Pane sunPane; 

    public SunPositionTimer(Pane sunPane, Circle sun) {
        super();
        this.sunPane = sunPane;
        this.sun = sun;

        sunPane.getScene().getRoot().addEventHandler(EffectEvent.SUN_POSITION_ANIMATING, e -> {
            animating = (boolean) e.object;
        });
        sunPane.getScene().getRoot().addEventHandler(EffectEvent.SUN_POSITION_ARCWIDTH, e -> {
            arcWidth.setValue((double) e.object);
        });
        sunPane.getScene().getRoot().addEventHandler(EffectEvent.SUN_POSITION_ARCHEIGHT, e -> {
            arcHeight.setValue((double) e.object);
        });
        sunPane.getScene().getRoot().addEventHandler(EffectEvent.SUN_POSITION_VELOCITY, e -> {
            velocityHz.setValue((double) e.object);
        });
        sunPane.getScene().getRoot().addEventHandler(EffectEvent.SUN_POSITION_PATHMODE, e -> {
            setPathMode((SunPathMode) e.object);
        });
    }

    @Override
    public void handle(long nowNanos) {
        if (!animating) {
            return;
        }

        if (animationStartTime < 0) {
            animationStartTime = nowNanos / 1_000_000_000.0;
        }

        double timeSec = nowNanos / 1_000_000_000.0 - animationStartTime;
        double freq = velocityHz.get();
        //        double angle = 2 * Math.PI * freq * timeSec;
        double t = freq * timeSec;       // normalized cycle progress

        double paneW = sunPane.getWidth();
        double paneH = sunPane.getHeight();

        double arcW = arcWidth.get();
        double arcH = arcHeight.get();

        Point2D offset = pathMode.get().computePosition(t, paneW, paneH, arcW, arcH);

        sun.setTranslateX(paneW / 2.0 + offset.getX());
        sun.setTranslateY(paneH / 2.0 + offset.getY());
    }

    public void setPathMode(SunPathMode mode) {
        this.pathMode.set(mode);
    }
}
