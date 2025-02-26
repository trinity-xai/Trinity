package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.utils.ResourceUtils;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * @author Sean Phillips
 */
public class ImageFileListItem extends HBox {
    public static double PREF_LABEL_WIDTH = 200;
    private ImageView imageView;
    private Label label;
    private File file;
    public static Image DEFAULT_ICON = ResourceUtils.loadIconFile("noimage");
    
    public ImageFileListItem(File file) {
        this.file = file;
        label = new Label(file.getAbsolutePath());
        label.setPrefWidth(PREF_LABEL_WIDTH);
        try {
            imageView = new ImageView(ResourceUtils.loadImageFile(file));
        } catch (IOException ex) {
            Logger.getLogger(ImageFileListItem.class.getName()).log(Level.SEVERE, null, ex);
            imageView = new ImageView(DEFAULT_ICON);
        }
        imageView.setFitWidth(32);
        imageView.setFitHeight(32);
        getChildren().addAll(imageView, label);
        setSpacing(20);
        Tooltip.install(this, new Tooltip(file.getAbsolutePath()));
//        colorPicker.valueProperty().addListener(cl -> {
//            if (null != colorPicker.getScene()) {
//                featureLayer.setColor(colorPicker.getValue());
//                FeatureLayer.updateFeatureLayer(featureLayer.getIndex(), featureLayer);
//                getScene().getRoot().fireEvent(new HyperspaceEvent(
//                    HyperspaceEvent.UPDATED_FEATURE_LAYER, featureLayer));
//            }
//        });
    }

}
