package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.Dimension;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.FeatureRadarChart;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.util.List;
import java.util.Optional;

/**
 * @author Sean Phillips
 */
public class RadarPlotPane extends LitPathPane {

    FeatureRadarChart radarChart;

    private static BorderPane createContent() {
        FeatureRadarChart radarChart = new FeatureRadarChart(FeatureVector.EMPTY_FEATURE_VECTOR("Waiting...", 20), 500, 500);
        BorderPane bpOilSpill = new BorderPane(radarChart);
        return bpOilSpill;
    }

    public RadarPlotPane(Scene scene, Pane parent) {
        super(scene, parent, 500, 500, createContent(), "Parameters ", "RADAR", 200.0, 300.0);
        this.scene = scene;
        this.scene.addEventHandler(FeatureVectorEvent.SELECT_FEATURE_VECTOR, e -> {
            Platform.runLater(() -> {
                radarChart.setLabels((List<String>) e.object2);
                setFeatureVector((FeatureVector) e.object);
            });
        });
        this.scene.addEventHandler(HyperspaceEvent.DIMENSION_LABEL_REMOVED, e -> {
            Platform.runLater(() -> {
                Dimension d = (Dimension) e.object;
                radarChart.removeLabel(d.index);
            });
        });
        this.scene.addEventHandler(HyperspaceEvent.DIMENSION_LABEL_UPDATE, e -> {
            Platform.runLater(() -> {
                Dimension d = (Dimension) e.object;
                radarChart.updateLabel(d.index, d.labelString);
            });
        });
        this.scene.addEventHandler(HyperspaceEvent.CLEARED_DIMENSION_LABELS, e -> {
            Platform.runLater(() -> {
                radarChart.clearLabels();
            });
        });
        Optional<Node> bpOpt = contentPane.getChildren().stream()
            .filter(node -> node instanceof FeatureRadarChart)
            .findFirst();
        if (bpOpt.isPresent()) {
            radarChart = (FeatureRadarChart) bpOpt.get();
        }
        // must be set to prevent user from resizing too small.
        setMinWidth(300);
        setMinHeight(300);
        fadeEnabled = false; //by default we don't want to fade out with this pane.
    }

    public void setFeatureVector(FeatureVector featureVector) {
        if (radarChart.stacking.get())
            radarChart.stackRadarPlot(featureVector);
        else
            radarChart.updateRadarPlot(featureVector);
    }
}
