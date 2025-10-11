package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.PairwiseJpdfView;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceGridEvent;
import edu.jhuapl.trinity.utils.statistics.DensityCache;
import edu.jhuapl.trinity.utils.statistics.GridDensityResult;
import edu.jhuapl.trinity.utils.statistics.JpdfBatchEngine;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe;
import edu.jhuapl.trinity.utils.statistics.PairwiseJpdfConfigPanel;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.List;

public final class PairwiseJpdfPane extends LitPathPane {

    private final PairwiseJpdfView view;

    public PairwiseJpdfPane(
            Scene scene,
            Pane parent,
            JpdfBatchEngine engine,
            DensityCache cache,
            PairwiseJpdfConfigPanel configPanel
    ) {
        super(scene, parent,
                1100, 760,
                new PairwiseJpdfView(engine, cache, configPanel),
                "Pairwise Joint Densities", "Batch",
                420.0, 400.0);

        this.view = (PairwiseJpdfView) this.contentPane;

        // Wire up NEW_FEATURE_COLLECTION event to update Cohort A
        scene.addEventHandler(FeatureVectorEvent.NEW_FEATURE_COLLECTION, e -> {
            if (e.object instanceof FeatureCollection fc && fc.getFeatures() != null) {
                view.setCohortA(fc.getFeatures(), "A");
                view.toast("Loaded " + fc.getFeatures().size() + " vectors into Cohort A.", false);
            }
        });

        // Wire up cell click to open in 3D
        view.setOnCellClick(item -> {
            if (item == null || item.res == null) return;
            GridDensityResult res = item.res;
            var gridList = res.pdfAsListGrid();
            scene.getRoot().fireEvent(new HypersurfaceGridEvent(
                    HypersurfaceGridEvent.RENDER_PDF,
                    gridList,
                    res.getxCenters(),
                    res.getyCenters(),
                    item.xLabel + " | " + item.yLabel + " (PDF)"
            ));
            view.toast("Opened PDF in 3D.", false);
        });

        // Wire up toast to send to terminal
        view.setToastHandler(msg -> {
            Platform.runLater(() -> scene.getRoot().fireEvent(
                new CommandTerminalEvent(msg, new Font("Consolas", 18), Color.LIGHTGREEN)));
        });
    }

    public PairwiseJpdfPane(Scene scene, Pane parent) {
        this(scene, parent, null, null, null);
    }

    // --- Forwarders for public API compatibility ---
    public void setCohortA(List<FeatureVector> vectors, String label) {
        view.setCohortA(vectors, label);
    }
    public void setCohortB(List<FeatureVector> vectors, String label) {
        view.setCohortB(vectors, label);
    }
    public void runWithRecipe(JpdfRecipe recipe) {
        view.runWithRecipe(recipe);
    }
    public PairwiseJpdfView getView() {
        return view;
    }

    @Override
    public void maximize() {
        scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.POPOUT_PAIRWISEJPDF_JPDF, Boolean.TRUE)
        );
    }
}
