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
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

/**
 * @author Sean Phillips
 */
public class AnimatedSphere extends Sphere implements Selectable3D {
    private static final Logger LOG = LoggerFactory.getLogger(AnimatedSphere.class);
    public SimpleDoubleProperty scalingBind = null;
    private Color color;
    private PhongMaterial phongMaterial;
    private PhongMaterial selectedMaterial;
    private double sphereRadius;
    private boolean animateOnHover = false;
    private double expansionScale = 3.0;


    public AnimatedSphere(PhongMaterial material, double radius, int divisions, boolean animated) {
        super(radius, divisions);
        this.sphereRadius = radius;
        this.phongMaterial = material;
        this.animateOnHover = animated;
        setMaterial(this.phongMaterial);
        selectedMaterial = DEFAULT_SELECTED_MATERIAL;
        setOnMouseEntered(mouseEnter -> {
            if (animateOnHover) {
                expand();
            }
        });
        setOnMouseExited(mouseExit -> {
            if (animateOnHover) {
                contract();
            }
        });
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
                    textureImage = new Image(db.getFiles().get(0).toURI().toURL().toExternalForm());
                    material.setDiffuseMap(textureImage);
                    event.setDropCompleted(true);
                } catch (MalformedURLException ex) {
                    LOG.error(null, ex);
                    event.setDropCompleted(false);
                }
                event.consume();
            }
        });
    }
//
//    public void bindScale(SimpleDoubleProperty scaleProp) {
//        scalingBind = scaleProp;
//        scalingBind.addListener((obs, oV, nv) -> {
//            setRadius(getRadius() * nv.doubleValue());
//        });
//    }

    private void expand() {
        ScaleTransition outTransition =
            new ScaleTransition(Duration.millis(50), this);
        outTransition.setToX(null != scalingBind ? scalingBind.doubleValue() * getExpansionScale() : getExpansionScale());
        outTransition.setToY(null != scalingBind ? scalingBind.doubleValue() * getExpansionScale() : getExpansionScale());
        outTransition.setToZ(null != scalingBind ? scalingBind.doubleValue() * getExpansionScale() : getExpansionScale());
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
    }

    @Override
    public void select() {
//        expand();
        setMaterial(selectedMaterial);
    }

    @Override
    public void unselect() {
//        contract();
        setMaterial(phongMaterial);
    }

    @Override
    public boolean isSelected() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
        phongMaterial.setDiffuseColor(color);
    }

    /**
     * @return the material
     */
    public PhongMaterial getPhongMaterial() {
        return phongMaterial;
    }

    /**
     * @param material the material to set
     */
    public void setPhongMaterial(PhongMaterial material) {
        this.phongMaterial = material;
        this.setMaterial(phongMaterial);
    }

    /**
     * @return the radius
     */
    public double getSphereRadius() {
        return sphereRadius;
    }

    /**
     * @param radius the radius to set
     */
    public void setSphereRadius(double radius) {
        this.sphereRadius = radius;
        this.setRadius(sphereRadius);
    }

    /**
     * @return the animateOnHover
     */
    public boolean isAnimateOnHover() {
        return animateOnHover;
    }

    /**
     * @param animateOnHover the animateOnHover to set
     */
    public void setAnimateOnHover(boolean animateOnHover) {
        this.animateOnHover = animateOnHover;
    }

    /**
     * @return the expansionScale
     */
    public double getExpansionScale() {
        return expansionScale;
    }

    /**
     * @param expansionScale the expansionScale to set
     */
    public void setExpansionScale(double expansionScale) {
        this.expansionScale = expansionScale;
    }
}
