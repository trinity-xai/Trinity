package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.utils.metric.Metric;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextLandmarkSimilarityEngineTest {
    private final Metric euclidean = Metric.getMetric("euclidean");

    @Test
    void findsNearestLandmarkWithoutMutatingCandidates() {
        var candidates = List.of(
            new TextLandmarkSimilarityEngine.Landmark("A", new double[]{0, 0}),
            new TextLandmarkSimilarityEngine.Landmark("B", new double[]{10, 0}));
        var match = TextLandmarkSimilarityEngine.nearest(new double[]{9, 0}, candidates, euclidean);
        assertEquals("B", match.orElseThrow().label());
        assertArrayEquals(new double[]{0, 0}, candidates.get(0).values());
    }

    @Test
    void noLandmarksAndMismatchedDimensionsAreSafe() {
        assertTrue(TextLandmarkSimilarityEngine.nearest(new double[]{1, 0}, List.of(), euclidean).isEmpty());
        assertTrue(TextLandmarkSimilarityEngine.nearest(new double[]{1, 0},
            List.of(new TextLandmarkSimilarityEngine.Landmark("bad", new double[]{1, 2, 3})), euclidean).isEmpty());
    }

    @Test
    void excludesInvalidVectorsAndDistances() {
        var landmarks = List.of(
            new TextLandmarkSimilarityEngine.Landmark("invalid", new double[]{Double.NaN, 1}),
            new TextLandmarkSimilarityEngine.Landmark("valid", new double[]{1, 0}));
        assertEquals("valid", TextLandmarkSimilarityEngine.nearest(new double[]{0.9, 0}, landmarks, euclidean)
            .orElseThrow().label());
        assertFalse(TextLandmarkSimilarityEngine.usable(new double[]{Double.POSITIVE_INFINITY}));
        assertFalse(TextLandmarkSimilarityEngine.usable(new double[]{}));
        assertFalse(TextLandmarkSimilarityEngine.usable(null));
    }

    @Test
    void zeroVectorsAreNotClassifiedWithCosine() {
        Metric cosine = Metric.getMetric("cosine");
        var landmarks = List.of(new TextLandmarkSimilarityEngine.Landmark("zero", new double[]{0, 0}));
        assertTrue(TextLandmarkSimilarityEngine.nearest(new double[]{0, 0}, landmarks, cosine).isEmpty());
        assertTrue(TextLandmarkSimilarityEngine.nearest(new double[]{1, 0}, landmarks, cosine).isEmpty());
    }

    @Test
    void rejectsUnsupportedMetricInputsWithoutAssigningArbitraryLabels() {
        Metric haversine = Metric.getMetric("haversine");
        assertTrue(TextLandmarkSimilarityEngine.nearest(new double[]{1, 1, 1},
            List.of(new TextLandmarkSimilarityEngine.Landmark("three-dimensional", new double[]{1, 1, 1})),
            haversine).isEmpty());
    }
}
