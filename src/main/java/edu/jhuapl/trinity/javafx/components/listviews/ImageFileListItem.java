package edu.jhuapl.trinity.javafx.components.listviews;

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
    public static double PREF_LABEL_WIDTH = 500;
    private ImageView imageView;
    private Label label;
    private File file;
    public static Image DEFAULT_ICON = ResourceUtils.loadIconFile("noimage");
    public boolean renderIcon;
    
    public ImageFileListItem(File file) {
        this(file, true);
    }
    public ImageFileListItem(File file, boolean renderIcon) {
        this.file = file;
        this.renderIcon = renderIcon;
        label = new Label(file.getAbsolutePath());
        label.setPrefWidth(PREF_LABEL_WIDTH);
        if(renderIcon) {
            try {
                imageView = new ImageView(ResourceUtils.loadImageFile(file));
            } catch (IOException ex) {
                Logger.getLogger(ImageFileListItem.class.getName()).log(Level.SEVERE, null, ex);
                imageView = new ImageView(DEFAULT_ICON);
            }
        } else {
            imageView = new ImageView(DEFAULT_ICON);
        }
        imageView.setFitWidth(32);
        imageView.setFitHeight(32);
        getChildren().addAll(imageView, label);
        setSpacing(20);
        Tooltip.install(this, new Tooltip(file.getAbsolutePath()));
    }
    public void setLabelWidth(double width) {
        label.setPrefWidth(width);
    }
}
