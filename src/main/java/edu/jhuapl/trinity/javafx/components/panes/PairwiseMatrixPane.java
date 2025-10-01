package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.PairwiseMatrixView;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.utils.graph.GraphLayoutParams;
import edu.jhuapl.trinity.utils.statistics.DensityCache;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixConfigPanel;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * PairwiseMatrixPane
 * ------------------
 * Floating pane wrapper that hosts a {@link PairwiseMatrixView} and integrates
 * with Trinity’s event bus (e.g., NEW_FEATURE_COLLECTION → Cohort A) and terminal toasts.
 *
 * This mirrors the structure of PairwiseJpdfPane.
 *
 * @author Sean Phillips
 */
public final class PairwiseMatrixPane extends LitPathPane {

    private final PairwiseMatrixView view;

    public PairwiseMatrixPane(
            Scene scene,
            Pane parent,
            DensityCache cache,
            PairwiseMatrixConfigPanel configPanel
    ) {
        super(scene, parent,
                1100, 760,
                new PairwiseMatrixView(configPanel, cache),
                "Pairwise Matrices", "Matrix",
                420.0, 400.0);

        this.view = (PairwiseMatrixView) this.contentPane;

        // Event: load Cohort A from FeatureCollection
        scene.addEventHandler(FeatureVectorEvent.NEW_FEATURE_COLLECTION, e -> {
            if (e.object instanceof FeatureCollection fc && fc.getFeatures() != null) {
                view.setCohortA(fc.getFeatures(), "A");
                toast("Loaded " + fc.getFeatures().size() + " vectors into Cohort A.", false);
            }
        });
        scene.addEventHandler(GraphEvent.GRAPH_REBUILD_PARAMS, e -> {
            GraphLayoutParams p = (GraphLayoutParams) e.object; 
            view.triggerGraphBuildWithParams(p); 
        });        

        // wire matrix cell clicks (i,j,value). 
        view.setOnCellClick(click -> {
            if (click == null) return;
            // Use the latest request the user executed (or rebuild one)
            var req = view.getLastRequestOrBuild();
            view.renderPdfForCellUsingEngine(click.row, click.col,req);
            String msg = "Cell (" + click.row + "," + click.col + ") = " + click.value;
            toast(msg, false);
        });
        // Forward view toasts to the command terminal
        view.setToastHandler(msg -> {
            Platform.runLater(() -> scene.getRoot().fireEvent(
                new CommandTerminalEvent(msg, new Font("Consolas", 18), Color.LIGHTGREEN)));
        });
    }

    public PairwiseMatrixPane(Scene scene, Pane parent) {
        this(scene, parent, null, null);
    }

    // --- Forwarders for convenience (match your JPDF pane’s public API style) ---

    public void setCohortA(List<FeatureVector> vectors, String label) {
        view.setCohortA(vectors, label);
    }

    public void setCohortB(List<FeatureVector> vectors, String label) {
        view.setCohortB(vectors, label);
    }

    public PairwiseMatrixView getView() {
        return view;
    }
    // --- Helpers to fabricate/prepare Cohort B ---

    /** Copy Cohort A into Cohort B (A vs A sanity check). */
    public void useCohortAForB(String label) {
        var a = view.getCohortA();
        if (a == null || a.isEmpty()) return;
        // Shallow copy of the list is fine for test purposes
        setCohortB(new java.util.ArrayList<>(a),
                (label != null && !label.isBlank()) ? label : view.getCohortALabel() + " (copy)");
    }

    /** Split Cohort A into two halves: first half -> A, second half -> B. */
    public void splitACohortIntoAandB() {
        var a = view.getCohortA();
        if (a == null || a.size() < 2) return;
        int mid = a.size() / 2;
        setCohortB(new java.util.ArrayList<>(a.subList(mid, a.size())), view.getCohortALabel() + " (late)");
        setCohortA(new java.util.ArrayList<>(a.subList(0, mid)), view.getCohortALabel() + " (early)");
    }
    

    @Override
    public void maximize() {
        // You mentioned you'll wire the pop-out event yourself later.
        // Keeping default behavior; call super to preserve LitPathPane's handling.
        super.maximize();
    }

    private void toast(String msg, boolean isError) {
        String text = (isError ? "[Error] " : "") + msg;
        Platform.runLater(() -> scene.getRoot().fireEvent(
                new CommandTerminalEvent(text, new Font("Consolas", 18),
                        isError ? Color.PINK : Color.LIGHTGREEN)));
    }
}
