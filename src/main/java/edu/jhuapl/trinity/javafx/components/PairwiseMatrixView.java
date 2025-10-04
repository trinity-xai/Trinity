package edu.jhuapl.trinity.javafx.components;

import com.github.trinity.supermds.SuperMDS;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.MatrixHeatmapView.MatrixClick;
import edu.jhuapl.trinity.javafx.components.dialogs.SyntheticDataDialog;
import edu.jhuapl.trinity.javafx.events.HypersurfaceGridEvent;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.BuildGraphFromMatrixTask;
import edu.jhuapl.trinity.utils.graph.GraphLayoutParams;
import edu.jhuapl.trinity.utils.graph.MatrixToGraphAdapter;
import edu.jhuapl.trinity.utils.statistics.AxisParams;
import edu.jhuapl.trinity.utils.statistics.CanonicalGridPolicy;
import edu.jhuapl.trinity.utils.statistics.DensityCache;
import edu.jhuapl.trinity.utils.statistics.DivergenceComputer.DivergenceMetric;
import edu.jhuapl.trinity.utils.statistics.GridDensityResult;
import edu.jhuapl.trinity.utils.statistics.JpdfBatchEngine;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixConfigPanel;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixConfigPanel.Mode;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixConfigPanel.Request;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixEngine;
import edu.jhuapl.trinity.utils.statistics.PairwiseMatrixEngine.MatrixResult;
import edu.jhuapl.trinity.utils.statistics.StatisticEngine;
import edu.jhuapl.trinity.utils.statistics.SyntheticMatrixFactory;
import edu.jhuapl.trinity.utils.statistics.SyntheticMatrixFactory.SyntheticMatrix;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * PairwiseMatrixView
 * ------------------
 * Hosts config (left) + heatmap (right) + toolbar.
 * Runs PairwiseMatrixEngine to produce Similarity/Divergence matrices.
 * Also renders per-cell JPDF surfaces (Similarity) or ΔPDF (Divergence) into Hypersurface3DPane.
 *
 * This version reuses your existing Jpdf pipeline and supports synthetic matrices by
 * adopting fallback cohorts (carried by SyntheticMatrix) when needed.
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

    // If user loaded a synthetic matrix, we may get underlying vectors—stash them here.
    private List<FeatureVector> fallbackCohortA;
    private List<FeatureVector> fallbackCohortB;

    // What the current heatmap represents (used to pick weight mapping for graph)
    MatrixToGraphAdapter.MatrixKind currentMatrixKind = MatrixToGraphAdapter.MatrixKind.SIMILARITY;

    // --- Hooks
    private Consumer<String> toastHandler;
    private Consumer<MatrixClick> onCellClickHandler;

    // Remember the last executed configuration
    private PairwiseMatrixConfigPanel.Request lastRequest;

    public PairwiseMatrixView(PairwiseMatrixConfigPanel configPanel, DensityCache cache) {
        this.cache = (cache != null) ? cache : new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
        this.configPanel = (configPanel != null) ? configPanel : new PairwiseMatrixConfigPanel();
        this.heatmap = new MatrixHeatmapView();

        // Compact column labels: show indices only
        this.heatmap.setCompactColumnLabels(true);

        // Cell click → render JPDF (Similarity) or ΔPDF (Divergence)
        this.heatmap.setOnCellClick(click -> {
            if (click == null) return;

            try {
                Request effective = getLastRequestOrBuild();
                if (effective == null) {
                    toast("No configuration to render (run once or provide defaults).", true);
                } else {
                    // Adopt fallback cohorts if needed based on the expected mode
                    ensureCohortsFromFallbackIfAvailable(effective.mode == Mode.DIVERGENCE);

                    if (effective.mode == Mode.SIMILARITY) {
                        if (cohortA == null || cohortA.isEmpty()) {
                            toast("Cohort A is empty and no synthetic cohorts were provided.", true);
                            return;
                        }
                        renderPdfForCellUsingEngine(click.row, click.col, effective);
                    } else {
                        if (cohortA == null || cohortA.isEmpty() || cohortB == null || cohortB.isEmpty()) {
                            toast("Cohorts A/B are empty and no synthetic cohorts were provided.", true);
                            return;
                        }
                        renderDeltaPdfForCellUsingEngine(click.row, click.col, effective);
                    }
                }
            } catch (Throwable t) {
                toast("Cell render failed: " + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()), true);
            }

            // still notify any external listener you set via setOnCellClick(...)
            if (onCellClickHandler != null) onCellClickHandler.accept(click);
        });

        // Buttons from config panel
        this.cfgRunBtn = this.configPanel.getRunButton();
        this.cfgResetBtn = this.configPanel.getResetButton();

        // Toolbar collapse toggle
        collapseBtn = new Button("Collapse Controls");
        collapseBtn.setOnAction(e -> toggleControlsCollapsed());

        // Actions menu
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

    public PairwiseMatrixView() { this(null, null); }

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

    /** send toast/status to Terminal/Console integration. */
    public void setToastHandler(Consumer<String> handler) { this.toastHandler = handler; }

    /** observe cell clicks from the heatmap. */
    public void setOnCellClick(Consumer<MatrixClick> handler) { this.onCellClickHandler = handler; }

    // ---------------------------------------------------------------------
    // Core run logic
    // ---------------------------------------------------------------------

    private void runWithRequest(Request req) {
        if (req == null) { toast("No request provided.", true); return; }
        this.lastRequest = req; // cache config used for "Run"

        // Build recipe used by engine runs (for matrix building; per-click builds a single-pair recipe)
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
                        currentMatrixKind = (req.mode == Mode.SIMILARITY)
                                ? MatrixToGraphAdapter.MatrixKind.SIMILARITY
                                : MatrixToGraphAdapter.MatrixKind.DIVERGENCE;
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

    public PairwiseMatrixConfigPanel.Request getLastRequestOrBuild() {
        if (lastRequest != null) return lastRequest;
        return (configPanel != null ? configPanel.snapshotRequest() : null);
    }

    // ---------------------------------------------------------------------
    // Cell-click → JPDF surface (Similarity)
    // ---------------------------------------------------------------------

    public void renderPdfForCellUsingEngine(int i, int j, Request req) {
        if (req == null) { toast("No configuration available to build JPDF.", true); return; }
        // Adopt fallbacks if present
        ensureCohortsFromFallbackIfAvailable(false);
        if (cohortA == null || cohortA.isEmpty()) { toast("Cohort A is empty.", true); return; }

        // (1) Build a single-pair whitelist recipe from the current request
        JpdfRecipe recipe = buildSinglePairWhitelistRecipe(req, i, j);

        // (2) Resolve policy + cache
        CanonicalGridPolicy policy = resolveCanonicalPolicy(recipe);
        DensityCache useCache = resolveDensityCache();

        // (3) Run for just this pair (Similarity uses A only)
        JpdfBatchEngine.runWhitelistPairs(
                cohortA,
                recipe,
                policy,
                useCache,
                (pair) -> {
                    GridDensityResult gdr = pair.density;
                    List<List<Double>> zGrid = gdr.pdfAsListGrid();
                    getScene().getRoot().fireEvent(new HypersurfaceGridEvent(
                        HypersurfaceGridEvent.RENDER_PDF,
                        zGrid,
                        gdr.getxCenters(),
                        gdr.getyCenters(),
                        "Comp " + Math.min(i, j) + " | Comp " + Math.max(i, j) + " (PDF)"
                    ));
                }
        );

        toast("Rendered JPDF for pair (" + i + "," + j + ").", false);
    }

    // ---------------------------------------------------------------------
    // Cell-click → ΔPDF surface (Divergence: A − B)
    // ---------------------------------------------------------------------
    private void renderDeltaPdfForCellUsingEngine(int i, int j, Request req) {
        if (req == null) { toast("No configuration available to build ΔPDF.", true); return; }
        // Adopt fallbacks if present
        ensureCohortsFromFallbackIfAvailable(true);
        if (cohortA == null || cohortA.isEmpty()) { toast("Cohort A is empty.", true); return; }
        if (cohortB == null || cohortB.isEmpty()) { toast("Cohort B is empty.", true); return; }

        // (1) Pair-specific recipe (single pair via WHITELIST)
        JpdfRecipe recipe = buildSinglePairWhitelistRecipe(req, i, j);

        // (2) Resolve policy + cache
        CanonicalGridPolicy policy = resolveCanonicalPolicy(recipe);
        DensityCache useCache = resolveDensityCache();

        // (3) Run A and B separately, grab the single job result from each batch
        JpdfBatchEngine.BatchResult batchA =
            JpdfBatchEngine.runWhitelistPairs(cohortA, recipe, policy, useCache);
        JpdfBatchEngine.BatchResult batchB =
            JpdfBatchEngine.runWhitelistPairs(cohortB, recipe, policy, useCache);

        if (batchA.jobs.isEmpty() || batchB.jobs.isEmpty()
            || batchA.jobs.get(0).density == null || batchB.jobs.get(0).density == null) {
            toast("Could not compute ΔPDF for (" + i + "," + j + ").", true);
            return;
        }

        GridDensityResult a = batchA.jobs.get(0).density;
        GridDensityResult b = batchB.jobs.get(0).density;

        // (4) Align grids if necessary; then out = A.pdf - B.pdf
        double[] ax = a.getxCenters(), ay = a.getyCenters();
        double[] bx = b.getxCenters(), by = b.getyCenters();

        List<List<Double>> out;
        if (sameCenters(ax, bx) && sameCenters(ay, by)) {
            out = subtract(a.pdfAsListGrid(), b.pdfAsListGrid());
        } else {
            out = subtract(a.pdfAsListGrid(), bilinearResample(b.pdfAsListGrid(), bx, by, ax, ay));
        }

        // (5) Render into Hypersurface3DPane (your working event)
        getScene().getRoot().fireEvent(new HypersurfaceGridEvent(
            HypersurfaceGridEvent.RENDER_PDF,
            out,
            ax,
            ay,
            "Comp " + Math.min(i, j) + " | Comp " + Math.max(i, j) + " (ΔPDF A−B)"
        ));

        toast("Rendered ΔPDF (A−B) for pair (" + i + "," + j + ").", false);
    }

    // ---------------------------------------------------------------------
    // Graph build actions
    // ---------------------------------------------------------------------

    private MenuButton buildActionsMenu() {
        MenuButton mb = new MenuButton("Actions");

        MenuItem clearItem = new MenuItem("Clear Matrix");
        clearItem.setOnAction(e -> heatmap.setMatrix((double[][]) null));

        MenuItem buildGraphFromSim = new MenuItem("Build Graph from Similarity");
        buildGraphFromSim.setOnAction(e -> triggerGraphBuild(MatrixToGraphAdapter.MatrixKind.SIMILARITY));

        MenuItem buildGraphFromDiv = new MenuItem("Build Graph from Divergence");
        buildGraphFromDiv.setOnAction(e -> triggerGraphBuild(MatrixToGraphAdapter.MatrixKind.DIVERGENCE));

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

        // Synthetic Data Controller
        MenuItem syntheticDataDialogItem = new MenuItem("Synthetic Data Controller");
        syntheticDataDialogItem.setOnAction(e -> {
            SyntheticDataDialog dlg = new SyntheticDataDialog();
            dlg.initOwner(getScene().getWindow());
            dlg.showAndWait().ifPresent(res -> {
                switch (res.kind()) {
                    case SIMILARITY_MATRIX, DIVERGENCE_MATRIX -> loadSyntheticMatrix(res.matrix());
                    case COHORTS -> setCohorts(res.cohorts().cohortA, "A", res.cohorts().cohortB, "B");
                }
            });
        });

        mb.getItems().addAll(buildGraphFromSim, buildGraphFromDiv,
                new SeparatorMenuItem(), copyAToB, splitA, bShiftOneComp, cohortsByLabel,
                new SeparatorMenuItem(), bRandomGaussian, bRandomNoise,
                new SeparatorMenuItem(), syntheticDataDialogItem, clearItem);

        return mb;
    }

    public void triggerGraphBuildWithParams(GraphLayoutParams layout) {
        double[][] M = currentMatrix();
        if (M == null || M.length == 0) { toast("No matrix to build a graph from.", true); return; }

        var labels = currentRowLabels();
        var scene = getScene();
        if (scene == null) { toast("Scene not ready.", true); return; }

        // Choose weight mapping here (not in params)
        MatrixToGraphAdapter.WeightMode wmode =
                (currentMatrixKind == MatrixToGraphAdapter.MatrixKind.SIMILARITY)
                        ? MatrixToGraphAdapter.WeightMode.DIRECT
                        : MatrixToGraphAdapter.WeightMode.INVERSE_FOR_DIVERGENCE;

        SuperMDS.Params mdsParams = new SuperMDS.Params();
        mdsParams.outputDim = 3;

        setControlsDisabled(true);
        setProgressText("Building graph…");

        BuildGraphFromMatrixTask task = new BuildGraphFromMatrixTask(
                scene, M, labels, currentMatrixKind, wmode, layout, mdsParams);

        task.setOnSucceeded(ev -> { setControlsDisabled(false); setProgressText(""); toast("Graph built and dispatched.", false); });
        task.setOnFailed(ev -> { setControlsDisabled(false); setProgressText(""); var t = task.getException(); toast("Graph build failed: " + (t == null ? "unknown error" : t.getMessage()), true); });

        Thread th = new Thread(task, "BuildGraphFromMatrixTask"); th.setDaemon(true); th.start();
    }

    private void triggerGraphBuild(MatrixToGraphAdapter.MatrixKind kind) {
        currentMatrixKind = kind;
        GraphLayoutParams layout = new GraphLayoutParams()
                .withKind(GraphLayoutParams.LayoutKind.MDS_3D) // or FORCE_FR, etc.
                .withRadius(600)
                .withEdgePolicy(GraphLayoutParams.EdgePolicy.KNN)
                .withKnnK(8)
                .withKnnSymmetrize(true)
                .withMaxEdges(5000)
                .withMaxDegreePerNode(32)
                .withNormalizeWeights01(true);

        triggerGraphBuildWithParams(layout);
    }

    // ---------------------------------------------------------------------
    // Synthetic matrix & cohort helpers
    // ---------------------------------------------------------------------

    /** Load a synthetic matrix (similarity or divergence) into the heatmap and stash fallback cohorts if provided. */
    public void loadSyntheticMatrix(SyntheticMatrix sm) {
        if (sm == null || sm.matrix == null || sm.matrix.length == 0) {
            toast("Synthetic matrix is empty.", true);
            heatmap.setMatrix((double[][]) null);
            return;
        }

        // Map factory kind to adapter kind
        this.currentMatrixKind = (sm.kind == MatrixToGraphAdapter.MatrixKind.DIVERGENCE)
                ? MatrixToGraphAdapter.MatrixKind.DIVERGENCE
                : MatrixToGraphAdapter.MatrixKind.SIMILARITY;

        // Render the matrix
        heatmap.setMatrix(sm.matrix);
        if (sm.labels != null && !sm.labels.isEmpty()) {
            heatmap.setRowLabels(sm.labels);
            heatmap.setColLabels(sm.labels);
        } else {
            // fallback label set
            List<String> labels = new java.util.ArrayList<>(sm.matrix.length);
            for (int i = 0; i < sm.matrix.length; i++) labels.add("F" + i);
            heatmap.setRowLabels(labels);
            heatmap.setColLabels(labels);
        }

        // Palette + range defaults based on kind
        applyHeatmapDefaultsFor(this.currentMatrixKind, sm.matrix);

        // Stash possible underlying vectors as *fallback* cohorts
        this.fallbackCohortA = sm.cohortA != null ? new ArrayList<>(sm.cohortA) : null;
        this.fallbackCohortB = sm.cohortB != null ? new ArrayList<>(sm.cohortB) : null;

        // If user hasn't loaded cohorts yet, adopt the fallbacks now (keeps UX simple)
        if ((cohortA == null || cohortA.isEmpty()) && fallbackCohortA != null) {
            setCohortA(fallbackCohortA, "A");
        }
        if ((cohortB == null || cohortB.isEmpty()) && fallbackCohortB != null) {
            setCohortB(fallbackCohortB, "B");
        }

        String title = (sm.title != null && !sm.title.isBlank()) ? sm.title : "Synthetic";
        toast(title + " — N=" + sm.matrix.length + " loaded.", false);
    }

    /** Convenience: set both cohorts at once with labels. */
    public void setCohorts(List<FeatureVector> a, String labelA, List<FeatureVector> b, String labelB) {
        setCohortA(a, labelA);
        setCohortB(b, labelB);
        int na = (a == null ? 0 : a.size());
        int nb = (b == null ? 0 : b.size());
        toast("Cohorts set: A=" + labelA + " (" + na + "), B=" + labelB + " (" + nb + ")", false);
    }

    /** Convenience overload with default labels. */
    public void setCohorts(List<FeatureVector> a, List<FeatureVector> b) {
        setCohorts(a, "A", b, "B");
    }

    /** If cohorts are empty but fallbacks are available (from synthetic data), adopt them. */
    private void ensureCohortsFromFallbackIfAvailable(boolean needB) {
        if ((cohortA == null || cohortA.isEmpty()) && fallbackCohortA != null) {
            setCohortA(fallbackCohortA, (cohortALabel == null || cohortALabel.isBlank()) ? "A" : cohortALabel);
        }
        if (needB && (cohortB == null || cohortB.isEmpty()) && fallbackCohortB != null) {
            setCohortB(fallbackCohortB, (cohortBLabel == null || cohortBLabel.isBlank()) ? "B" : cohortBLabel);
        }
    }

    /* ---------- internal helpers ---------- */

    private void applyHeatmapDefaultsFor(MatrixToGraphAdapter.MatrixKind kind, double[][] M) {
        // Legend ON, auto range ON
        heatmap.setShowLegend(true);
        heatmap.setAutoRange(true);

        // Palette: Similarity -> sequential; Divergence -> diverging centered at mid-range
        if (kind == MatrixToGraphAdapter.MatrixKind.DIVERGENCE) {
            double[] mm = minMax(M);
            double center = (mm[0] + mm[1]) * 0.5;
            heatmap.useDivergingPalette(center);
        } else {
            heatmap.useSequentialPalette();
        }
    }

    /** Returns [min,max] over finite entries. */
    private static double[] minMax(double[][] M) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < M.length; i++) {
            double[] row = M[i];
            for (int j = 0; j < row.length; j++) {
                double v = row[j];
                if (Double.isFinite(v)) {
                    if (v < min) min = v;
                    if (v > max) max = v;
                }
            }
        }
        if (!(max > min)) { // degenerate
            min = 0.0; max = 1.0;
        }
        return new double[]{min, max};
    }

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
        try { idx = Integer.parseInt(compRes.get().trim()); }
        catch (Exception ex) { toast("Invalid component index.", true); return; }
        if (idx < 0 || idx >= d) { toast("Index out of range.", true); return; }

        TextInputDialog deltaDlg = new TextInputDialog("1.0");
        deltaDlg.setTitle("Shift Component");
        deltaDlg.setHeaderText("Shift one component in Cohort B");
        deltaDlg.setContentText("Delta to add:");
        var deltaRes = deltaDlg.showAndWait();
        if (deltaRes.isEmpty()) return;

        double delta;
        try { delta = Double.parseDouble(deltaRes.get().trim()); }
        catch (Exception ex) { toast("Invalid delta.", true); return; }

        List<FeatureVector> b = new ArrayList<>(cohortA.size());
        for (FeatureVector fv : cohortA) {
            List<Double> src = fv.getData();
            ArrayList<Double> row = new ArrayList<>(src.size());
            for (int j = 0; j < src.size(); j++) {
                double v = src.get(j);
                row.add(j == idx ? v + delta : v);
            }
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
            String lbl = fv.getLabel();
            return (lbl == null) ? "" : lbl.trim();
        } catch (Throwable t) { return ""; }
    }

    // ---------------------------------------------------------------------
    // Collapse/Expand
    // ---------------------------------------------------------------------

    private void toggleControlsCollapsed() {
        if (!controlsCollapsed) {
            double pos = splitPane.getDividerPositions()[0];
            if (pos > 0.02 && pos < 0.98) lastDividerPos = pos;

            controlsCollapsed = true;
            collapseBtn.setText("Expand Controls");

            controlsBox.setPrefWidth(0);
            controlsBox.setMaxWidth(0);

            splitPane.setDividerPositions(0.0);
            splitPane.requestLayout();
        } else {
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
    private double[][] currentMatrix() { return heatmap.getMatrixCopy(); }
    private List<String> currentRowLabels() { return heatmap.getRowLabels(); }

    private static String safeName(String s) {
        String x = (s == null) ? "" : s.trim();
        return x.isEmpty() ? "PairwiseMatrix" : x;
    }

    private static <T> T nonNull(T v, T def) { return (v != null ? v : def); }

    // ---------------------------------------------------------------------
    // Jpdf recipe builders & policy/cache resolvers
    // ---------------------------------------------------------------------

    private JpdfRecipe buildRecipeFromRequest(Request req) {
        // Used for matrix construction (broad run). Clicks will use the single-pair builder below.
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

    /** Build a whitelist recipe for a single (i,j) pair. */
    private JpdfRecipe buildSinglePairWhitelistRecipe(Request req, int i, int j) {
        int lo = Math.min(i, j);
        int hi = Math.max(i, j);

        AxisParams x = new AxisParams();
        x.setType(StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION);
        x.setComponentIndex(lo);

        AxisParams y = new AxisParams();
        y.setType(StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION);
        y.setComponentIndex(hi);

        String name = safeName(req.name);
        JpdfRecipe.Builder b = JpdfRecipe.newBuilder(name)
                .pairSelection(JpdfRecipe.PairSelection.WHITELIST)
                .addAxisPair(x, y)
                .componentPairsMode(false)
                .includeSelfPairs(true)
                .orderedPairs(false)
                .bins(req.binsX, req.binsY)
                .boundsPolicy(req.boundsPolicy)
                .canonicalPolicyId(req.canonicalPolicyId == null ? "default" : req.canonicalPolicyId)
                .minAvgCountPerCell(3.0)
                .outputKind(JpdfRecipe.OutputKind.PDF_AND_CDF)
                .cacheEnabled(true);

        if (req.similarityMetric != null) b.scoreMetric(req.similarityMetric);
        if (req.cohortALabel != null || req.cohortBLabel != null) {
            b.cohortLabels(
                    req.cohortALabel != null ? req.cohortALabel : "A",
                    req.cohortBLabel != null ? req.cohortBLabel : "B"
            );
        }
        return b.build();
    }

    private CanonicalGridPolicy resolveCanonicalPolicy(JpdfRecipe recipe) {
        return CanonicalGridPolicy.get(
                (recipe.getBoundsPolicy() == JpdfRecipe.BoundsPolicy.CANONICAL_BY_FEATURE)
                        ? recipe.getCanonicalPolicyId()
                        : "default");
    }

    private DensityCache resolveDensityCache() { return this.cache; }

    // ---------------------------------------------------------------------
    // Grid math helpers for ΔPDF
    // ---------------------------------------------------------------------

    private static boolean sameCenters(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        for (int k = 0; k < a.length; k++) if (Math.abs(a[k] - b[k]) > 1e-9) return false;
        return true;
    }

    private static List<List<Double>> subtract(List<List<Double>> A, List<List<Double>> B) {
        int rows = Math.min(A.size(), B.size());
        int cols = Math.min(A.get(0).size(), B.get(0).size());
        List<List<Double>> out = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            List<Double> row = new ArrayList<>(cols);
            List<Double> ar = A.get(r);
            List<Double> br = B.get(r);
            for (int c = 0; c < cols; c++) row.add(ar.get(c) - br.get(c));
            out.add(row);
        }
        return out;
    }

    /** Bilinear-resample grid G (srcX,srcY) → (dstX,dstY). Returns |dstY| x |dstX|. */
    private static List<List<Double>> bilinearResample(List<List<Double>> G,
                                                       double[] srcX, double[] srcY,
                                                       double[] dstX, double[] dstY) {
        int h = dstY.length, w = dstX.length;
        List<List<Double>> out = new ArrayList<>(h);

        for (int r = 0; r < h; r++) {
            double y = dstY[r];
            int jy = clamp(upperFloor(srcY, y), 0, srcY.length - 2);
            double y0 = srcY[jy], y1 = srcY[jy + 1];
            double ty = safeT(y, y0, y1);

            List<Double> row = new ArrayList<>(w);
            for (int c = 0; c < w; c++) {
                double x = dstX[c];
                int ix = clamp(upperFloor(srcX, x), 0, srcX.length - 2);
                double x0 = srcX[ix], x1 = srcX[ix + 1];
                double tx = safeT(x, x0, x1);

                double f00 = G.get(jy).get(ix);
                double f10 = G.get(jy).get(ix + 1);
                double f01 = G.get(jy + 1).get(ix);
                double f11 = G.get(jy + 1).get(ix + 1);

                double f0 = lerp(f00, f10, tx);
                double f1 = lerp(f01, f11, tx);
                row.add(lerp(f0, f1, ty));
            }
            out.add(row);
        }
        return out;
    }

    /** largest k such that src[k] <= v, or 0 if v <= src[0] */
    private static int upperFloor(double[] src, double v) {
        int lo = 0, hi = src.length - 1;
        if (v <= src[0]) return 0;
        if (v >= src[hi]) return hi - 1;
        while (lo + 1 < hi) {
            int mid = (lo + hi) >>> 1;
            if (src[mid] <= v) lo = mid; else hi = mid;
        }
        return lo;
    }

    private static int clamp(int x, int lo, int hi) { return Math.max(lo, Math.min(hi, x)); }
    private static double lerp(double a, double b, double t) { return a + t * (b - a); }
    private static double safeT(double v, double a, double b) { double d = (b - a); return d == 0 ? 0.0 : (v - a) / d; }
}
