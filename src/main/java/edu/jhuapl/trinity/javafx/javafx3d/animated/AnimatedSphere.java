package edu.jhuapl.trinity.javafx.javafx3d.animated;

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
    private boolean contractOnFinish = true;
    private double expansionScale = 3.0;
    private double animationTimeMS = 100.0;
    private boolean isAnimating = false;

    public AnimatedSphere(PhongMaterial material, double radius, int divisions, boolean animated) {
        super(radius, divisions);
        this.sphereRadius = radius;
        this.phongMaterial = material;
        this.animateOnHover = animated;
        setMaterial(this.phongMaterial);
        selectedMaterial = DEFAULT_SELECTED_MATERIAL;
        selectedMaterial.setSpecularColor(Color.ALICEBLUE); //fix for aarch64 Mac Ventura
        setOnMouseEntered(mouseEnter -> {
            if (animateOnHover) {
                expand(contractOnFinish);
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
        addEventHandler(DragEvent.DRAG_ENTERED, event -> expand(false));
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

    private void expand(boolean contractOnFinish) {
        if (isAnimating)
            return;
        ScaleTransition outTransition =
            new ScaleTransition(Duration.millis(getAnimationTimeMS()), this);
        outTransition.setToX(null != scalingBind ? scalingBind.doubleValue() * getExpansionScale() : getExpansionScale());
        outTransition.setToY(null != scalingBind ? scalingBind.doubleValue() * getExpansionScale() : getExpansionScale());
        outTransition.setToZ(null != scalingBind ? scalingBind.doubleValue() * getExpansionScale() : getExpansionScale());
        outTransition.setAutoReverse(contractOnFinish);
        if (contractOnFinish)
            outTransition.setCycleCount(2);
        else
            outTransition.setCycleCount(1);
        outTransition.setInterpolator(Interpolator.EASE_OUT);
        outTransition.setOnFinished(e -> isAnimating = false);
        outTransition.play();
    }

    private void contract() {
        if (isAnimating)
            return;

        ScaleTransition inTransition =
            new ScaleTransition(Duration.millis(getAnimationTimeMS()), this);
        inTransition.setToX(null != scalingBind ? scalingBind.doubleValue() : 1f);
        inTransition.setToY(null != scalingBind ? scalingBind.doubleValue() : 1f);
        inTransition.setToZ(null != scalingBind ? scalingBind.doubleValue() : 1f);
        inTransition.setCycleCount(1);
        inTransition.setAutoReverse(false);
        inTransition.setInterpolator(Interpolator.EASE_OUT);
        inTransition.setOnFinished(e -> isAnimating = false);
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

    public void setMaterialOpacity(double alpha) {
        double a = Math.max(0.0, Math.min(1.0, alpha));
        Color base = phongMaterial.getDiffuseColor();
        if (base == null) {
            base = (color != null) ? color : Color.WHITE;
        }
        Color withAlpha = new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
        // keep the cached color field in sync
        this.color = withAlpha;
        phongMaterial.setDiffuseColor(withAlpha);
        // Optional: match specular alpha for a more consistent look
        Color spec = phongMaterial.getSpecularColor();
        if (spec != null) {
            phongMaterial.setSpecularColor(new Color(spec.getRed(), spec.getGreen(), spec.getBlue(), a));
        }
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

    /**
     * @return the contractOnFinish
     */
    public boolean isContractOnFinish() {
        return contractOnFinish;
    }

    /**
     * @param contractOnFinish the contractOnFinish to set
     */
    public void setContractOnFinish(boolean contractOnFinish) {
        this.contractOnFinish = contractOnFinish;
    }

    /**
     * @return the animationTimeMS
     */
    public double getAnimationTimeMS() {
        return animationTimeMS;
    }

    /**
     * @param animationTimeMS the animationTimeMS to set
     */
    public void setAnimationTimeMS(double animationTimeMS) {
        this.animationTimeMS = animationTimeMS;
    }
}
