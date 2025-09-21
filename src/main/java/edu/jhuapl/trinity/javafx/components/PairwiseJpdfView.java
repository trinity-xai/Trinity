package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.statistics.CanonicalGridPolicy;
import edu.jhuapl.trinity.utils.statistics.DensityCache;
import edu.jhuapl.trinity.utils.statistics.HeatmapThumbnailView;
import edu.jhuapl.trinity.utils.statistics.JpdfBatchEngine;
import edu.jhuapl.trinity.utils.statistics.JpdfBatchEngine.BatchResult;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe;
import edu.jhuapl.trinity.utils.statistics.PairGridPane;
import edu.jhuapl.trinity.utils.statistics.PairwiseJpdfConfigPanel;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.layout.Background;

/**
 * PairwiseJpdfView (SplitPane-based)
 * ----------------------------------
 * Layout:
 *   [SplitPane HORIZONTAL]
 *     ├─ Left (VBox):
 *     │    ├─ Top bar (buttons + streaming controls on the left, progress label on the right)
 *     │    ├─ Separator
 *     │    └─ PairwiseJpdfConfigPanel (2-col compact)
 *     └─ Right:
 *          └─ PairGridPane (scrollable TilePane of thumbnails)
 *
 * Streaming:
 *   - Results are appended live via the engine’s onResult callback.
 *   - Flush cadence and incremental sorting can be tuned in the top bar.
 */
public class PairwiseJpdfView extends BorderPane {

    // Core GUI nodes
    private final PairwiseJpdfConfigPanel configPanel;
    private final PairGridPane gridPane;

    // Split layout
    private final SplitPane splitPane;
    private final VBox controlsBox;           // left side of split
    private final BorderPane topBar;          // top of left side
    private final HBox topLeft;               // buttons + streaming controls (left)
    private final Label progressLabel;        // progress (right)
    private double lastDividerPos = 0.36;     // remembered position for collapse/expand

    // Buttons
    private final Button cfgResetBtn;
    private final Button cfgRunBtn;
    private final Button runExternalBtn;
    private final Button clearBtn;
    private final Button collapseBtn;

    // Streaming controls
    private final Spinner<Integer> flushSizeSpinner;      // how many completed items per UI flush
    private final Spinner<Integer> flushIntervalSpinner;  // how many ms between forced UI flushes
    private final CheckBox incrementalSortCheck;          // optional incremental sorting

    // Cache (always non-null)
    private final DensityCache cache;

    // State
    private List<FeatureVector> cohortA = new ArrayList<>();
    private List<FeatureVector> cohortB = new ArrayList<>();
    private String cohortALabel = "A";
    private String cohortBLabel = "B";

    private Supplier<JpdfRecipe> recipeSupplier;    // optional for "Run (external)"
    private Consumer<String> toastHandler;          // optional for user messages
    private Consumer<PairGridPane.PairItem> onCellClickHandler;

    // Background worker ref (optional cancellation)
    private volatile Thread workerThread;

