package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceGridEvent;
import edu.jhuapl.trinity.utils.statistics.CanonicalGridPolicy;
import edu.jhuapl.trinity.utils.statistics.DensityCache;
import edu.jhuapl.trinity.utils.statistics.GridDensityResult;
import edu.jhuapl.trinity.utils.statistics.HeatmapThumbnailView;
import edu.jhuapl.trinity.utils.statistics.JpdfBatchEngine;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe;
import edu.jhuapl.trinity.utils.statistics.PairGridPane;
import edu.jhuapl.trinity.utils.statistics.PairwiseJpdfConfigPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class PairwiseJpdfPane extends LitPathPane {

    private final BorderPane root;
    private final PairwiseJpdfConfigPanel config;
    private final PairGridPane grid;

    private final JpdfBatchEngine engine;
    private final DensityCache cache;

    private List<FeatureVector> cohortA = new ArrayList<>();
    private List<FeatureVector> cohortB = new ArrayList<>();
    private String cohortALabel = "A";
    private String cohortBLabel = "B";

    private Supplier<JpdfRecipe> recipeSupplier;

    public PairwiseJpdfPane(Scene scene,
                            Pane parent,
                            JpdfBatchEngine engine,
                            DensityCache cache,
                            PairwiseJpdfConfigPanel configPanel) {
        super(scene, parent,
            1100, 760,
            new BorderPane(),
            "Pairwise Joint Densities", "Batch",
            420.0, 400.0);

        this.root = (BorderPane) this.contentPane;
        this.engine = (engine != null) ? engine : new JpdfBatchEngine();
        this.cache = (cache != null) ? cache : new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
        this.config = Objects.requireNonNull(configPanel, "configPanel");

        this.grid = new PairGridPane();
        this.grid.setOnCellClick(click -> openCellIn3D(click.item));

        buildLayout();
        wireHandlers(scene);
    }

    public PairwiseJpdfPane(Scene scene, Pane parent) {
        this(scene, parent, null, null, new PairwiseJpdfConfigPanel());
    }

    public void setCohortA(List<FeatureVector> vectors, String label) {
        this.cohortA = (vectors == null) ? new ArrayList<>() : new ArrayList<>(vectors);
        if (label != null && !label.isBlank()) this.cohortALabel = label;
    }

    public void setCohortB(List<FeatureVector> vectors, String label) {
        this.cohortB = (vectors == null) ? new ArrayList<>() : new ArrayList<>(vectors);
        if (label != null && !label.isBlank()) this.cohortBLabel = label;
    }

    public PairwiseJpdfConfigPanel getConfigPanel() { return config; }
    public PairGridPane getGridPane() { return grid; }

    public void setRecipeSupplier(Supplier<JpdfRecipe> supplier) { this.recipeSupplier = supplier; }

    public void runWithRecipe(JpdfRecipe recipe) {
        if (recipe == null) { toast("No recipe provided.", true); return; }
        if (cohortA == null || cohortA.isEmpty()) { toast("Cohort A is empty. Load or set vectors first.", true); return; }

        CanonicalGridPolicy policy = CanonicalGridPolicy.get(
                (recipe.getBoundsPolicy() == JpdfRecipe.BoundsPolicy.CANONICAL_BY_FEATURE)
                        ? recipe.getCanonicalPolicyId()
                        : "default"
        );

        toast("Computing pairwise densities…", false);

        JpdfBatchEngine.BatchResult batch;
        switch (recipe.getPairSelection()) {
            case WHITELIST -> batch = JpdfBatchEngine.runWhitelistPairs(cohortA, recipe, policy, cache);
            default -> batch = JpdfBatchEngine.runComponentPairs(cohortA, recipe, policy, cache);
        }

        List<PairGridPane.PairItem> items = new ArrayList<>(batch.jobs.size());
        for (JpdfBatchEngine.PairJobResult job : batch.jobs) {
            String xLbl = (job.i >= 0) ? ("Comp " + job.i) : "X";
            String yLbl = (job.j >= 0) ? ("Comp " + job.j) : "Y";

            PairGridPane.PairItem.Builder b = PairGridPane.PairItem
                    .newBuilder(xLbl, yLbl)
                    .from(job.density, false, true)         // PDF thumbnail by default, flipY=true
                    .palette(HeatmapThumbnailView.PaletteKind.SEQUENTIAL)
                    .autoRange(true)
                    .showLegend(false);

            if (job.rank != null) b.score(job.rank.score);
            items.add(b.build());
        }
        grid.setItems(items);
        grid.sortByScoreDescending();

        toast("Batch complete: " + items.size() + " surfaces; cacheHits=" + batch.cacheHits +
                "; wall=" + batch.wallMillis + " ms.", false);
    }

    @Override
    public void maximize() {
        scene.getRoot().fireEvent(
            new ApplicationEvent(ApplicationEvent.POPOUT_PAIRWISEJPDF_JPDF, Boolean.TRUE)
        );
    }

    private void buildLayout() {
        // Top bar now includes: Reset, Run (from config), | Run (external), Clear Grid
        Button runExternalBtn = new Button("Run (external)");
        runExternalBtn.setOnAction(e -> {
            if (recipeSupplier != null) runWithRecipe(recipeSupplier.get());
            else toast("No recipe supplier set; use the panel’s Run button.", true);
        });

        Button clearBtn = new Button("Clear Grid");
        clearBtn.setOnAction(e -> grid.clearItems());

        Button cfgReset = config.getResetButton();
        Button cfgRun   = config.getRunButton();

        HBox bar = new HBox(10, cfgReset, cfgRun, runExternalBtn, clearBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 8, 6, 8));

        BorderPane left = new BorderPane();
        left.setTop(bar);
        left.setCenter(config);
        BorderPane.setMargin(config, new Insets(6, 8, 6, 8));

        root.setLeft(left);
        root.setCenter(grid);
        BorderPane.setMargin(grid, new Insets(2));
        root.setPadding(new Insets(2));
    }

    private void wireHandlers(Scene scene) {
        // Wire config 'Run' to our execution path
        config.setOnRun(this::runWithRecipe);

        // Accept NEW_FEATURE_COLLECTION as Cohort A
        scene.addEventHandler(FeatureVectorEvent.NEW_FEATURE_COLLECTION, e -> {
            if (e.object instanceof FeatureCollection fc && fc.getFeatures() != null) {
                setCohortA(fc.getFeatures(), "A");
                toast("Loaded " + fc.getFeatures().size() + " vectors into Cohort A.", false);
            }
        });
    }

    private void openCellIn3D(PairGridPane.PairItem item) {
        if (item == null || item.res == null) return;
        GridDensityResult res = item.res;

        // default: open PDF; your UI can add a toggle later if needed
        var gridList = res.pdfAsListGrid();
        scene.getRoot().fireEvent(new HypersurfaceGridEvent(
                HypersurfaceGridEvent.RENDER_PDF,
                gridList,
                res.getxCenters(),
                res.getyCenters(),
                item.xLabel + " | " + item.yLabel + " (PDF)"
        ));
        toast("Opened PDF in 3D.", false);
    }

    private void toast(String msg, boolean isError) {
        Platform.runLater(() -> scene.getRoot().fireEvent(
                new CommandTerminalEvent(msg, new Font("Consolas", 18),
                        isError ? Color.ORANGERED : Color.LIGHTGREEN)));
    }
}
