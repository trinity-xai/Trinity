package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

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

import edu.jhuapl.trinity.javafx.handlers.ActiveKeyEventHandler;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

/**
 * @author Sean Phillips
 */
public class MaterialBox extends Box {
    private static final Logger LOG = LoggerFactory.getLogger(MaterialBox.class);
    public Color color;
    public SimpleDoubleProperty scalingBind = null;
    public boolean animateOnHover = false;

    public MaterialBox(double width, double height, double depth) {
        super(width, height, depth);
        color = Color.WHITESMOKE.deriveColor(1, 1, 1, 0.05);
        setMaterial(new PhongMaterial(color));
        addEventHandler(DragEvent.DRAG_OVER, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() &&
                JavaFX3DUtils.isTextureFile(db.getFiles().get(0))) {
                event.acceptTransferModes(TransferMode.COPY);
            }
        });
        addEventHandler(DragEvent.DRAG_ENTERED, event -> expand());
        addEventHandler(DragEvent.DRAG_EXITED, event -> contract());

        // Dropping over surface
        addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() &&
                JavaFX3DUtils.isTextureFile(db.getFiles().get(0))) {
                Image textureImage;
                try {
                    PhongMaterial material = (PhongMaterial) getMaterial();
                    textureImage = new Image(db.getFiles().get(0).toURI().toURL().toExternalForm());
                    if (ActiveKeyEventHandler.isPressed(KeyCode.CONTROL))
                        material.setBumpMap(textureImage);
                    else if (ActiveKeyEventHandler.isPressed(KeyCode.SHIFT))
                        material.setSpecularMap(textureImage);
                    else if (ActiveKeyEventHandler.isPressed(KeyCode.ALT))
                        material.setSelfIlluminationMap(textureImage);
                    else //diffuse
                        material.setDiffuseMap(textureImage);
                    setMaterial(material);
                    event.setDropCompleted(true);
                } catch (MalformedURLException ex) {
                    LOG.error(null, ex);
                    event.setDropCompleted(false);
                }
                event.consume();
            }
        });
    }

    public void bindScale(SimpleDoubleProperty scaleProp) {
        scalingBind = scaleProp;
        scalingBind.addListener((obs, oV, nv) -> {
            setScaleX(nv.doubleValue());
            setScaleY(nv.doubleValue());
            setScaleZ(nv.doubleValue());

        });

    }

    private void expand() {
        ScaleTransition outTransition =
            new ScaleTransition(Duration.millis(50), this);
        outTransition.setToX(null != scalingBind ? scalingBind.doubleValue() * 3.0 : 3f);
        outTransition.setToY(null != scalingBind ? scalingBind.doubleValue() * 3.0 : 3f);
        outTransition.setToZ(null != scalingBind ? scalingBind.doubleValue() * 3.0 : 3f);
        outTransition.setCycleCount(1);
        outTransition.setAutoReverse(false);
        outTransition.setInterpolator(Interpolator.EASE_OUT);
        outTransition.play();
    }

    private void contract() {

        ScaleTransition inTransition =
            new ScaleTransition(Duration.millis(50), this);
        inTransition.setToX(null != scalingBind ? scalingBind.doubleValue() : 1f);
        inTransition.setToY(null != scalingBind ? scalingBind.doubleValue() : 1f);
        inTransition.setToZ(null != scalingBind ? scalingBind.doubleValue() : 1f);
        inTransition.setCycleCount(1);
        inTransition.setAutoReverse(false);
        inTransition.setInterpolator(Interpolator.EASE_OUT);
        inTransition.play();
        if (null != scalingBind)
            inTransition.setOnFinished(e -> bindScale(scalingBind));
    }
}
