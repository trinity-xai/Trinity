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
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
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
 *  - Top bar: Run/Reset + Actions (+ quick A/B helpers)
 *
 * Runs PairwiseMatrixEngine for Similarity or Divergence based on the panel request
 * and updates the MatrixHeatmapView with results and initial display settings.
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

    // Quick cohort helpers
    private Button btnBEqualsA;
    private Button btnSplitA;

    // --- Actions (extensible)
    private final MenuButton actionsBtn;

    // --- State (data + cache)
    private List<FeatureVector> cohortA = new ArrayList<>();
    private List<FeatureVector> cohortB = new ArrayList<>();
    private String cohortALabel = "A";
    private String cohortBLabel = "B";
    private final DensityCache cache;

    // Divergence preflight option
    private boolean autoFillBFromAWhenMissing = true;

    // --- Hooks
    private Consumer<String> toastHandler;
    private Consumer<MatrixClick> onCellClickHandler;

    public PairwiseMatrixView(PairwiseMatrixConfigPanel configPanel, DensityCache cache) {
        this.cache = (cache != null) ? cache : new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
        this.configPanel = (configPanel != null) ? configPanel : new PairwiseMatrixConfigPanel();
        this.heatmap = new MatrixHeatmapView();

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

        // Actions menu (+ divergence helpers)
        this.actionsBtn = buildActionsMenu();

        // Quick A/B helper buttons
        this.btnBEqualsA = new Button("B = A");
        this.btnBEqualsA.setOnAction(e -> useCohortAForB(null));
        this.btnSplitA = new Button("Split A→B");
        this.btnSplitA.setOnAction(e -> splitACohortIntoAandB());

        // Slim toolbar (left = buttons; right = progress text)
        this.topLeft = new HBox(10, cfgResetBtn, cfgRunBtn, collapseBtn, actionsBtn, btnBEqualsA, btnSplitA);
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

        // Build minimal recipe from UI request
        JpdfRecipe recipe = buildRecipeFromRequest(req);

        // Heatmap display settings
        heatmap.setShowLegend(req.showLegend);
        if (req.paletteDiverging) heatmap.useDivergingPalette(req.divergingCenter != null ? req.divergingCenter : 0.0);
        else heatmap.useSequentialPalette();

        if (req.autoRange) {
            heatmap.setAutoRange(true);
        } else {
            heatmap.setFixedRange(
                    req.fixedMin != null ? req.fixedMin : 0.0,
                    req.fixedMax != null ? req.fixedMax : 1.0
            );
        }

        // Preflight: ensure data availability
        if (cohortA == null || cohortA.isEmpty()) {
            toast("Cohort A is empty. Load or set vectors first.", true);
            return;
        }
        if (req.mode == Mode.DIVERGENCE) {
            if (!ensureBForDivergenceIfNeeded()) {
                // ensureB already toasts a message
                return;
            }
        }

        setControlsDisabled(true);
        setProgressText("Running…");

        // Run off FX thread (computations can be non-trivial)
        new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                MatrixResult result;

                if (req.mode == Mode.SIMILARITY) {
                    result = PairwiseMatrixEngine.computeSimilarityMatrix(
                            cohortA, recipe, req.includeDiagonal
                    );
                } else {
                    // Divergence
                    result = PairwiseMatrixEngine.computeDivergenceMatrix(
                            cohortA, cohortB, recipe,
                            nonNull(req.divergenceMetric, DivergenceMetric.JS),
                            cache
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
        // Engines use bins/bounds/policy/indices and (for similarity) scoreMetric.
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
            b.scoreMetric(JpdfRecipe.ScoreMetric.PEARSON);
        }
        return b.build();
    }

    // ---------------------------------------------------------------------
    // Toolbar + Actions
    // ---------------------------------------------------------------------

    private MenuButton buildActionsMenu() {
        MenuButton mb = new MenuButton("Actions");

        // Clear matrix
        MenuItem clearItem = new MenuItem("Clear Matrix");
        clearItem.setOnAction(e -> heatmap.setMatrix((double[][]) null));

        // Divergence helpers
        CheckMenuItem autoFillBItem = new CheckMenuItem("Auto-fill B from A when missing");
        autoFillBItem.setSelected(autoFillBFromAWhenMissing);
        autoFillBItem.selectedProperty().addListener((obs, ov, nv) -> autoFillBFromAWhenMissing = nv);

        MenuItem useAasBItem = new MenuItem("Set B = A (copy)");
        useAasBItem.setOnAction(e -> useCohortAForB(null));

        MenuItem splitAItem = new MenuItem("Split A into A/B");
        splitAItem.setOnAction(e -> splitACohortIntoAandB());

        mb.getItems().addAll(
                clearItem,
                new SeparatorMenuItem(),
                autoFillBItem,
                useAasBItem,
                splitAItem
        );
        return mb;
    }

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

    // ---------------------------------------------------------------------
    // Cohort helpers + divergence preflight
    // ---------------------------------------------------------------------

    /** Copy Cohort A into Cohort B (A vs A sanity check). */
    private void useCohortAForB(String label) {
        List<FeatureVector> a = getCohortA();
        if (a == null || a.isEmpty()) return;
        String lab = (label != null && !label.isBlank()) ? label : (getCohortALabel() + " (copy)");
        setCohortB(new ArrayList<>(a), lab);
    }

    /** Split Cohort A into two halves: first half -> A, second half -> B. */
    private void splitACohortIntoAandB() {
        List<FeatureVector> a = getCohortA();
        if (a == null || a.size() < 2) return;
        int mid = a.size() / 2;
        setCohortB(new ArrayList<>(a.subList(mid, a.size())), getCohortALabel() + " (late)");
        setCohortA(new ArrayList<>(a.subList(0, mid)), getCohortALabel() + " (early)");
    }

    /** Ensure Cohort B exists for divergence runs. Auto-fill from A if enabled. */
    private boolean ensureBForDivergenceIfNeeded() {
        if (cohortB != null && !cohortB.isEmpty()) return true;
        if (autoFillBFromAWhenMissing) {
            useCohortAForB(getCohortALabel() + " (copy)");
            toast("Cohort B was empty — auto-filled with a copy of A for testing.", false);
            return true;
        } else {
            toast("Cohort B is empty. Set B or enable 'Auto-fill B from A' in Actions.", true);
            return false;
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
