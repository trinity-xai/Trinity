package edu.jhuapl.trinity.javafx.components.annotations;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;

import java.util.List;

/**
 * @author Sean Phillips
 */
public class SegmentationOverlay extends StackPane {
    public static Color DEFAULT_POLYGON_STROKECOLOR = Color.ALICEBLUE;
    public static Color DEFAULT_POLYGON_FILLCOLOR = Color.TRANSPARENT;
    public static Color DEFAULT_LABEL_FILLCOLOR = Color.CORNFLOWERBLUE;
    public Polygon segmentationPolygon;
    public Label overlayLabel;
    Paint polygonStroke = DEFAULT_POLYGON_STROKECOLOR;
    Paint polygonFill = DEFAULT_POLYGON_FILLCOLOR;
    Paint polygonMouseEnteredStroke = DEFAULT_POLYGON_STROKECOLOR;
    Paint polygonMouseEnteredFill = Color.GHOSTWHITE.deriveColor(1, 1, 1, 0.1);
    List<Double> xyCoordinates;
    String id;
    double bboxX;
    double bboxY;

    public SegmentationOverlay(double bboxX, double bboxY, List<Double> xyCoordinates, String id) {
        this.bboxX = bboxX;
        this.bboxY = bboxY;
        this.xyCoordinates = xyCoordinates;
        this.id = id;
        double[] coords = xyCoordinates.stream().mapToDouble(d -> d).toArray();
        segmentationPolygon = new Polygon(coords);
        segmentationPolygon.setFill(DEFAULT_POLYGON_FILLCOLOR);
        segmentationPolygon.setStroke(DEFAULT_POLYGON_STROKECOLOR);

        overlayLabel = new Label(id);
        overlayLabel.setBackground(new Background(new BackgroundFill(
            DEFAULT_LABEL_FILLCOLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        getChildren().addAll(segmentationPolygon, overlayLabel);
        setPickOnBounds(false);
        setOnMouseEntered(e -> {
            setBackground(new Background(new BackgroundFill(
                polygonMouseEnteredFill, CornerRadii.EMPTY, Insets.EMPTY)));
        });
        setOnMouseExited(e -> {
            setBackground(new Background(new BackgroundFill(
                polygonFill, CornerRadii.EMPTY, Insets.EMPTY)));
        });
        setLayoutX(bboxX);
        setLayoutY(bboxY);
    }

    public void setStroke(Paint paint) {
        polygonStroke = paint;
        segmentationPolygon.setStroke(paint);
    }

    public void setFill(Paint paint) {
        polygonFill = paint;
        segmentationPolygon.setFill(paint);
    }

    public void setMouseEnteredStroke(Paint paint) {
        segmentationPolygon.setStroke(paint);
    }

    public void setMouseEnteredFill(Paint paint) {
        segmentationPolygon.setFill(paint);
    }

}
