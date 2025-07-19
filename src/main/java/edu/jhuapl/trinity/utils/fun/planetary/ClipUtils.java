package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Node;
import javafx.scene.shape.Circle;

/**
 *
 * @author Sean Phillips
 */
public class ClipUtils {
    public static void applyCircularClip(Node node, Circle sourceCircle, double padding) {
        Circle clip = new Circle();
        clip.centerXProperty().bind(sourceCircle.centerXProperty());
        clip.centerYProperty().bind(sourceCircle.centerYProperty());
        clip.radiusProperty().bind(sourceCircle.radiusProperty().subtract(padding));
        node.setClip(clip);
    }
}
