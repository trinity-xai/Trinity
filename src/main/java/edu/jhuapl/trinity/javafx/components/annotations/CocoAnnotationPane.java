package edu.jhuapl.trinity.javafx.components.annotations;

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Sean Phillips
 */
public class CocoAnnotationPane extends StackPane {
    private static final Logger LOG = LoggerFactory.getLogger(CocoAnnotationPane.class);
    public static double DEFAULT_SIZE = 512;
    //make transparent so it doesn't interfere with subnode transparency effects
    private Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));

    private Color fillColor = Color.ALICEBLUE.deriveColor(1, 1, 1, 0.1);
    private Image baseImage;
    private ImageView baseImageView;
    private Canvas heatMapCanvas;
    private Pane annotationPane;
    private GraphicsContext canvasGC;

    public CocoAnnotationPane() {
        baseImageView = new ImageView();
        try {
            baseImage = ResourceUtils.load3DTextureImage("carl-b-portrait");
            baseImageView.setImage(baseImage);
        } catch (IOException ex) {
            LOG.warn("Failed to load default Coco Annotation image of Carl.");
        }
        baseImageView.setPreserveRatio(true);
        baseImageView.setSmooth(true);
        annotationPane = new Pane();
        setBackground(transBack);
        setMinSize(DEFAULT_SIZE, DEFAULT_SIZE);
        StackPane.setAlignment(baseImageView, Pos.TOP_LEFT);

        getChildren().addAll(baseImageView, annotationPane);

//@TODO SMP need logic to automatically resize overlays to match any fit size changes
//        baseImageView.fitWidthProperty().bind(centerStackPane.widthProperty());

        widthProperty().addListener(cl -> {
            annotationPane.setMinWidth(getWidth());
            annotationPane.setPrefWidth(getWidth());
            annotationPane.setMaxWidth(getWidth());
//            transformOverlays();
        });
        heightProperty().addListener(cl -> {
            annotationPane.setMinHeight(getHeight());
            annotationPane.setPrefHeight(getHeight());
            annotationPane.setMaxHeight(getHeight());
        });
    }

    private void transformOverlays() {
        for (Node node : annotationPane.getChildren()) {
            if (node instanceof BBoxOverlay bbox) {
                Point2D xy = bbox.localToParent(bbox.bbox.get(0), bbox.bbox.get(1));

                Point2D x1y1 = bbox.localToParent(bbox.bbox.get(0) + bbox.bbox.get(2),
                    bbox.bbox.get(1) + bbox.bbox.get(2));
                bbox.rectangle.setX(xy.getX());
                bbox.rectangle.setY(xy.getY());
                bbox.rectangle.setWidth(x1y1.getX() - xy.getX());
                bbox.rectangle.setHeight(x1y1.getY() - xy.getY());

                bbox.setLayoutX(xy.getX());
                bbox.setLayoutY(xy.getY());
            }
        }
    }

    public void clearAnnotations() {
        annotationPane.getChildren().clear();
    }

    public void addCocoBBox(BBoxOverlay bbox) {
        annotationPane.getChildren().add(bbox); //should be pretranslated
    }

    public void addSegmentationOverlay(SegmentationOverlay seg) {
        annotationPane.getChildren().add(seg); //should be pretranslated
    }

    public void setBaseImage(Image image) {
        baseImage = image;
        baseImageView.setImage(baseImage);
    }
}
