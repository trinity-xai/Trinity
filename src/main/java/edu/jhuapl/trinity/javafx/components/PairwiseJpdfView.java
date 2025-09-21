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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * PairwiseJpdfView
 * - Top bar with: Reset, Run (from config), Run (external), Clear Grid
 * - Left: Config Panel
 * - Center: Grid of heatmap thumbnails
 * - Public API to set cohorts, supply recipes, and handle run/clear events.
 * - Usable in floating pane or in popout/fullscreen windows.
 */
public class PairwiseJpdfView extends BorderPane {

    // Core GUI nodes
    private final PairwiseJpdfConfigPanel configPanel;
    private final PairGridPane gridPane;
    private final HBox topBar;

    // Core engine/cache (now always non-null)
    private final DensityCache cache;

    // State
    private List<FeatureVector> cohortA = new ArrayList<>();
    private List<FeatureVector> cohortB = new ArrayList<>();
    private String cohortALabel = "A";
    private String cohortBLabel = "B";

    private Supplier<JpdfRecipe> recipeSupplier; // optional, for run external button
    private Consumer<String> toastHandler; // optional, for showing user messages

    // Cell click handler (e.g., for open in 3D)
    private Consumer<PairGridPane.PairItem> onCellClickHandler;

    public PairwiseJpdfView(JpdfBatchEngine engine, DensityCache cache, PairwiseJpdfConfigPanel configPanel) {
        // Defensive: always have a cache (if none supplied, create default)
        this.cache = (cache != null) ? cache : new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
        this.configPanel = (configPanel != null) ? configPanel : new PairwiseJpdfConfigPanel();

        this.gridPane = new PairGridPane();
        this.gridPane.setOnCellClick(click -> {
            if (onCellClickHandler != null) onCellClickHandler.accept(click.item);
        });

        // Buttons (configPanel's Run/Reset, plus ours)
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

        Button cfgReset = this.configPanel.getResetButton();
        Button cfgRun   = this.configPanel.getRunButton();

        topBar = new HBox(10, cfgReset, cfgRun, runExternalBtn, clearBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(6, 8, 6, 8));

        // Left = VBox(topBar, configPanel)
        BorderPane left = new BorderPane();
        left.setTop(topBar);
        left.setCenter(this.configPanel);
        BorderPane.setMargin(this.configPanel, new Insets(6, 8, 6, 8));

        setLeft(left);
        setCenter(gridPane);
        BorderPane.setMargin(gridPane, new Insets(6));
        setPadding(new Insets(6));

        // Wire config panel 'Run' to runWithRecipe
        this.configPanel.setOnRun(this::runWithRecipe);
    }

    // --- Public API ---

    /** Set the vectors for Cohort A (primary) */
    public void setCohortA(List<FeatureVector> vectors, String label) {
        this.cohortA = (vectors == null) ? new ArrayList<>() : new ArrayList<>(vectors);
        if (label != null && !label.isBlank()) this.cohortALabel = label;
    }

    /** Set the vectors for Cohort B (not currently used in batch, but included for completeness) */
    public void setCohortB(List<FeatureVector> vectors, String label) {
        this.cohortB = (vectors == null) ? new ArrayList<>() : new ArrayList<>(vectors);
        if (label != null && !label.isBlank()) this.cohortBLabel = label;
    }

    /** Allow parent to provide a recipe supplier for "Run (external)" button */
    public void setRecipeSupplier(Supplier<JpdfRecipe> supplier) { this.recipeSupplier = supplier; }

    /** Set a custom handler for toasts/user messages (optional) */
    public void setToastHandler(Consumer<String> handler) { this.toastHandler = handler; }

    /** Set cell click handler (e.g., open in 3D) */
    public void setOnCellClick(Consumer<PairGridPane.PairItem> handler) { this.onCellClickHandler = handler; }

    /** Run computation and populate grid, using provided recipe */
    public void runWithRecipe(JpdfRecipe recipe) {
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

        if (this.cache == null) {
            toast("DensityCache is null! This should never happen.", true);
            return;
        }

        toast("Computing pairwise densities…", false);

        // For now, only cohortA is used
        BatchResult batch;
        switch (recipe.getPairSelection()) {
            case WHITELIST -> batch = JpdfBatchEngine.runWhitelistPairs(cohortA, recipe, policy, this.cache);
            default -> batch = JpdfBatchEngine.runComponentPairs(cohortA, recipe, policy, this.cache);
        }

        List<PairGridPane.PairItem> items = new ArrayList<>(batch.jobs.size());
        for (JpdfBatchEngine.PairJobResult job : batch.jobs) {
            String xLbl = (job.i >= 0) ? ("Comp " + job.i) : "X";
            String yLbl = (job.j >= 0) ? ("Comp " + job.j) : "Y";
            PairGridPane.PairItem.Builder b = PairGridPane.PairItem
                .newBuilder(xLbl, yLbl)
                .from(job.density, false, true)
                .palette(HeatmapThumbnailView.PaletteKind.SEQUENTIAL)
                .autoRange(true)
                .showLegend(false);
            if (job.rank != null) b.score(job.rank.score);
            items.add(b.build());
        }
        gridPane.setItems(items);
        gridPane.sortByScoreDescending();
        toast("Batch complete: " + items.size() + " surfaces; cacheHits=" + batch.cacheHits +
                "; wall=" + batch.wallMillis + " ms.", false);
    }

    // --- Toast/user message ---
    public void toast(String msg, boolean isError) {
        if (toastHandler != null) {
            toastHandler.accept((isError ? "[Error] " : "") + msg);
        } else {
            // fallback: print to console
            System.out.println((isError ? "[Error] " : "[Info] ") + msg);
        }
    }

    // --- Expose underlying controls for advanced wiring, if needed ---
    public PairwiseJpdfConfigPanel getConfigPanel() { return configPanel; }
    public PairGridPane getGridPane() { return gridPane; }
    public HBox getTopBar() { return topBar; }

    // --- Advanced: get current state (optional) ---
    public List<FeatureVector> getCohortA() { return cohortA; }
    public List<FeatureVector> getCohortB() { return cohortB; }
    public String getCohortALabel() { return cohortALabel; }
    public String getCohortBLabel() { return cohortBLabel; }
}
