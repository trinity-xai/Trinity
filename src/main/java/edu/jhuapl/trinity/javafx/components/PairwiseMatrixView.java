package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.MatrixHeatmapView.MatrixClick;
import edu.jhuapl.trinity.utils.statistics.DensityCache;
import edu.jhuapl.trinity.utils.statistics.DivergenceComputer.DivergenceMetric;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixConfigPanel;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixConfigPanel.Mode;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixConfigPanel.Request;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixEngine;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixEngine.MatrixResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * PairwiseMatrixView
 * ------------------
 * Parent composite that hosts:
 *  - Left: PairwiseMatrixConfigPanel (inputs/controls)
 *  - Right: MatrixHeatmapView (rendered matrix)
 *  - Top bar: Run/Reset + Actions
 *
 * Runs PairwiseMatrixEngine for Similarity or Divergence based on the panel request
 * and updates the MatrixHeatmapView with results and initial display settings.
 *
 * This view is suitable to be wrapped by a floating pane (e.g., PairwiseMatrixPane),
 * similar to PairwiseJpdfView/Pane.
 *
 * @author Sean Phillips
 */
public final class PairwiseMatrixView extends BorderPane {

    // --- Core GUI nodes
    private final PairwiseMatrixConfigPanel configPanel;
    private final MatrixHeatmapView heatmap;

    // --- Split layout
    private final SplitPane splitPane;
    private final VBox controlsBox;
    private final BorderPane topBar;
    private final HBox topLeft;
    private final Label progressLabel;
    private double lastDividerPos = 0.36;

    // --- Buttons (visible in toolbar)
    private final Button cfgRunBtn;
    private final Button cfgResetBtn;
    private final Button collapseBtn;

    // --- Actions (synthetic cohorts, derive by label, etc.)
    private final MenuButton actionsBtn;

    // --- State (data + cache)
    private List<FeatureVector> cohortA = new ArrayList<>();
    private List<FeatureVector> cohortB = new ArrayList<>();
    private String cohortALabel = "A";
    private String cohortBLabel = "B";
    private final DensityCache cache;

    // --- Hooks
    private Consumer<String> toastHandler;
    private Consumer<MatrixClick> onCellClickHandler;

