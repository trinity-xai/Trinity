package edu.jhuapl.trinity.utils.fun.solar;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class FlareOcclusionUtil {

    public static double computeFlareAlpha(double sunX, double sunY, double centerX, double centerY, double sceneW, double sceneH) {
        // Visibility check
        boolean isVisible = sunX >= 0 && sunX <= sceneW && sunY >= 0 && sunY <= sceneH;
        if (!isVisible) {
            return 0;
        }

        double dx = centerX - sunX;
        double dy = centerY - sunY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double maxDist = Math.sqrt(centerX * centerX + centerY * centerY);

        // Falloff curve
        double alpha = 1.0 - Math.pow(distance / maxDist, 1.5);
        return Math.max(0.0, Math.min(1.0, alpha));
    }

    /**
     * Computes a visibility factor [0.0, 1.0] for the sun based on GUI
     * occlusion. 1.0 = fully visible, 0.0 = fully occluded, values in-between
     * indicate partial occlusion/falloff.
     *
     * @param sunScenePos Position of the sun in scene coordinates.
     * @param sunRadius   The radius to check for depth of occlusion
     * @param occluders   List of GUI nodes that may block the sun.
     * @param fadeRadius  Number of pixels around the occluder edge to apply soft
     *                    fade-out.
     * @return Visibility factor in [0, 1].
     */

    public static double computeSunOcclusionFactor(
        Point2D sunScenePos,
        double sunRadius,
        List<Node> occluders
    ) {
        if (sunScenePos == null || occluders == null || occluders.isEmpty()) {
            return 1.0;
        }

        double worstOcclusion = 0.0;

        for (Node node : occluders) {
            Bounds bounds = node.localToScene(node.getBoundsInLocal());
            if (null == bounds || node.getScene() == null
                || !isEffectivelyVisible(node) || node.getOpacity() <= 0.0) {
                continue;
            }

            // Optional: handle circular occluders like PlanetaryDisc
            if (node instanceof Circle occluderCircle) {
                Point2D occluderCenter = node.localToScene(occluderCircle.getCenterX(), occluderCircle.getCenterY());
                double occluderRadius = occluderCircle.getRadius();
                double opacity = node.getOpacity();

                double dx = sunScenePos.getX() - occluderCenter.getX();
                double dy = sunScenePos.getY() - occluderCenter.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);

                double totalRadius = sunRadius + occluderRadius;
                double falloff;

                if (dist >= totalRadius) {
                    falloff = 0.0; // No intersection
                } else if (dist <= Math.abs(occluderRadius - sunRadius)) {
                    falloff = 1.0; // Full occlusion
                } else {
                    // Partial overlap — use interpolation
                    double t = (totalRadius - dist) / (2.0 * sunRadius);
                    falloff = smoothstep(clamp(t, 0.0, 1.0));
                }

                double localOcclusion = opacity * falloff;
                worstOcclusion = Math.max(worstOcclusion, localOcclusion);
            } else {
                double opacity = node.getOpacity();
                double sunX = sunScenePos.getX();
                double sunY = sunScenePos.getY();
                // Signed distance from sun center to edge of occluder
                double dx = Math.max(bounds.getMinX() - sunX, sunX - bounds.getMaxX());
                double dy = Math.max(bounds.getMinY() - sunY, sunY - bounds.getMaxY());
                double signedDistance = Math.max(dx, dy); // Negative if inside
                double falloff;
                if (signedDistance >= sunRadius) {
                    falloff = 0.0; // Fully visible
                } else if (signedDistance <= -sunRadius) {
                    falloff = 1.0; // Fully occluded
                } else {
                    // Interpolate between visible and occluded (center near edge)
                    double t = (sunRadius - signedDistance) / (2.0 * sunRadius);
                    falloff = smoothstep(clamp(t, 0.0, 1.0));
                }

                double localOcclusion = opacity * falloff;
                worstOcclusion = Math.max(worstOcclusion, localOcclusion);
            }
        }

        return 1.0 - clamp(worstOcclusion, 0.0, 1.0);
    }

    public static boolean isEffectivelyVisible(Node node) {
        while (node != null) {
            if (!node.isVisible()) {
                return false;
            }
            node = node.getParent();
        }
        return true;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }

    /**
     * Recursively collects all visible, non-transparent nodes inside a
     * container that could occlude the flare.
     *
     * @param root The parent node to search (e.g., Pane, Group, VBox)
     * @return A flat list of visible, partially opaque nodes
     */
    public static List<Node> collectOccluders(Parent root) {
        List<Node> result = new ArrayList<>();
        collectOccludersRecursive(root, result);
        return result;
    }

    private static void collectOccludersRecursive(Parent parent, List<Node> result) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (!node.isVisible() || node.getOpacity() <= 0.0) {
                continue;
            }

            // Optional: ignore nodes tagged with a special property
            Object ignore = node.getProperties().get("flare.ignore");
            if (ignore instanceof Boolean && (Boolean) ignore) {
                continue;
            }

            result.add(node);

            if (node instanceof Parent) {
                collectOccludersRecursive((Parent) node, result);
            }
        }
    }
}
