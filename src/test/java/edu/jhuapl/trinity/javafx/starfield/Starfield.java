package edu.jhuapl.trinity.javafx.starfield;

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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Starfield extends Application {

    public static ObservableList<Node> child;
    //
    private static final String title = "JellyBeanci";
    public static final int width = 800;
    public static final int height = 800;
    public static double speed = 0;
    private static Color backcolor = Color.rgb(13, 13, 13);

    private static Timeline update;

    @Override
    public void start(Stage stage) throws Exception {
        Pane root = new Pane();
        child = root.getChildren();
        //
        for (int i = 0; i < 500; i++) {
            new Star();
        }
        for (Star star : Star.stars) {
            child.add(star.getBody());
        }
        //

        root.setOnScroll(e -> {
            speed += e.getDeltaY() * 0.1;
        });

        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case F1: {
                    //PLAY
                    update.play();
                    break;
                }
                case F2: {
                    //PAUSE
                    update.pause();
                    break;
                }
                case F3: {
                    //Show Child Count
                    System.out.println("Child Count: " + child.size());
                    break;
                }
            }
        });
        update = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            //60 fps
            for (Star star : Star.stars) {
                star.update();
            }

        }));
        update.setCycleCount(Timeline.INDEFINITE);
        update.setRate(1);
        update.setAutoReverse(false);
        //update.play(); //uncomment for play when start
        //
        stage.setTitle(title);
        stage.setResizable(false);
        stage.setScene(new Scene(root, width - 10, height - 10, backcolor));
        stage.show();
        root.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
