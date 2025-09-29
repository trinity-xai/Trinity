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
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
 * Designed to be wrapped by a floating pane (e.g., PairwiseMatrixPane), similar to PairwiseJpdfView/Pane.
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

    // --- Collapse/expand state
    private boolean controlsCollapsed = false;
    private double lastDividerPos = 0.36;
    private double expandedControlsPrefWidth = 390.0;

    // --- Buttons (visible in toolbar)
    private final Button cfgRunBtn;
    private final Button cfgResetBtn;
    private final Button collapseBtn;

    // --- Actions (for future extensibility)
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

        // Compact column labels: show indices only (no "Comp" prefix)
        this.heatmap.setCompactColumnLabels(true);

        // Hook matrix clicks to external handler if provided
        this.heatmap.setOnCellClick(click -> {
            if (onCellClickHandler != null) onCellClickHandler.accept(click);
        });

        // Buttons from config panel
        this.cfgRunBtn = this.configPanel.getRunButton();
        this.cfgResetBtn = this.configPanel.getResetButton();

        // Toolbar collapse toggle
        collapseBtn = new Button("Collapse Controls");
        collapseBtn.setOnAction(e -> toggleControlsCollapsed());

        // Actions menu (placeholder for future save/load/export)
        actionsBtn = buildActionsMenu();

        // Slim toolbar (left = buttons; right = progress text)
        topLeft = new HBox(10, cfgResetBtn, cfgRunBtn, collapseBtn, actionsBtn);
        topLeft.setAlignment(Pos.CENTER_LEFT);
        topLeft.setPadding(new Insets(6, 8, 6, 8));

        progressLabel = new Label("");
        BorderPane.setAlignment(progressLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(progressLabel, new Insets(6, 8, 6, 8));

        topBar = new BorderPane();
        topBar.setLeft(topLeft);
        topBar.setRight(progressLabel);
        setTop(topBar);

        // Left controls box
        controlsBox = new VBox();
        controlsBox.getChildren().addAll(this.configPanel);
        VBox.setMargin(this.configPanel, new Insets(6, 8, 6, 8));

        // Make both sides freely resizable
        controlsBox.setMinWidth(0);
        controlsBox.setPrefWidth(expandedControlsPrefWidth);
        controlsBox.setMaxWidth(Region.USE_COMPUTED_SIZE);

        heatmap.setMinWidth(0);
        heatmap.setMaxWidth(Double.MAX_VALUE);

        // SplitPane: left controls, right heatmap
        splitPane = new SplitPane(controlsBox, heatmap);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(lastDividerPos);
        splitPane.setBackground(Background.EMPTY);
        setCenter(splitPane);
        BorderPane.setMargin(splitPane, new Insets(6));

        // Let SplitPane resize both children
        SplitPane.setResizableWithParent(controlsBox, Boolean.TRUE);
        SplitPane.setResizableWithParent(heatmap, Boolean.TRUE);

        // Track divider pos only when expanded; also remember an approximate expanded width
        if (!splitPane.getDividers().isEmpty()) {
            splitPane.getDividers().get(0).positionProperty().addListener((obs, ov, nv) -> {
                if (!controlsCollapsed && nv != null) {
                    double p = nv.doubleValue();
                    if (p > 0.02 && p < 0.98) {
                        lastDividerPos = p;
                        double total = splitPane.getWidth() <= 0 ? getWidth() : splitPane.getWidth();
                        if (total > 0) {
                            // Keep a sensible floor for the left panel width
                            expandedControlsPrefWidth = Math.max(220, Math.round(p * total));
                            controlsBox.setPrefWidth(expandedControlsPrefWidth);
                        }
                    }
                }
            });
        }

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

        // Heatmap visual settings from request
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
        // Engines only use bins/bounds/policy/indices/scoreMetric.
        JpdfRecipe.Builder b = JpdfRecipe.newBuilder(safeName(req.name))
                .bins(req.binsX, req.binsY)
                .boundsPolicy(req.boundsPolicy)
                .canonicalPolicyId(req.canonicalPolicyId == null ? "default" : req.canonicalPolicyId)
                .componentPairsMode(true)
                .componentIndexRange(req.componentStart, req.componentEnd)
                .includeSelfPairs(req.includeDiagonal)
                .orderedPairs(req.orderedPairs)
                .cacheEnabled(true)
                .saveThumbnails(false)
                .minAvgCountPerCell(3.0);

        if (req.mode == Mode.SIMILARITY && req.similarityMetric != null) {
            b.scoreMetric(req.similarityMetric);
        } else {
            // fallback (unused for divergence path)
            b.scoreMetric(JpdfRecipe.ScoreMetric.PEARSON);
        }
        return b.build();
    }

    // ---------------------------------------------------------------------
    // Toolbar helpers
    // ---------------------------------------------------------------------

    private MenuButton buildActionsMenu() {
        MenuButton mb = new MenuButton("Actions");

        // Simple "Clear" action for the heatmap
        MenuItem clearItem = new MenuItem("Clear Matrix");
        clearItem.setOnAction(e -> heatmap.setMatrix((double[][]) null));

        // Reserved hooks for future: Save/Load config, export image, etc.
        MenuItem sep = new SeparatorMenuItem();

        mb.getItems().addAll(clearItem, sep);
        return mb;
    }

    // ---------------------------------------------------------------------
    // Collapse/Expand (no node swapping; width-based)
    // ---------------------------------------------------------------------

    private void toggleControlsCollapsed() {
        if (!controlsCollapsed) {
            // Save current divider position if sensible
            double pos = splitPane.getDividerPositions()[0];
            if (pos > 0.02 && pos < 0.98) lastDividerPos = pos;

            // Collapse by width (keep node in SplitPane)
            controlsCollapsed = true;
            collapseBtn.setText("Expand Controls");

            controlsBox.setPrefWidth(0);
            controlsBox.setMaxWidth(0);

            splitPane.setDividerPositions(0.0);
            splitPane.requestLayout();
        } else {
            // Expand: restore widths and divider
            controlsCollapsed = false;
            collapseBtn.setText("Collapse Controls");

            controlsBox.setMaxWidth(Region.USE_COMPUTED_SIZE);
            controlsBox.setPrefWidth(expandedControlsPrefWidth);

            final double target = (lastDividerPos > 0.02 && lastDividerPos < 0.98) ? lastDividerPos : 0.36;
            Platform.runLater(() -> {
                splitPane.setDividerPositions(target);
                splitPane.requestLayout();
            });
        }
    }

    // ---------------------------------------------------------------------
    // Misc helpers
    // ---------------------------------------------------------------------

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
