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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * PairwiseJpdfView
 * - Top: buttons (left) + progress label (right)
 * - Left: Config Panel
 * - Center: Grid of heatmap thumbnails (live-append as results complete)
 *
 * Streaming UI approach:
 *   - Engine threads enqueue results into 'pendingQueue' (no runLater per item).
 *   - A Timeline on the FX thread flushes up to N items every tick to the grid.
 *   - When engine signals done and queue drains, we finalize (sort, toast, enable UI).
 */
public class PairwiseJpdfView extends BorderPane {

    // Core GUI nodes
    private final PairwiseJpdfConfigPanel configPanel;
    private final PairGridPane gridPane;
    private final BorderPane topBar;   // container for buttons (left) + progress (right)
    private final HBox topButtons;     // left side
    private final Label progressLabel; // right side

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

    // Background worker ref (optional cancellation later)
    private volatile Thread workerThread;

    // --- Streaming UI buffers/state ---
    private final ConcurrentLinkedQueue<PairGridPane.PairItem> pendingQueue = new ConcurrentLinkedQueue<>();
    private Timeline flushTimer;                 // runs on FX thread, drains pendingQueue
    private volatile boolean streamDone = false; // set true when engine finishes
    private final AtomicInteger completed = new AtomicInteger(0);

    // final summary to show when flushing finishes
    private volatile JpdfBatchEngine.BatchResult finalBatchSummary = null;
    private volatile long finalWallMillis = 0L;

    // Tuning: how often & how many items to flush per tick
    private static final Duration FLUSH_PERIOD = Duration.millis(50);
    private static final int FLUSH_BATCH_SIZE = 12;

    public PairwiseJpdfView(JpdfBatchEngine engine, DensityCache cache, PairwiseJpdfConfigPanel configPanel) {
        this.cache = (cache != null) ? cache : new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
        this.configPanel = (configPanel != null) ? configPanel : new PairwiseJpdfConfigPanel();

        this.gridPane = new PairGridPane();
        this.gridPane.setOnCellClick(click -> {
            if (onCellClickHandler != null) onCellClickHandler.accept(click.item);
        });

        // Buttons (use the config panel's run/reset, plus our extras)
        Button cfgReset = this.configPanel.getResetButton();
        Button cfgRun   = this.configPanel.getRunButton();

        Button runExternalBtn = new Button("Run (external)");
        runExternalBtn.setOnAction(e -> {
            if (recipeSupplier != null) {
                runWithRecipe(recipeSupplier.get());
            } else {
                toast("No recipe supplier set; use the panel’s Run button.", true);
            }
        });

        Button clearBtn = new Button("Clear Grid");
        clearBtn.setOnAction(e -> gridPane.clearItems());

        topButtons = new HBox(10, cfgReset, cfgRun, runExternalBtn, clearBtn);
        topButtons.setAlignment(Pos.CENTER_LEFT);
        topButtons.setPadding(new Insets(6, 8, 6, 8));

        progressLabel = new Label("");
        BorderPane.setAlignment(progressLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(progressLabel, new Insets(6, 8, 6, 8));

        topBar = new BorderPane();
        topBar.setLeft(topButtons);
        topBar.setRight(progressLabel);

        // Left = VBox-like: topBar, then config panel
        BorderPane left = new BorderPane();
        left.setTop(topBar);
        left.setCenter(this.configPanel);
        BorderPane.setMargin(this.configPanel, new Insets(6, 8, 6, 8));

        setLeft(left);
        setCenter(gridPane);
        BorderPane.setMargin(gridPane, new Insets(6));
        setPadding(new Insets(6));

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

    /** Run computation (on a background thread) and populate grid with live-append thumbnails using a buffered FX flush. */
    public void runWithRecipe(JpdfRecipe recipe) {
        if (recipe == null && recipeSupplier != null) {
            recipe = recipeSupplier.get();
        }
        if (recipe == null) { toast("No recipe provided.", true); return; }
        if (cohortA == null || cohortA.isEmpty()) { toast("Cohort A is empty. Load or set vectors first.", true); return; }

        CanonicalGridPolicy policy = CanonicalGridPolicy.get(
            (recipe.getBoundsPolicy() == JpdfRecipe.BoundsPolicy.CANONICAL_BY_FEATURE)
                ? recipe.getCanonicalPolicyId()
                : "default"
        );
        if (policy == null) {
            toast("Failed to determine canonical grid policy for recipe.", true);
            return;
        }

        // Prepare UI for a fresh run
        gridPane.clearItems();
        setTopBarDisabled(true);
        setProgressText("Preparing…");
        toast("Computing pairwise densities…", false);

        // Reset streaming state
        pendingQueue.clear();
        completed.set(0);
        streamDone = false;
        finalBatchSummary = null;
        finalWallMillis = 0L;

        // Start/Restart the flush timer on FX thread
        startFlushTimer();

        // Streaming callback invoked by engine threads (NO runLater here)
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

            // enqueue for FX flush
            pendingQueue.add(item);
            completed.incrementAndGet();
        };

        final JpdfRecipe runRecipe = recipe;   // capture final reference
        final CanonicalGridPolicy runPolicy = policy;

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                JpdfBatchEngine.BatchResult batch;
                long start = System.currentTimeMillis();
                try {
                    switch (runRecipe.getPairSelection()) {
                        case WHITELIST ->
                            batch = JpdfBatchEngine.runWhitelistPairs(cohortA, runRecipe, runPolicy, cache, onResult);
                        default ->
                            batch = JpdfBatchEngine.runComponentPairs(cohortA, runRecipe, runPolicy, cache, onResult);
                    }
                } catch (Throwable t) {
                    final String msg = "Batch failed: " + t.getClass().getSimpleName()
                                     + " - " + String.valueOf(t.getMessage());
                    Platform.runLater(() -> {
                        streamDone = true;
                        setTopBarDisabled(false);
                        setProgressText("");
                        toast(msg, true);
                        stopFlushTimer(); // no more appends
                    });
                    return null;
                }

                final BatchResult finalBatch = batch;
                final long wall = System.currentTimeMillis() - start;

                // Signal done & stash summary for finalize on FX thread
                Platform.runLater(() -> {
                    finalBatchSummary = finalBatch;
                    finalWallMillis = wall;
                    streamDone = true;
                });
                return null;
            }
        };

