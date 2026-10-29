package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.utils.metric.Metric;
import edu.jhuapl.trinity.utils.metric.CosineMetric;
import edu.jhuapl.trinity.utils.metric.AngularMetric;
import java.util.List;
import java.util.Optional;

/** Pure Java nearest-landmark classification; no JavaFX scene-graph access. */
public final class TextLandmarkSimilarityEngine {
    private TextLandmarkSimilarityEngine() { }

    public record Landmark(String label, double[] values) { }
    public record Match(String label, double distance) { }

    public static boolean usable(double[] data) {
        if (data == null || data.length == 0) return false;
        for (double value : data) if (!Double.isFinite(value)) return false;
        return true;
    }

    public static boolean compatible(double[] target, double[] landmark) {
        return usable(target) && usable(landmark) && target.length == landmark.length;
    }

    private static boolean isZero(double[] values) {
        for (double value : values) if (value != 0.0) return false;
        return true;
    }

    public static Optional<Match> nearest(double[] target, List<Landmark> landmarks, Metric metric) {
        if (metric == null || !usable(target) || landmarks == null) return Optional.empty();
        Match best = null;
        for (Landmark landmark : landmarks) {
            if (landmark == null || !compatible(target, landmark.values())) continue;
            // A zero vector has no meaningful direction for cosine/angular distance.
            if ((metric instanceof CosineMetric || metric instanceof AngularMetric)
                && (isZero(target) || isZero(landmark.values()))) continue;
            double distance;
            try {
                distance = metric.distance(target, landmark.values());
            } catch (RuntimeException ex) {
                // Some metrics cannot evaluate particular vectors (e.g. zero denominators).
                continue;
            }
            if (!Double.isFinite(distance)) continue;
            if (best == null || distance < best.distance()) best = new Match(landmark.label(), distance);
        }
        return Optional.ofNullable(best);
    }
}
