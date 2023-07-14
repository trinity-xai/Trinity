package edu.jhuapl.trinity.javafx.components.radial;

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

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * @author Sean Phillips
 */
public class AnimatedNeonCircle extends Circle {

    private final RotateTransition transition;

    public AnimatedNeonCircle(AnimatedNeonCircle.Animation a, double radius, double strokeWidth, Double... dashedArray) {
        this.transition = new RotateTransition(a.durationProperty().get(), AnimatedNeonCircle.this);
        this.transition.setCycleCount(a.cycleCountProperty().get());
        this.transition.setAutoReverse(a.autoReverseProperty().get());
        this.transition.setByAngle(360);
        this.transition.setInterpolator(Interpolator.EASE_BOTH);
        super.setFill(Color.TRANSPARENT);
        super.setStroke(Color.CYAN);
        super.setStrokeWidth(strokeWidth);
        super.setEffect(new Glow(0.9));
        super.setRadius(radius);
        super.getStrokeDashArray().setAll(dashedArray);
    }

    public void play(boolean play) {
        if (play)
            transition.play();
        else
            transition.stop();
    }

    // =======================================================================
    public static class Animation {

        private final SimpleObjectProperty<Duration> duration = new SimpleObjectProperty<>();
        private final SimpleIntegerProperty cycleCount = new SimpleIntegerProperty();
        private final SimpleBooleanProperty autoReverse = new SimpleBooleanProperty();

        public Animation(Duration duration, int cycleCount, boolean autoReverse) {
            this.duration.set(duration);
            this.cycleCount.set(cycleCount);
            this.autoReverse.set(autoReverse);
        }

        public SimpleObjectProperty<Duration> durationProperty() {
            return duration;
        }

        public SimpleIntegerProperty cycleCountProperty() {
            return cycleCount;
        }

        public SimpleBooleanProperty autoReverseProperty() {
            return autoReverse;
        }
    }
}
