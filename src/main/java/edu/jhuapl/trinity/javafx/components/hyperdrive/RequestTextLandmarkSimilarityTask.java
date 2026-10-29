package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsTextListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.utils.metric.Metric;
import javafx.application.Platform;
import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;

/** Computes classifications off-thread; only the FX-thread success handler applies labels. */
public class RequestTextLandmarkSimilarityTask extends HyperdriveTask {
    public record Target(EmbeddingsTextListItem item, String originalLabel, List<Double> originalData,
                         double[] values) { }
    public record Classification(Target target, String landmarkLabel) { }

    private final List<Target> targets;
    private final List<TextLandmarkSimilarityEngine.Landmark> landmarks;
    private final Metric metric;
    private final List<Classification> classifications = new ArrayList<>();
    private int skipped;

    public RequestTextLandmarkSimilarityTask(Scene scene, CircleProgressIndicator indicator,
                                             List<Target> targets,
                                             List<TextLandmarkSimilarityEngine.Landmark> landmarks,
                                             Metric metric) {
        super(scene, indicator, null, null);
        this.targets = List.copyOf(targets);
        this.landmarks = List.copyOf(landmarks);
        this.metric = metric;
    }

    @Override
    protected void processTask() {
        int total = targets.size();
        for (int i = 0; i < total; i++) {
            if (isCancelled()) return;
            Target target = targets.get(i);
            var match = TextLandmarkSimilarityEngine.nearest(target.values(), landmarks, metric);
            if (match.isPresent()) classifications.add(new Classification(target, match.get().label()));
            else skipped++;
            int completed = i + 1;
            if (progressIndicator != null && (completed == total || completed % 50 == 0)) {
                Platform.runLater(() -> {
                    if (isCancelled()) return;
                    progressIndicator.setPercentComplete((double) completed / total);
                    progressIndicator.setLabelLater("Compared " + completed + " of " + total);
                });
            }
        }
    }

    public List<Classification> getClassifications() { return List.copyOf(classifications); }
    public int getSkipped() { return skipped; }
}