    public PairwiseJpdfView(JpdfBatchEngine engine, DensityCache cache, PairwiseJpdfConfigPanel configPanel) {
        // Engine is used indirectly via static calls; keep cache & config
        this.cache = (cache != null) ? cache : new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
        this.configPanel = (configPanel != null) ? configPanel : new PairwiseJpdfConfigPanel();

        // Grid (right side)
        this.gridPane = new PairGridPane();
        this.gridPane.setOnCellClick(click -> {
            if (onCellClickHandler != null) onCellClickHandler.accept(click.item);
        });

        // --- Buttons
        cfgResetBtn = this.configPanel.getResetButton();
        cfgRunBtn   = this.configPanel.getRunButton();

        runExternalBtn = new Button("Run (external)");
        runExternalBtn.setOnAction(e -> {
            if (recipeSupplier != null) {
                runWithRecipe(recipeSupplier.get());
            } else {
                toast("No recipe supplier set; use the panel’s Run button.", true);
            }
        });

        clearBtn = new Button("Clear Grid");
        clearBtn.setOnAction(e -> gridPane.clearItems());

        collapseBtn = new Button("Collapse Controls");
        collapseBtn.setOnAction(e -> toggleControlsCollapsed());

        // --- Streaming controls
        flushSizeSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 512, 6, 1));
        flushSizeSpinner.setEditable(true);

        flushIntervalSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 2000, 80, 10));
        flushIntervalSpinner.setEditable(true);

        incrementalSortCheck = new CheckBox("Incremental sort");

        // --- Top left row (buttons + streaming controls)
        HBox streamControls = new HBox(8,
                new Label("Flush N"), flushSizeSpinner,
                new Label("Flush ms"), flushIntervalSpinner,
                incrementalSortCheck
        );
        streamControls.setAlignment(Pos.CENTER_LEFT);

        topLeft = new HBox(10, cfgResetBtn, cfgRunBtn, runExternalBtn, clearBtn, collapseBtn, streamControls);
        topLeft.setAlignment(Pos.CENTER_LEFT);
        topLeft.setPadding(new Insets(6, 8, 6, 8));

        // --- Progress label (right)
        progressLabel = new Label("");
        BorderPane.setAlignment(progressLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(progressLabel, new Insets(6, 8, 6, 8));

        // --- Top bar (left=topLeft, right=progressLabel)
        topBar = new BorderPane();
        topBar.setLeft(topLeft);
        topBar.setRight(progressLabel);
        setTop(topBar);
        
        // --- Left controls area (VBox): topBar, Separator, configPanel
        controlsBox = new VBox();
//        controlsBox.getChildren().addAll(topBar, new Separator(), this.configPanel);
        controlsBox.getChildren().addAll(this.configPanel);
        VBox.setMargin(this.configPanel, new Insets(6, 8, 6, 8));

        // Give controls a reasonable min/pref so divider behaves well
        controlsBox.setMinWidth(220);
        controlsBox.setPrefWidth(390);

        // --- SplitPane: controls (left), grid (right)
        splitPane = new SplitPane(controlsBox, gridPane);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(lastDividerPos);
        splitPane.setBackground(Background.EMPTY);
        setCenter(splitPane);
        BorderPane.setMargin(splitPane, new Insets(6));

        // Wire config panel Run to us
        this.configPanel.setOnRun(this::runWithRecipe);
    }

    // --- Public API ---

    public void setCohortA(List<FeatureVector> vectors, String label) {
        this.cohortA = (vectors == null) ? new ArrayList<>() : new ArrayList<>(vectors);
        if (label != null && !label.isBlank()) this.cohortALabel = label;
    }

    public void setCohortB(List<FeatureVector> vectors, String label) {
        this.cohortB = (vectors == null) ? new ArrayList<>() : new ArrayList<>(vectors);
        if (label != null && !label.isBlank()) this.cohortBLabel = label;
    }

    public void setRecipeSupplier(Supplier<JpdfRecipe> supplier) { this.recipeSupplier = supplier; }

    public void setToastHandler(Consumer<String> handler) { this.toastHandler = handler; }

    public void setOnCellClick(Consumer<PairGridPane.PairItem> handler) { this.onCellClickHandler = handler; }

    /** Run computation (background thread) and live-append thumbnails with tunable cadence. */
    public void runWithRecipe(JpdfRecipe recipe) {
        if (recipe == null && recipeSupplier != null) {
            recipe = recipeSupplier.get();
        }
        if (recipe == null) { toast("No recipe provided.", true); return; }
        if (cohortA == null || cohortA.isEmpty()) { toast("Cohort A is empty. Load or set vectors first.", true); return; }

        final CanonicalGridPolicy policy = CanonicalGridPolicy.get(
                (recipe.getBoundsPolicy() == JpdfRecipe.BoundsPolicy.CANONICAL_BY_FEATURE)
                        ? recipe.getCanonicalPolicyId()
                        : "default"
        );
        if (policy == null) {
            toast("Failed to determine canonical grid policy for recipe.", true);
            return;
        }

        // Prepare UI
        gridPane.clearItems();
        setControlsDisabled(true);
        setProgressText("Preparing…");
        toast("Computing pairwise densities…", false);

        // Progress counter
        AtomicInteger completed = new AtomicInteger(0);

        // Streaming cadence
        final int flushN = safeSpinnerInt(flushSizeSpinner, 6);
        final int flushMs = safeSpinnerInt(flushIntervalSpinner, 80);
        final boolean doIncrementalSort = incrementalSortCheck.isSelected();

        // On-result (invoked from engine worker threads)
        final java.util.function.Consumer<JpdfBatchEngine.PairJobResult> onResult = job -> {
            if (job == null || job.density == null) return;

            String xLbl = (job.i >= 0) ? ("Comp " + job.i) : "X";
            String yLbl = (job.j >= 0) ? ("Comp " + job.j) : "Y";

            PairGridPane.PairItem.Builder b = PairGridPane.PairItem
                    .newBuilder(xLbl, yLbl)
                    .from(job.density, /*useCDF*/ false, /*flipY*/ true)
                    .palette(HeatmapThumbnailView.PaletteKind.SEQUENTIAL)
                    .autoRange(true)
                    .showLegend(false);

            if (job.rank != null) b.score(job.rank.score);
            PairGridPane.PairItem item = b.build();

            gridPane.addItemStreaming(item, flushN, flushMs, doIncrementalSort);

            int n = completed.incrementAndGet();
            if ((n % flushN) == 0) {
                Platform.runLater(() -> setProgressText("Loaded " + n + "…"));
            }
        };

        final JpdfRecipe runRecipe = recipe;

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                JpdfBatchEngine.BatchResult batch;
                long start = System.currentTimeMillis();
                try {
                    switch (runRecipe.getPairSelection()) {
                        case WHITELIST ->
                                batch = JpdfBatchEngine.runWhitelistPairs(cohortA, runRecipe, policy, cache, onResult);
                        default ->
                                batch = JpdfBatchEngine.runComponentPairs(cohortA, runRecipe, policy, cache, onResult);
                    }
                } catch (Throwable t) {
                    final String msg = "Batch failed: " + t.getClass().getSimpleName()
                            + " - " + String.valueOf(t.getMessage());
                    Platform.runLater(() -> {
                        setControlsDisabled(false);
                        setProgressText("");
                        toast(msg, true);
                    });
                    return null;
                }

                final BatchResult finalBatch = batch;
                final int finalCompleted = completed.get();
                final long wall = System.currentTimeMillis() - start;

                Platform.runLater(() -> {
                    // Final sort by score (descending) to present a clean list at the end
                    gridPane.sortByScoreDescending();

                    int total = Math.max(finalBatch.submittedPairs, finalCompleted);
                    setProgressText("Loaded " + finalCompleted + " / " + total);
                    setControlsDisabled(false);
                    toast("Batch complete: " + finalCompleted
                            + " surfaces; cacheHits=" + finalBatch.cacheHits
                            + "; wall=" + wall + " ms.", false);
                });
                return null;
            }
        };

        workerThread = new Thread(task, "PairwiseJpdfView-Worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    // --- Helpers ---

    private void toggleControlsCollapsed() {
        double pos = splitPane.getDividerPositions()[0];
        // Collapse if not already collapsed
        if (pos > 0.02) {
            lastDividerPos = pos;
            splitPane.setDividerPositions(0.0);
            collapseBtn.setText("Expand Controls");
        } else {
            splitPane.setDividerPositions(Math.max(0.15, lastDividerPos));
            collapseBtn.setText("Collapse Controls");
        }
    }

    private int safeSpinnerInt(Spinner<Integer> sp, int fallback) {
        try {
            sp.commitValue();
            Integer v = sp.getValue();
            return (v == null) ? fallback : v.intValue();
        } catch (Throwable ignore) {
            return fallback;
        }
    }

    private void setControlsDisabled(boolean disabled) {
        topBar.setDisable(disabled);
        configPanel.setDisable(disabled);
    }

    private void setProgressText(String text) {
        progressLabel.setText(text == null ? "" : text);
    }

    public void toast(String msg, boolean isError) {
        if (toastHandler != null) {
            toastHandler.accept((isError ? "[Error] " : "") + msg);
        } else {
            System.out.println((isError ? "[Error] " : "[Info] ") + msg);
        }
    }

    // Expose underlying controls if needed
    public PairwiseJpdfConfigPanel getConfigPanel() { return configPanel; }
    public PairGridPane getGridPane() { return gridPane; }
    public SplitPane getSplitPane() { return splitPane; }

    // Optional cancel hook (engine would need to check interrupts to honor it)
    public void cancelRun() {
        Thread t = workerThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
    }

    // Current state (optional)
    public List<FeatureVector> getCohortA() { return cohortA; }
    public List<FeatureVector> getCohortB() { return cohortB; }
    public String getCohortALabel() { return cohortALabel; }
    public String getCohortBLabel() { return cohortBLabel; }
}
