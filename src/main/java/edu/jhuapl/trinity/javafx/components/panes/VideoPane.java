package edu.jhuapl.trinity.javafx.components.panes;

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

import edu.jhuapl.trinity.utils.ResourceUtils;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Scene;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/**
 * @author Sean Phillips
 */
public class VideoPane extends LitPathPane {
    BorderPane bp;
    public static int PANE_WIDTH = 1000;
    public static double NODE_WIDTH = PANE_WIDTH - 50;
    public static double NODE_HEIGHT = NODE_WIDTH / 2.0;
    Media media;
    MediaView mediaView;
    public MediaPlayer mediaPlayer;
    
    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        MediaView mediaView = new MediaView();
        Glow glow = new Glow(0.95);
        mediaView.setEffect(glow);
        bpOilSpill.setCenter(mediaView);
       return bpOilSpill;
    }

    public VideoPane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, 750, createContent(),
            "Video Player", "", 300.0, 400.0);
        this.scene = scene;
        bp = (BorderPane) this.contentPane;
        mediaView = (MediaView) bp.getCenter();
        mediaView.setPreserveRatio(true);
        mediaView.setFitWidth(NODE_WIDTH);
        mediaView.setOnMouseClicked(e -> {
            shutdown();
        });
        getChildren().remove(mainTitleArea);
    }
    
    @Override
    public void close() {
        super.close();
        mediaPlayer.pause();
    }
    @Override
    public void minimize() {
        super.minimize();
        mediaPlayer.pause();
    }
    
    public void shutdown() {
        close();
        parent.getChildren().remove(this);        
    }
    
    public void setVideo() {
        try {
            media = ResourceUtils.loadRandomMediaMp4();
            if(null != media) {
                mediaPlayer = new MediaPlayer(media);
                mediaView.setMediaPlayer(mediaPlayer);
                mediaPlayer.seek(Duration.seconds(1));
                mediaPlayer.play();
            } else {
                shutdown();
            }
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(VideoPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
