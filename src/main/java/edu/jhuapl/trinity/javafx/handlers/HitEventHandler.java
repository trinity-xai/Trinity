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
import edu.jhuapl.trinity.javafx.javafx3d.projectiles.Projectile;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.event.EventHandler;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class HitEventHandler implements EventHandler<HitEvent> {

    private static HashMap<String, HitBox> hitBoxMap = new HashMap<>();
    Media smallBoom = null;
    MediaPlayer smallBoomMediaPlayer = null;
    Media bigBoom = null;
    MediaPlayer bigBoomMediaPlayer = null;
    Media death = null;
    MediaPlayer deathMediaPlayer = null;

    public HitEventHandler() {

        try {
            smallBoom = ResourceUtils.loadMediaWav("smallBoom");
            smallBoomMediaPlayer = new MediaPlayer(smallBoom);
            bigBoom = ResourceUtils.loadMediaWav("bigBoom");
            bigBoomMediaPlayer = new MediaPlayer(bigBoom);
            death = ResourceUtils.loadMediaWav("death");
            deathMediaPlayer = new MediaPlayer(death);
        } catch (IOException ex) {
            Logger.getLogger(HitEventHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void checkType(Projectile p, HitBox h) {
//        if(h instanceof BrickHitBox){
//            playBreak();
//        } else if(h instanceof ShieldHitBox){
//            playBounce();
//        } //@TODO SMP character death
    }

    @Override
    public void handle(HitEvent event) {
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
            playBreak();
        }        
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