        workerThread = new Thread(task, "PairwiseJpdfView-Worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    // --- Streaming flush machinery (FX thread) ---

    private void startFlushTimer() {
        if (flushTimer != null) {
            flushTimer.stop();
        }
        flushTimer = new Timeline(new KeyFrame(FLUSH_PERIOD, e -> flushPendingOnce()));
        flushTimer.setCycleCount(Timeline.INDEFINITE);
        flushTimer.playFromStart();
    }

    private void stopFlushTimer() {
        if (flushTimer != null) {
            flushTimer.stop();
            flushTimer = null;
        }
    }

    /** Runs on FX thread every tick; drains up to FLUSH_BATCH_SIZE items and appends to the grid. */
    private void flushPendingOnce() {
        int n = 0;
        PairGridPane.PairItem item;
        while (n < FLUSH_BATCH_SIZE && (item = pendingQueue.poll()) != null) {
            gridPane.addItemStreaming(item);
            n++;
        }
        if (n > 0) {
            setProgressText("Loaded " + completed.get() + "…");
        }

        // If engine is done and queue is empty, finalize once and stop timer
        if (streamDone && pendingQueue.isEmpty()) {
            // Optional final sort (rebuilds once at the end)
            gridPane.sortByScoreDescending();

            BatchResult fb = finalBatchSummary;
            if (fb != null) {
                int total = Math.max(fb.submittedPairs, completed.get());
                setProgressText("Loaded " + completed.get() + " / " + total);
                setTopBarDisabled(false);
                toast("Batch complete: " + completed.get()
                        + " surfaces; cacheHits=" + fb.cacheHits
                        + "; wall=" + finalWallMillis + " ms.", false);
            } else {
                setTopBarDisabled(false);
            }
            stopFlushTimer();
        }
    }

    // --- Helpers ---

    private void setTopBarDisabled(boolean disabled) {
        topButtons.setDisable(disabled);
        progressLabel.setDisable(disabled);
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
    public BorderPane getTopBar() { return topBar; }

    // Optional cancel hook (engine would need to check interrupts to honor it)
    public void cancelRun() {
        Thread t = workerThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
        Platform.runLater(() -> {
            streamDone = true;
            stopFlushTimer();
            setTopBarDisabled(false);
            setProgressText("Cancelled");
        });
    }

    // Current state (optional)
    public List<FeatureVector> getCohortA() { return cohortA; }
    public List<FeatureVector> getCohortB() { return cohortB; }
    public String getCohortALabel() { return cohortALabel; }
    public String getCohortBLabel() { return cohortBLabel; }
}
