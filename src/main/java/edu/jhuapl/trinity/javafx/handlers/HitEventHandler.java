package edu.jhuapl.trinity.javafx.handlers;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2024 Sean Phillips
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

import edu.jhuapl.trinity.javafx.events.HitEvent;
import edu.jhuapl.trinity.javafx.javafx3d.projectiles.HitBox;
import edu.jhuapl.trinity.javafx.javafx3d.projectiles.HitShape3D;
import edu.jhuapl.trinity.javafx.javafx3d.projectiles.Projectile;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.geometry.VPos;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import lit.litfx.controls.output.AnimatedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * @author Sean Phillips
 */
public class HitEventHandler implements EventHandler<HitEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(HitEventHandler.class);
    private static final Interpolator INTERPOLATOR =
        Interpolator.SPLINE(0.295, 0.800, 0.305, 1.000);
    private static final Random RANDOM = new Random();
    private static HashMap<String, HitBox> hitBoxMap = new HashMap<>();
    Media smallBoom = null;
    MediaPlayer smallBoomMediaPlayer = null;
    Media bigBoom = null;
    MediaPlayer bigBoomMediaPlayer = null;
    Media death = null;
    MediaPlayer deathMediaPlayer = null;
    public Pane hudPane;
    AnimatedText scoreText;
    AnimatedText playersText;
    int score = 0;
    int players = 4;
    Color hudTextColor;

    public HitEventHandler(Pane hudPane) {
        this.hudPane = hudPane;
        hudTextColor = Color.TOMATO
            .deriveColor(1, 1, 1, 0.5);
        scoreText = new AnimatedText("000000",
            Font.font("Consolas", 72),
            hudTextColor, AnimatedText.ANIMATION_STYLE.TYPED);
        scoreText.setStyle("    -fx-font-size: 72;");
        scoreText.setFill(hudTextColor);
        scoreText.setStroke(hudTextColor);
        scoreText.setTextOrigin(VPos.CENTER);
        scoreText.setMouseTransparent(true);
        scoreText.setTranslateX(100);
        scoreText.setTranslateY(50);
        scoreText.setAnimationTimeMS(15);
        scoreText.setVisible(false); //hide at start
        hudPane.getChildren().add(scoreText);

        playersText = new AnimatedText("04",
            Font.font("Consolas", 72),
            hudTextColor, AnimatedText.ANIMATION_STYLE.TYPED);
        playersText.setStyle("    -fx-font-size: 72;");
        playersText.setFill(hudTextColor);
        playersText.setStroke(hudTextColor);
        playersText.setTextOrigin(VPos.CENTER);
        playersText.setMouseTransparent(true);
        playersText.setTranslateX(scoreText.getBoundsInLocal().getMaxX() + 100);
        playersText.setTranslateY(50);
        playersText.setVisible(false); //hide at start
        hudPane.getChildren().add(playersText);

        try {
            smallBoom = ResourceUtils.loadMediaWav("smallBoom");
            smallBoomMediaPlayer = new MediaPlayer(smallBoom);
            bigBoom = ResourceUtils.loadMediaWav("bigBoom");
            bigBoomMediaPlayer = new MediaPlayer(bigBoom);
            death = ResourceUtils.loadMediaWav("death");
            deathMediaPlayer = new MediaPlayer(death);
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
    }

    @Override
    public void handle(HitEvent event) {
        if (event.getEventType().equals(HitEvent.TRACKING_PROJECTILE_EVENTS)) {
            boolean tracking = (boolean) event.object1;
            scoreText.setFill(hudTextColor);
            scoreText.setStroke(hudTextColor);
            scoreText.setTranslateX(hudPane.getWidth() / 4.0 - scoreText.getBoundsInLocal().getWidth());
            if (scoreText.getTranslateX() < 0)
                scoreText.setTranslateX(100); //hacky safety check
            scoreText.setTranslateY(50);
            scoreText.setVisible(tracking);

            playersText.setFill(hudTextColor);
            playersText.setStroke(hudTextColor);
            playersText.setTranslateX(hudPane.getWidth() - hudPane.getWidth() / 4.0
                - playersText.getBoundsInLocal().getWidth());
            if (playersText.getTranslateX() < 0)
                playersText.setTranslateX(scoreText.getBoundsInLocal().getMaxX() + 100); //hacky safety check
            playersText.setVisible(tracking);

        }
        if (event.getEventType().equals(HitEvent.PROJECTILE_HIT_BOX)) {
            checkType((Projectile) event.object1, (HitBox) event.object2);
        }
        if (event.getEventType().equals(HitEvent.PROJECTILE_HIT_SHIELD)) {
            playBounce();
        }
        if (event.getEventType().equals(HitEvent.PROJECTILE_HIT_BRICK)) {
            playBreak();
        }
        if (event.getEventType().equals(HitEvent.PROJECTILE_HIT_CHARACTER)) {
            playDeath();
        }
        if (event.getEventType().equals(HitEvent.PROJECTILE_HIT_SHAPE)) {
            HitShape3D hit = (HitShape3D) event.object2;
            //Will we flip?
            if (score + hit.getPoints().size() > 999999)
                score = 999999 + hit.getPoints().size() - 999999;
            else
                score += hit.getPoints().size();
            scoreText.animate(String.format("%06d", score));
            Point3D coordinates = hit.localToScene(
                Point3D.ZERO, true);
            createFadeoutText(
                String.valueOf(hit.getPoints().size()), coordinates);
        }
    }

    private void checkType(Projectile p, HitBox h) {
//        if(h instanceof BrickHitBox){
//            playBreak();
//        } else if(h instanceof ShieldHitBox){
//            playBounce();
//        } //@TODO SMP character death
    }

    private static float getRandom(double min, double max) {
        return (float) (RANDOM.nextFloat() * (max - min) + min);
    }

    private void createFadeoutText(String c, Point3D sceneLocation) {
        final Text letter = new Text(c);
        letter.setFill(Color.FIREBRICK.deriveColor(1, 1, 1, 0.8));
        letter.setFont(Font.font("Consolas"));
        letter.setTextOrigin(VPos.CENTER);
        letter.setMouseTransparent(true);
        letter.setTranslateX(sceneLocation.getX());
        letter.setTranslateY(sceneLocation.getY());
        hudPane.getChildren().add(letter);
        // over 5 seconds move letter to random position and fade it out
        final Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.seconds(5), event -> {
                // we are done remove us from scene
                hudPane.getChildren().remove(letter);
            },
                new KeyValue(letter.scaleXProperty(), 5.0, INTERPOLATOR),
                new KeyValue(letter.scaleYProperty(), 5.0, INTERPOLATOR),
                new KeyValue(letter.translateXProperty(),
                    getRandom(0.0f, hudPane.getWidth() / 2.0 - letter.getBoundsInLocal().getWidth()),
                    INTERPOLATOR),
                new KeyValue(letter.translateYProperty(),
                    getRandom(0.0f, hudPane.getHeight() / 2.0 - letter.getBoundsInLocal().getHeight()),
                    INTERPOLATOR),
                new KeyValue(letter.opacityProperty(), 0f)
            ));
        timeline.play();
    }

    private void playBounce() {
        if (null != smallBoomMediaPlayer) {
            smallBoomMediaPlayer.stop();
            smallBoomMediaPlayer.seek(smallBoomMediaPlayer.getStartTime());
            smallBoomMediaPlayer.play();
        }
    }

    private void playBreak() {
        if (null != bigBoomMediaPlayer) {
            bigBoomMediaPlayer.stop();
            bigBoomMediaPlayer.seek(bigBoomMediaPlayer.getStartTime());
            bigBoomMediaPlayer.play();
        }
    }

    private void playDeath() {
        if (null != deathMediaPlayer) {
            deathMediaPlayer.stop();
            deathMediaPlayer.seek(deathMediaPlayer.getStartTime());
            deathMediaPlayer.play();
        }
    }
}
