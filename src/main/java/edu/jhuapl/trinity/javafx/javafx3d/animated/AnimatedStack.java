package edu.jhuapl.trinity.javafx.javafx3d.animated;

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

import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class AnimatedStack extends Group {
    public double DEFAULT_RATEOFCHANGE = 0.01;
    public double rateOfChange = DEFAULT_RATEOFCHANGE;
    float[] uvCoords = {0, 0, 1, 0, 1, 1, 0, 1};
    public static String DEFAULT_TOP_TEXTURE = "neonrectanglepurple";
    public static String DEFAULT_STACK_TEXTURE = "verticalbluelines";
    ArrayList<AnimatedBox> boxes = new ArrayList<>();
    PhongMaterial topMat;
    PhongMaterial highMat;
    float width, height, depth;

    public AnimatedStack(int count, float width, float height, float depth) throws IOException {
        this(ResourceUtils.load3DTextureImage(DEFAULT_TOP_TEXTURE),
            ResourceUtils.load3DTextureImage(DEFAULT_STACK_TEXTURE),
            count, width, height, depth);
    }

    public AnimatedStack(Image top, Image stack, int count, float width, float height, float depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        topMat = new PhongMaterial(Color.WHITE, top, top, null, null);
        highMat = new PhongMaterial(Color.WHITE, stack, stack, null, null);

        for (int i = 0; i < count; i++) {
            double totalHeight = height * (count - 1);
            AnimatedBox box = new AnimatedBox(width, height, depth);
            box.setTranslateY(-(totalHeight - i * height));
            box.addEventHandler(DragEvent.DRAG_OVER, event -> {
                Dragboard db = event.getDragboard();
                if (db.hasFiles() &&
                    JavaFX3DUtils.isTextureFile(db.getFiles().get(0))) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
            });
            if (i < 1) {
                box.setMaterial(topMat);
                box.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
                    topDropped(event);
                });
            } else {
                box.setMaterial(highMat);
                box.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
                    stackDropped(event);
                });
            }
            boxes.add(box);
            getChildren().add(box);
        }
        addEventHandler(ScrollEvent.SCROLL, event -> {
            if (event.getTotalDeltaY() < 0) {
                addStack(width, height, depth);
            } else {
                if (boxes.size() > 1)
                    removeStack();
            }
            event.consume();
        });
//        addEventHandler(DragEvent.DRAG_ENTERED, event -> expand());
//        addEventHandler(DragEvent.DRAG_EXITED, event -> contract());
    }

    private void removeStack() {
        boxes.remove(boxes.size() - 1);
        getChildren().remove(getChildren().size() - 1);
        //slide all the other boxes down
        for (int i = 0; i < boxes.size(); i++) {
            //slide it down one height unit
            boxes.get(i).setTranslateY(boxes.get(i).getTranslateY() + height);
        }
    }

    private void addStack(float width, float height, float depth) {
        double totalHeight = height * (boxes.size() - 1);

        AnimatedBox box = new AnimatedBox(width, height, depth);
        box.addEventHandler(DragEvent.DRAG_OVER, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() &&
                JavaFX3DUtils.isTextureFile(db.getFiles().get(0))) {
                event.acceptTransferModes(TransferMode.COPY);
            }
        });
        box.setMaterial(highMat);
        box.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            stackDropped(event);
        });
        //set the translate of the new box to be the bottom box
        box.setTranslateY(-height);
        //slide all the other boxes up
        for (int i = 0; i < boxes.size(); i++) {
            boxes.get(i).setTranslateY(-(totalHeight - i * height));
        }
        boxes.add(box);
        getChildren().add(box);
    }

    private void topDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles() &&
            JavaFX3DUtils.isTextureFile(db.getFiles().get(0))) {
            Image textureImage;
            try {
                textureImage = new Image(db.getFiles().get(0).toURI().toURL().toExternalForm());
                PhongMaterial material = new PhongMaterial(Color.GREY,
                    textureImage,
                    textureImage,
                    null,
                    null
                );
                boxes.get(0).setMaterial(material);
                event.setDropCompleted(true);
            } catch (MalformedURLException ex) {
                Logger.getLogger(AnimatedSphere.class.getName()).log(Level.SEVERE, null, ex);
                event.setDropCompleted(false);
            }
            event.consume();
        }
    }

    private void stackDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles() &&
            JavaFX3DUtils.isTextureFile(db.getFiles().get(0))) {
            Image textureImage;
            try {
                textureImage = new Image(db.getFiles().get(0).toURI().toURL().toExternalForm());
                PhongMaterial material = new PhongMaterial(Color.GREY,
                    textureImage,
                    textureImage,
                    null,
                    null
                );
                for (int i = 1; i < boxes.size(); i++)
                    boxes.get(i).setMaterial(material);
                event.setDropCompleted(true);
            } catch (MalformedURLException ex) {
                Logger.getLogger(AnimatedSphere.class.getName()).log(Level.SEVERE, null, ex);
                event.setDropCompleted(false);
            }
            event.consume();
        }
    }
}
