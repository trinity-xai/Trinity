package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
public class ImageFileListItem extends HBox {
    private static final Logger LOG = LoggerFactory.getLogger(ImageFileListItem.class);
    public static double PREF_LABEL_WIDTH = 500;
    private ImageView imageView;
    private Label fileLabel;
    private File file;
    public static Image DEFAULT_ICON = ResourceUtils.loadIconFile("noimage");
    public boolean renderIcon;

    public ImageFileListItem(File file) {
        this(file, true);
    }

    public ImageFileListItem(File file, boolean renderIcon) {
        this.file = file;
        this.renderIcon = renderIcon;
        fileLabel = new Label(file.getAbsolutePath());
        fileLabel.setPrefWidth(PREF_LABEL_WIDTH);
        if (renderIcon) {
            try {
                imageView = new ImageView(ResourceUtils.loadImageFile(file));
            } catch (IOException ex) {
                LOG.error(null, ex);
                imageView = new ImageView(DEFAULT_ICON);
            }
        } else {
            imageView = new ImageView(DEFAULT_ICON);
        }
        imageView.setFitWidth(32);
        imageView.setFitHeight(32);
        getChildren().addAll(imageView, fileLabel);
        setSpacing(20);
        Tooltip.install(this, new Tooltip(file.getAbsolutePath()));
    }

    public void setLabelWidth(double width) {
        fileLabel.setPrefWidth(width);
    }

    public static Function<File, ImageFileListItem> itemFromFile = file -> {
        return new ImageFileListItem(file);
    };
}
