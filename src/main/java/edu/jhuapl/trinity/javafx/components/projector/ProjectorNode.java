package edu.jhuapl.trinity.javafx.components.projector;

import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class ProjectorNode extends Pane {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectorNode.class);
    ImageView imageView;
    Border hoverBorder;
    Border emptyBorder;
    Border selectedBorder;
    int borderWidth = 50;

    public ProjectorNode(Image image) {
        imageView = new ImageView(image);
        getChildren().add(imageView);
        emptyBorder = Border.EMPTY;
        hoverBorder = new Border(new BorderStroke(Color.LIGHTCYAN.deriveColor(1, 1, 1, 0.75),
            BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
            new BorderWidths(borderWidth),
            new Insets(-borderWidth / 2.0, -borderWidth / 2.0, -borderWidth / 2.0, -borderWidth / 2.0)));
        selectedBorder = new Border(new BorderStroke(Color.CYAN.deriveColor(1, 1, 1, 0.75),
            BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
            new BorderWidths(borderWidth),
            new Insets(-borderWidth / 2.0, -borderWidth / 2.0, -borderWidth / 2.0, -borderWidth / 2.0)));

        setBorder(Border.EMPTY);
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            requestFocus();
            setBorder(hoverBorder);
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            setBorder(emptyBorder);
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            requestFocus();
            setBorder(selectedBorder);
        });
    }
}
