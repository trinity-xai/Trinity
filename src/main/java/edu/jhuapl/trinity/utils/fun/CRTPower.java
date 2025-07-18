package edu.jhuapl.trinity.utils.fun;

/**
 *
 * @author Sean Phillips
 */
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

public class CRTPower {

    private final Node target;
    private final Scale scaleTransform = new Scale(1, 1, 0.5, 0.5);
    private final Glow glow = new Glow(0.0);
    private final Duration duration;
    private final boolean withGlow;
    private final boolean withScanline;

    private CRTPower(Node target, Duration duration, boolean withGlow, boolean withScanline) {
        this.target = target;
        this.duration = duration;
        this.withGlow = withGlow;
        this.withScanline = withScanline;

        target.getTransforms().add(scaleTransform);

        if (withGlow) {
            target.setEffect(glow);
        }
    }

    public void play(boolean powerOn) {
        double fromScale = powerOn ? 0.0 : 1.0;
        double toScale = powerOn ? 1.0 : 0.0;
        double fromOpacity = powerOn ? 0.0 : 1.0;
        double toOpacity = powerOn ? 1.0 : 0.0;
        double fromGlow = powerOn ? 1.0 : 0.2;
        double toGlow = powerOn ? 0.2 : 1.0;

        Timeline animation = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(scaleTransform.yProperty(), fromScale),
                new KeyValue(target.opacityProperty(), fromOpacity),
                new KeyValue(glow.levelProperty(), withGlow ? fromGlow : 0.0)
            ),
            new KeyFrame(duration,
                new KeyValue(scaleTransform.yProperty(), toScale),
                new KeyValue(target.opacityProperty(), toOpacity),
                new KeyValue(glow.levelProperty(), withGlow ? toGlow : 0.0)
            )
        );

        animation.setOnFinished(e -> {
            if (!powerOn) {
                target.setVisible(false);
            }
        });

        target.setVisible(true);
        animation.play();

        if (withScanline && powerOn) {
            playScanlineFlash();
        }
    }

    private void playScanlineFlash() {
        Node parent = target.getParent();
        if (!(parent instanceof Pane)) return;

        Pane parentPane = (Pane) parent;

        Rectangle scanline = new Rectangle();
        scanline.setFill(Color.WHITE);
        scanline.setOpacity(0);
        scanline.setHeight(2);

        double targetTop = target.getLayoutY();
        double targetHeight = target.getBoundsInParent().getHeight();
        scanline.setY(targetTop + targetHeight / 2 - 1);
        scanline.setX(0);
        scanline.setWidth(parentPane.getWidth());

        parentPane.getChildren().add(scanline);

        Timeline flash = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(scanline.opacityProperty(), 0.9)),
            new KeyFrame(Duration.millis(500), new KeyValue(scanline.opacityProperty(), 0.0))
        );

        flash.setOnFinished(e -> parentPane.getChildren().remove(scanline));
        flash.play();
    }

    // ──────────────────────────────────────────────
    // Builder Support
    // ──────────────────────────────────────────────

    public static class Builder {
        private final Node target;
        private Duration duration = Duration.millis(600);
        private boolean withGlow = true;
        private boolean withScanline = true;

        public Builder(Node target) {
            this.target = target;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder enableGlow(boolean enabled) {
            this.withGlow = enabled;
            return this;
        }

        public Builder enableScanline(boolean enabled) {
            this.withScanline = enabled;
            return this;
        }

        public CRTPower build() {
            return new CRTPower(target, duration, withGlow, withScanline);
        }
    }
}