    public PairwiseMatrixView(PairwiseMatrixConfigPanel configPanel, DensityCache cache) {
        this.cache = (cache != null) ? cache : new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
        this.configPanel = (configPanel != null) ? configPanel : new PairwiseMatrixConfigPanel();
        this.heatmap = new MatrixHeatmapView();
        this.heatmap.setCompactColumnLabels(true); // compact column labels by default

        // Hook matrix clicks to external handler if provided
        this.heatmap.setOnCellClick(click -> {
            if (onCellClickHandler != null) onCellClickHandler.accept(click);
        });

        // Buttons from config panel
        this.cfgRunBtn = this.configPanel.getRunButton();
        this.cfgResetBtn = this.configPanel.getResetButton();

        // Toolbar collapse toggle
        this.collapseBtn = new Button("Collapse Controls");
        this.collapseBtn.setOnAction(e -> toggleControlsCollapsed());

        // Actions menu (synthetic cohorts + helpers)
        this.actionsBtn = buildActionsMenu();

        // Slim toolbar (left = buttons; right = progress text)
        this.topLeft = new HBox(10, cfgResetBtn, cfgRunBtn, collapseBtn, actionsBtn);
        this.topLeft.setAlignment(Pos.CENTER_LEFT);
        this.topLeft.setPadding(new Insets(6, 8, 6, 8));

        this.progressLabel = new Label("");
        BorderPane.setAlignment(progressLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(progressLabel, new Insets(6, 8, 6, 8));

        this.topBar = new BorderPane();
        this.topBar.setLeft(topLeft);
        this.topBar.setRight(progressLabel);
        setTop(topBar);

        // Left controls box
        this.controlsBox = new VBox();
        this.controlsBox.getChildren().addAll(this.configPanel);
        VBox.setMargin(this.configPanel, new Insets(6, 8, 6, 8));
        this.controlsBox.setMinWidth(0);
        this.controlsBox.setPrefWidth(390);

        // SplitPane: left controls, right heatmap
        this.splitPane = new SplitPane(controlsBox, heatmap);
        this.splitPane.setOrientation(Orientation.HORIZONTAL);
        this.splitPane.setDividerPositions(lastDividerPos);
        this.splitPane.setBackground(Background.EMPTY);
        setCenter(splitPane);
        BorderPane.setMargin(splitPane, new Insets(6));

        // Wiring
        this.configPanel.setOnRun(this::runWithRequest);
    }

    public PairwiseMatrixView() {
        this(null, null);
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    public void setCohortA(List<FeatureVector> vectors, String label) {
        this.cohortA = (vectors == null) ? new ArrayList<>() : new ArrayList<>(vectors);
        if (label != null && !label.isBlank()) this.cohortALabel = label;
    }

    public void setCohortB(List<FeatureVector> vectors, String label) {
        this.cohortB = (vectors == null) ? new ArrayList<>() : new ArrayList<>(vectors);
        if (label != null && !label.isBlank()) this.cohortBLabel = label;
    }

    public List<FeatureVector> getCohortA() { return cohortA; }
    public List<FeatureVector> getCohortB() { return cohortB; }
    public String getCohortALabel() { return cohortALabel; }
    public String getCohortBLabel() { return cohortBLabel; }

    public PairwiseMatrixConfigPanel getConfigPanel() { return configPanel; }
    public MatrixHeatmapView getHeatmapView() { return heatmap; }
    public SplitPane getSplitPane() { return splitPane; }

    /** Optional: send toast/status to Terminal/Console integration. */
    public void setToastHandler(Consumer<String> handler) { this.toastHandler = handler; }

    /** Optional: observe cell clicks from the heatmap. */
    public void setOnCellClick(Consumer<MatrixClick> handler) { this.onCellClickHandler = handler; }

    // ---------------------------------------------------------------------
    // Core run logic
    // ---------------------------------------------------------------------

    private void runWithRequest(Request req) {
        if (req == null) { toast("No request provided.", true); return; }

        // Build a minimal JpdfRecipe from the request (bins & bounds & indices & scoring).
        JpdfRecipe recipe = buildRecipeFromRequest(req);

        heatmap.setShowLegend(req.showLegend);
        if (req.paletteDiverging) heatmap.useDivergingPalette(req.divergingCenter != null ? req.divergingCenter : 0.0);
        else heatmap.useSequentialPalette();

        if (req.autoRange) heatmap.setAutoRange(true);
        else heatmap.setFixedRange(
                req.fixedMin != null ? req.fixedMin : 0.0,
                req.fixedMax != null ? req.fixedMax : 1.0
        );

        setControlsDisabled(true);
        setProgressText("Running…");

        // Run off FX thread (computations can be non-trivial)
        new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                MatrixResult result;

                if (req.mode == Mode.SIMILARITY) {
                    if (cohortA == null || cohortA.isEmpty()) {
                        Platform.runLater(() -> {
                            setControlsDisabled(false);
                            setProgressText("");
                            toast("Cohort A is empty. Load or set vectors first.", true);
                        });
                        return;
                    }
                    result = PairwiseMatrixEngine.computeSimilarityMatrix(
                            cohortA, recipe, req.includeDiagonal
                    );
                } else {
                    // DIVERGENCE
                    if (cohortA == null || cohortA.isEmpty()) {
                        Platform.runLater(() -> {
                            setControlsDisabled(false);
                            setProgressText("");
                            toast("Cohort A is empty. Load or set vectors first.", true);
                        });
                        return;
                    }
                    if (cohortB == null || cohortB.isEmpty()) {
                        Platform.runLater(() -> {
                            setControlsDisabled(false);
                            setProgressText("");
                            toast("Cohort B is empty. Load or set vectors first.", true);
                        });
                        return;
                    }
                    result = PairwiseMatrixEngine.computeDivergenceMatrix(
                            cohortA, cohortB, recipe, nonNull(req.divergenceMetric, DivergenceMetric.JS), cache
                    );
                }

                long wall = System.currentTimeMillis() - start;

                Platform.runLater(() -> {
                    if (result == null || result.matrix == null || result.matrix.length == 0) {
                        heatmap.setMatrix((double[][]) null);
                        toast("No matrix produced.", true);
                    } else {
                        heatmap.setMatrix(result.matrix);
                        heatmap.setRowLabels(result.labels);
                        heatmap.setColLabels(result.labels);
                        heatmap.setCompactColumnLabels(true); // ensure compact columns after labels set
                        toast((result.title != null ? result.title + " — " : "") +
                              "N=" + result.matrix.length + "; wall=" + wall + " ms.", false);
                    }
                    setProgressText("");
                    setControlsDisabled(false);
                });
            } catch (Throwable t) {
                Platform.runLater(() -> {
                    setControlsDisabled(false);
                    setProgressText("");
                    toast("Matrix run failed: " + t.getClass().getSimpleName() + " - " + String.valueOf(t.getMessage()), true);
                });
            }
        }, "PairwiseMatrixView-Worker").start();
    }

    private static <T> T nonNull(T v, T def) { return (v != null ? v : def); }

    private JpdfRecipe buildRecipeFromRequest(Request req) {
        // We keep this minimal—engines only use bins/bounds/policy/indices/scoreMetric.
        // For similarity path we include the requested ScoreMetric; divergence ignores score metric.
        JpdfRecipe.Builder b = JpdfRecipe.newBuilder(safeName(req.name))
                .bins(req.binsX, req.binsY)
                .boundsPolicy(req.boundsPolicy)
                .canonicalPolicyId(req.canonicalPolicyId == null ? "default" : req.canonicalPolicyId)
                .componentPairsMode(true) // not used directly here, but harmless
                .componentIndexRange(req.componentStart, req.componentEnd)
                .includeSelfPairs(req.includeDiagonal) // used by similarity engine as hint
                .orderedPairs(req.orderedPairs)
                .cacheEnabled(true)                    // let Divergence/Comparison leverage cache if passed
                .saveThumbnails(false)
                .minAvgCountPerCell(3.0);              // sufficiency default used by PairScorer

        if (req.mode == Mode.SIMILARITY && req.similarityMetric != null) {
            b.scoreMetric(req.similarityMetric);
        } else {
            // fallback (unused for divergence)
            b.scoreMetric(JpdfRecipe.ScoreMetric.PEARSON);
        }
        return b.build();
    }

    // ---------------------------------------------------------------------
    // Actions menu (synthetic cohorts & helpers)
    // ---------------------------------------------------------------------

    private MenuButton buildActionsMenu() {
        MenuButton mb = new MenuButton("Actions");

        // Clear heatmap
        MenuItem clearItem = new MenuItem("Clear Matrix");
        clearItem.setOnAction(e -> heatmap.setMatrix((double[][]) null));

        // Synthetic cohort tools
        MenuItem copyAToB = new MenuItem("Set Cohort B = Cohort A (copy)");
        copyAToB.setOnAction(e -> {
            if (cohortA == null || cohortA.isEmpty()) { toast("Cohort A is empty.", true); return; }
            setCohortB(new ArrayList<>(cohortA), cohortALabel + " (copy)");
            toast("Cohort B set to copy of Cohort A.", false);
        });

        MenuItem splitA = new MenuItem("Split Cohort A into A/B halves");
        splitA.setOnAction(e -> {
            if (cohortA == null || cohortA.size() < 2) { toast("Need ≥2 vectors in Cohort A to split.", true); return; }
            int mid = cohortA.size() / 2;
            List<FeatureVector> first = new ArrayList<>(cohortA.subList(0, mid));
            List<FeatureVector> second = new ArrayList<>(cohortA.subList(mid, cohortA.size()));
            setCohortA(first, cohortALabel + " (early)");
            setCohortB(second, cohortALabel + " (late)");
            toast("Split A into A(early)/B(late).", false);
        });

        MenuItem bRandomGaussian = new MenuItem("Set Cohort B = Gaussian noise (match N, D)");
        bRandomGaussian.setOnAction(e -> {
            if (cohortA == null || cohortA.isEmpty()) { toast("Cohort A is empty.", true); return; }
            List<FeatureVector> b = synthGaussianLikeA(cohortA, 0.0, 1.0);
            setCohortB(b, "Gaussian");
            toast("Cohort B generated: Gaussian(μ=0,σ=1)", false);
        });

        MenuItem bRandomNoise = new MenuItem("Set Cohort B = Uniform noise (match N, D)");
        bRandomNoise.setOnAction(e -> {
            if (cohortA == null || cohortA.isEmpty()) { toast("Cohort A is empty.", true); return; }
            List<FeatureVector> b = synthUniformLikeA(cohortA, -1.0, 1.0);
            setCohortB(b, "Uniform[-1,1]");
            toast("Cohort B generated: Uniform[-1,1]", false);
        });

        MenuItem bShiftOneComp = new MenuItem("Set Cohort B = A shifted on one component…");
        bShiftOneComp.setOnAction(e -> promptShiftOneComponent());

        // Label-derived cohorts
        MenuItem cohortsByLabel = new MenuItem("Derive A/B by vector label…");
        cohortsByLabel.setOnAction(e -> promptDeriveCohortsByLabel());

        mb.getItems().addAll(
                clearItem,
                new SeparatorMenuItem(),
                copyAToB,
                splitA,
                bRandomGaussian,
                bRandomNoise,
                bShiftOneComp,
                new SeparatorMenuItem(),
                cohortsByLabel
        );
        return mb;
    }

    // ---------------------------------------------------------------------
    // Synthetic cohort helpers
    // ---------------------------------------------------------------------

    private List<FeatureVector> synthGaussianLikeA(List<FeatureVector> likeA, double mean, double stddev) {
        int n = likeA.size();
        int d = (likeA.get(0).getData() != null) ? likeA.get(0).getData().size() : 0;
        java.util.Random rng = new java.util.Random(42);
        List<FeatureVector> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ArrayList<Double> row = new ArrayList<>(d);
            for (int j = 0; j < d; j++) {
                double z = rng.nextGaussian() * stddev + mean;
                row.add(z);
            }
            // Adjust constructor if your FeatureVector differs
            out.add(new FeatureVector(row));
        }
        return out;
    }

    private List<FeatureVector> synthUniformLikeA(List<FeatureVector> likeA, double min, double max) {
        int n = likeA.size();
        int d = (likeA.get(0).getData() != null) ? likeA.get(0).getData().size() : 0;
        java.util.Random rng = new java.util.Random(43);
        double span = max - min;
        List<FeatureVector> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ArrayList<Double> row = new ArrayList<>(d);
            for (int j = 0; j < d; j++) {
                double v = min + rng.nextDouble() * span;
                row.add(v);
            }
            // Adjust constructor if your FeatureVector differs
            out.add(new FeatureVector(row));
        }
        return out;
    }

    private void promptShiftOneComponent() {
        if (cohortA == null || cohortA.isEmpty()) { toast("Cohort A is empty.", true); return; }
        int d = (cohortA.get(0).getData() != null) ? cohortA.get(0).getData().size() : 0;
        if (d == 0) { toast("Cohort A has zero-dimensional vectors.", true); return; }

        TextInputDialog compDlg = new TextInputDialog("0");
        compDlg.setTitle("Shift Component");
        compDlg.setHeaderText("Shift one component in Cohort B");
        compDlg.setContentText("Component index (0.." + (d - 1) + "):");
        var compRes = compDlg.showAndWait();
        if (compRes.isEmpty()) return;

        int idx;
        try {
            idx = Integer.parseInt(compRes.get().trim());
        } catch (Exception ex) {
            toast("Invalid component index.", true);
            return;
        }
        if (idx < 0 || idx >= d) { toast("Index out of range.", true); return; }

        TextInputDialog deltaDlg = new TextInputDialog("1.0");
        deltaDlg.setTitle("Shift Component");
        deltaDlg.setHeaderText("Shift one component in Cohort B");
        deltaDlg.setContentText("Delta to add:");
        var deltaRes = deltaDlg.showAndWait();
        if (deltaRes.isEmpty()) return;

        double delta;
        try {
            delta = Double.parseDouble(deltaRes.get().trim());
        } catch (Exception ex) {
            toast("Invalid delta.", true);
            return;
        }

        // Build B by copying A and adding delta on idx
        List<FeatureVector> b = new ArrayList<>(cohortA.size());
        for (FeatureVector fv : cohortA) {
            List<Double> src = fv.getData();
            ArrayList<Double> row = new ArrayList<>(src.size());
            for (int j = 0; j < src.size(); j++) {
                double v = src.get(j);
                row.add(j == idx ? v + delta : v);
            }
            // Adjust constructor if your FeatureVector differs
            b.add(new FeatureVector(row));
        }
        setCohortB(b, cohortALabel + " (shifted comp " + idx + " by " + delta + ")");
        toast("Cohort B = A with comp " + idx + " shifted by " + delta, false);
    }

    private void promptDeriveCohortsByLabel() {
        if (cohortA == null || cohortA.isEmpty()) {
            toast("Load a FeatureCollection (A) first; we’ll derive A/B subsets from it.", true);
            return;
        }
        TextInputDialog aDlg = new TextInputDialog("classA");
        aDlg.setTitle("Cohorts by Label");
        aDlg.setHeaderText("Derive cohorts from vector labels in Cohort A");
        aDlg.setContentText("Label for Cohort A:");
        var aRes = aDlg.showAndWait();
        if (aRes.isEmpty()) return;

        TextInputDialog bDlg = new TextInputDialog("classB");
        bDlg.setTitle("Cohorts by Label");
        bDlg.setHeaderText("Derive cohorts from vector labels in Cohort A");
        bDlg.setContentText("Label for Cohort B:");
        var bRes = bDlg.showAndWait();
        if (bRes.isEmpty()) return;

        String la = aRes.get().trim();
        String lb = bRes.get().trim();
        if (la.isEmpty() || lb.isEmpty()) { toast("Labels cannot be empty.", true); return; }

        List<FeatureVector> aList = new ArrayList<>();
        List<FeatureVector> bList = new ArrayList<>();

        for (FeatureVector fv : cohortA) {
            String lab = safeVectorLabel(fv);
            if (la.equals(lab)) aList.add(fv);
            else if (lb.equals(lab)) bList.add(fv);
        }

        if (aList.isEmpty() || bList.isEmpty()) {
            toast("No matches for one or both labels. A=" + aList.size() + ", B=" + bList.size(), true);
            return;
        }

        setCohortA(new ArrayList<>(aList), la);
        setCohortB(new ArrayList<>(bList), lb);
        toast("Derived cohorts by label: A=" + la + " (" + aList.size() + "), B=" + lb + " (" + bList.size() + ")", false);
    }

    private static String safeVectorLabel(FeatureVector fv) {
        try {
            // Adjust if your FeatureVector exposes labels differently.
            String lbl = fv.getLabel(); // e.g., fv.getLabel() or fv.getName() or metadata map
            return (lbl == null) ? "" : lbl.trim();
        } catch (Throwable t) {
            return "";
        }
    }

    // ---------------------------------------------------------------------
    // Toolbar helpers
    // ---------------------------------------------------------------------

    private void toggleControlsCollapsed() {
        double pos = splitPane.getDividerPositions()[0];
        if (pos > 0.02 || controlsBox.isVisible()) {
            lastDividerPos = pos;
            controlsBox.setManaged(false);
            controlsBox.setVisible(false);
            splitPane.setDividerPositions(0.0);
            collapseBtn.setText("Expand Controls");
        } else {
            controlsBox.setManaged(true);
            controlsBox.setVisible(true);
            double restore = (lastDividerPos <= 0.02) ? 0.36 : lastDividerPos;
            splitPane.setDividerPositions(restore);
            collapseBtn.setText("Collapse Controls");
        }
    }

    private void setControlsDisabled(boolean disabled) {
        topBar.setDisable(disabled);
        configPanel.setDisable(disabled);
    }

    private void setProgressText(String text) {
        progressLabel.setText(text == null ? "" : text);
    }

    private void toast(String msg, boolean isError) {
        if (toastHandler != null) {
            toastHandler.accept((isError ? "[Error] " : "") + msg);
        } else {
            System.out.println((isError ? "[Error] " : "[Info] ") + msg);
        }
    }

    private static String safeName(String s) {
        String x = (s == null) ? "" : s.trim();
        return x.isEmpty() ? "PairwiseMatrix" : x;
    }
}
