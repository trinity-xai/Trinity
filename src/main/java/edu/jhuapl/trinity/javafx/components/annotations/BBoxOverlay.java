package edu.jhuapl.trinity.javafx.components.annotations;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * @author Sean Phillips
 */
public class BBoxOverlay extends StackPane {
    public static Color DEFAULT_RECTANGLE_STROKECOLOR = Color.ALICEBLUE;
    public static Color DEFAULT_RECTANGLE_FILLCOLOR = Color.TRANSPARENT;
    public static Color DEFAULT_LABEL_FILLCOLOR = Color.CORNFLOWERBLUE;
    public Rectangle rectangle;
    public Label overlayLabel;
    public List<Double> bbox;
    public String id;

    public BBoxOverlay(List<Double> bbox, String id) {
        this.bbox = bbox;
        this.id = id;
        rectangle = new Rectangle(bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3));
        rectangle.setFill(DEFAULT_RECTANGLE_FILLCOLOR);
        rectangle.setStroke(DEFAULT_RECTANGLE_STROKECOLOR);

        overlayLabel = new Label(id);
        overlayLabel.setBackground(new Background(new BackgroundFill(
            DEFAULT_LABEL_FILLCOLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        getChildren().addAll(rectangle, overlayLabel);
        setPickOnBounds(false);
        setOnMouseEntered(e -> {
            setBackground(new Background(new BackgroundFill(
                Color.GHOSTWHITE.deriveColor(1, 1, 1, 0.1), CornerRadii.EMPTY, Insets.EMPTY)));
        });
        setOnMouseExited(e -> {
            setBackground(Background.EMPTY);
        });
        setLayoutX(bbox.get(0));
        setLayoutY(bbox.get(1));
    }
}
