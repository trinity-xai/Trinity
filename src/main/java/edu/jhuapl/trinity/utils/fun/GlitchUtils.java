/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.fun;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.FloatMap;
import javafx.util.Duration;

/**
 * @author Sean Phillips
 */
public enum GlitchUtils {
    INSTANCE;

    /**
     * Uses a DisplacementMap and a Timeline to temporarily cause any Node to
     * have a wavy sinusoidal effect.
     * The effect will kick in for a user set time and then return back to normal.
     * Suggested usage:
     * <p>
     * this.addEventHandler(MouseEvent.MOUSE_CLICKED, e-> {
     * GlitchUtils.gitchNode(this,
     * Duration.millis(250), Duration.ZERO, 2);
     * });
     *
     * @param node       The Node to make wavy
     * @param duration   how long to maintain the wavy effect
     * @param delay      how before the effect kicks in
     * @param cycleCount how many times to repeat the effect
     */
    public static void glitchNode(Node node, Duration duration, Duration delay, int cycleCount) {
        int width = 220;
        int height = 100;

        FloatMap floatMap = new FloatMap();
        floatMap.setWidth(width);
        floatMap.setHeight(height);

        for (int i = 0; i < width; i++) {
            double v = (Math.sin(i / 20.0 * Math.PI) - 0.5) / 40.0;
            for (int j = 0; j < height; j++) {
                floatMap.setSamples(i, j, 0.0f, (float) v);
            }
        }
        DisplacementMap map = new DisplacementMap(floatMap);
        //, width, width, width, width)

        Timeline t = new Timeline(
            new KeyFrame(Duration.millis(0), e -> node.setEffect(map)),
            new KeyFrame(duration, e -> node.setEffect(null)),
            new KeyFrame(duration.add(delay), e -> noOp())
        );
        t.setCycleCount(cycleCount);
        t.setAutoReverse(false);
        t.play();
    }

    private static void noOp() {
        int i = 2 + 2;
    }
}
