package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

//import org.fxyz3d.importers.Model3D;
//import org.fxyz3d.importers.obj.ObjImporter;

/**
 * @author Sean Phillips
 */
public class MaterialModel extends Group {
    private static final Logger LOG = LoggerFactory.getLogger(MaterialModel.class);
    public Color diffuseColor;
    public String modelResource;
    //    Model3D model;
    public SimpleDoubleProperty scalingBind = null;
    public boolean animateOnHover = false;
    List<Material> originalMaterials;

    public MaterialModel(String modelResource, Color diffuseColor, double scale) throws IOException {

        this.diffuseColor = diffuseColor;
        scalingBind = new SimpleDoubleProperty(scale);
        setScaleX(scale);
        setScaleY(scale);
        setScaleZ(scale);

//        ObjImporter importer = new ObjImporter();
//        model = importer.load(
//        ResourceUtils.class.getResource("/edu/jhuapl/trinity/models/" + modelResource + ".obj"));
//        getChildren().add(model.getRoot());

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
//                    for(Material m : model.getMaterials()) {
//                        PhongMaterial material = (PhongMaterial)m;
//                        if(ActiveKeyEventHandler.isPressed(KeyCode.CONTROL))
//                            material.setBumpMap(textureImage);
//                        else if(ActiveKeyEventHandler.isPressed(KeyCode.SHIFT))
//                            material.setSpecularMap(textureImage);
//                        else if(ActiveKeyEventHandler.isPressed(KeyCode.ALT))
//                            material.setSelfIlluminationMap(textureImage);
//                        else //diffuse
//                            material.setDiffuseMap(textureImage);
//                    }
                    event.setDropCompleted(true);
                } catch (MalformedURLException ex) {
                    LOG.error(null, ex);
                    event.setDropCompleted(false);
                }
                event.consume();
            }
        });
    }

    public void setChildVisible(int index, boolean visible) {
//        if(model.getRoot().getChildren().size() > index)
//            model.getRoot().getChildren().get(index).setVisible(visible);
    }

    public void resetMaterials(Color diffuseColor) {
//        for(Material m : model.getMaterials()) {
//            PhongMaterial material = (PhongMaterial)m;
//            material.setBumpMap(null);
//            material.setSpecularMap(null);
//            material.setSelfIlluminationMap(null);
//            material.setDiffuseMap(null);
//            material.setDiffuseColor(diffuseColor);
//        }
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
//        if(null != scalingBind)
//            inTransition.setOnFinished(e->bindScale(scalingBind));
    }
}
