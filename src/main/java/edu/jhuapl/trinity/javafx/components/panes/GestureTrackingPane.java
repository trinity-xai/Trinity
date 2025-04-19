package edu.jhuapl.trinity.javafx.components.panes;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * A pane containing a dynamic resizable canvas with methods for
 * tracking gesture touch points.
 *
 * @author Sean Phillips
 */
public class GestureTrackingPane extends CanvasOverlayPane {

    Rectangle bindingRectangle = null;
    public SimpleBooleanProperty showNearsidePoints = new SimpleBooleanProperty(false);
    GraphicsContext gc;

    public GestureTrackingPane() {
        this(false, true);
    }

    public GestureTrackingPane(boolean debugBorder, boolean clearCanvas) {
        this(new Canvas(), debugBorder, clearCanvas);
    }

    public GestureTrackingPane(boolean debugBorder, boolean clearCanvas, Rectangle rectangle) {
        this(new Canvas(), debugBorder, clearCanvas, rectangle);
    }

    public GestureTrackingPane(Canvas canvas, boolean debugBorder, boolean clearCanvas) {
        this(canvas, debugBorder, clearCanvas, null);
    }

    public GestureTrackingPane(Canvas canvas, boolean debugBorder, boolean clearCanvas, Rectangle rectangle) {
        super(canvas, debugBorder, clearCanvas);
        setPickOnBounds(false); // allows you to click to pass through.
        setMouseTransparent(true);
        getCanvas().setPickOnBounds(false);
        getCanvas().setMouseTransparent(true);
        setRectangle(rectangle);
        setBackground(new Background(new BackgroundFill(
            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

        widthProperty().addListener(cl -> {
            redraw();
        });
        heightProperty().addListener(cl -> {
            redraw();
        });
        gc = getCanvas().getGraphicsContext2D();
    }

    public void drawPoint(double x, double y, double radius, Color color) {
        gc.setFill(color);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    public void setFill(Color fillColor) {
        gc.setFill(fillColor);
        gc.fillRect(0, 0, getCanvas().getWidth(), getCanvas().getHeight());
    }


    public void clearAll() {
        gc.clearRect(0, 0, getCanvas().getWidth(), getCanvas().getHeight());
    }

    public GraphicsContext getGraphicsContext() {
        return gc;
    }

    public void redraw() {
        layout();
    }

    public final void setRectangle(Rectangle rectangle) {
        //Translations
        translateXProperty().unbind();
        translateYProperty().unbind();
        translateZProperty().unbind();
        //Rotations
        rotationAxisProperty().unbind();
        rotateProperty().unbind();
        //Sizing (but not scale)
        minWidthProperty().unbind();
        minHeightProperty().unbind();
        maxWidthProperty().unbind();
        maxHeightProperty().unbind();
        //visibility
        visibleProperty().unbind();

        bindingRectangle = rectangle;
        if (null != bindingRectangle) {
            translateXProperty().bind(bindingRectangle.translateXProperty());
            translateYProperty().bind(bindingRectangle.translateYProperty());
            translateZProperty().bind(bindingRectangle.translateZProperty());
            rotationAxisProperty().bind(bindingRectangle.rotationAxisProperty());
            rotateProperty().bind(bindingRectangle.rotateProperty());
            minWidthProperty().bind(bindingRectangle.widthProperty());
            minHeightProperty().bind(bindingRectangle.heightProperty());
            maxWidthProperty().bind(bindingRectangle.widthProperty());
            maxHeightProperty().bind(bindingRectangle.heightProperty());
            visibleProperty().bind(bindingRectangle.visibleProperty().or(showNearsidePoints));
        }
    }
}
