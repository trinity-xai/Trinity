package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceGridEvent;
import edu.jhuapl.trinity.utils.statistics.CanonicalGridPolicy;
import edu.jhuapl.trinity.utils.statistics.DensityCache;
import edu.jhuapl.trinity.utils.statistics.GridDensityResult;
import edu.jhuapl.trinity.utils.statistics.JpdfBatchEngine;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe;
import edu.jhuapl.trinity.utils.statistics.PairGridPane;
import edu.jhuapl.trinity.utils.statistics.StatisticEngine;
import edu.jhuapl.trinity.utils.statistics.AxisParams;
import edu.jhuapl.trinity.utils.statistics.PairwiseJpdfConfigPanel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * PairwiseJpdfPane
 * ----------------
 * Floating parent (LitPathPane) hosting:
 *  - PairwiseJpdfConfigPanel (provided by caller, shown on left)
 *  - PairGridPane (center thumbnails)
 *
 * The config panel emits a JpdfRecipe via setOnRun(...). This pane runs a batch with
 * JpdfBatchEngine.{runComponentPairs,runWhitelistPairs} depending on the recipe and
 * renders results in the PairGridPane. Clicking a thumbnail fires
 * HypersurfaceGridEvent.{RENDER_PDF, RENDER_CDF} so Hypersurface3DPane can update.
 *
 * Provide vectors via setCohortA / setCohortB. Cohort B is optional.
 *
 * Notes:
 *  - Thumbnails render PDF by default; the "Open as" control decides what is sent to 3D.
 */
public final class PairwiseJpdfPane extends LitPathPane {

    private enum OpenKind { PDF, CDF }

    private final BorderPane root;
    private final PairwiseJpdfConfigPanel config;
    private final PairGridPane grid;

    private final JpdfBatchEngine engine;
    private final DensityCache cache;

    private List<FeatureVector> cohortA = new ArrayList<>();
    private List<FeatureVector> cohortB = new ArrayList<>();
    private String cohortALabel = "A";
    private String cohortBLabel = "B";

    /** Optional external trigger to provide a recipe; not required if you use the panel's Run. */
    private Supplier<JpdfRecipe> recipeSupplier;

    // UI controls local to this pane
    private final ComboBox<OpenKind> openKindCombo = new ComboBox<>();

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

        buildLayout();
        wireHandlers(scene);
    }

    public PairwiseJpdfPane(Scene scene, Pane parent) {
        this(scene, parent, null, null, new PairwiseJpdfConfigPanel());
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

    public PairwiseJpdfConfigPanel getConfigPanel() { return config; }
    public PairGridPane getGridPane() { return grid; }

    /** Optional: Use if you want an external Run button elsewhere. */
    public void setRecipeSupplier(Supplier<JpdfRecipe> supplier) { this.recipeSupplier = supplier; }

    /** Entry point used by the config panel's Run callback. */
    public void runWithRecipe(JpdfRecipe recipe) {
        if (recipe == null) {
            toast("No recipe provided.", true);
            return;
        }
        if (cohortA == null || cohortA.isEmpty()) {
            toast("Cohort A is empty. Load or set vectors first.", true);
            return;
        }

        // Resolve canonical policy (even if not used, we pass a valid object)
        CanonicalGridPolicy policy = CanonicalGridPolicy.get(
                (recipe.getBoundsPolicy() == JpdfRecipe.BoundsPolicy.CANONICAL_BY_FEATURE)
                        ? recipe.getCanonicalPolicyId()
                        : "default"
        );

        toast("Computing pairwise densities…", false);

        JpdfBatchEngine.BatchResult batch;
        switch (recipe.getPairSelection()) {
            case WHITELIST -> {
                batch = JpdfBatchEngine.runWhitelistPairs(cohortA, recipe, policy, cache);
            }
            default -> {
                // ALL / TOP_K_BY_SCORE / THRESHOLD_BY_SCORE
                batch = JpdfBatchEngine.runComponentPairs(cohortA, recipe, policy, cache);
            }
        }

        // Convert to PairGridPane items for the UI (thumbnails: PDF by default)
        List<PairGridPane.PairItem> items = new ArrayList<>(batch.jobs.size());
        for (JpdfBatchEngine.PairJobResult job : batch.jobs) {
            String xLab = axisLabel(job.xAxis, job.i);
            String yLab = axisLabel(job.yAxis, job.j);

            Double score = (job.rank != null) ? job.rank.score : null;

            PairGridPane.PairItem item = PairGridPane.PairItem
                    .newBuilder(xLab, yLab)
                    .from(job.density, /*useCDF=*/false, /*flipY=*/true)
                    .score(score)
                    .showLegend(false)
                    .build();

            items.add(item);
        }

        // Sort by score descending (nulls last)
        grid.setSorter(Comparator.nullsLast((a, b) -> {
            double sa = a.score == null ? Double.NEGATIVE_INFINITY : a.score;
            double sb = b.score == null ? Double.NEGATIVE_INFINITY : b.score;
            return -Double.compare(sa, sb);
        }));

        grid.setItems(items);

        toast("Batch complete: " + items.size() + " surfaces; cacheHits=" + batch.cacheHits +
                "; wall=" + batch.wallMillis + " ms.", false);
    }

    @Override
    public void maximize() {
        // Intentionally left for you to wire to your pop-out event if desired.
        // Example:
        // scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.POPOUT_PAIRWISE_JPDF, Boolean.TRUE));
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private void buildLayout() {
        // Controls: Open-as selector + convenience buttons
        openKindCombo.getItems().setAll(OpenKind.PDF, OpenKind.CDF);
        openKindCombo.getSelectionModel().select(OpenKind.PDF);

        Button runBtn = new Button("Run (external)");
        runBtn.setOnAction(e -> {
            if (recipeSupplier != null) {
                runWithRecipe(recipeSupplier.get());
            } else {
                toast("No recipe supplier set; use the panel’s Run button.", true);
            }
        });

        Button clearBtn = new Button("Clear Grid");
        clearBtn.setOnAction(e -> grid.clearItems());

        HBox bar = new HBox(10,
                runBtn,
                clearBtn,
                new Separator(),
                openKindCombo
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 8, 6, 8));

        BorderPane left = new BorderPane();
        left.setTop(bar);
        left.setCenter(config);
        BorderPane.setMargin(config, new Insets(6, 8, 6, 8));

        root.setLeft(left);
        root.setCenter(grid);
        BorderPane.setMargin(grid, new Insets(6));
        root.setPadding(new Insets(6));
        root.setBottom(new Separator());
    }

    private void wireHandlers(Scene scene) {
        // Wire the config panel to this pane
        config.setOnRun(this::runWithRecipe);

        // Click on a thumbnail -> open in 3D (PDF/CDF decided by openKindCombo)
        grid.setOnCellClick(click -> {
            if (click == null || click.item == null) return;
            openClickedCell(click);
        });

        // Convenience: accept NEW_FEATURE_COLLECTION as Cohort A
        scene.addEventHandler(FeatureVectorEvent.NEW_FEATURE_COLLECTION, e -> {
            if (e.object instanceof FeatureCollection fc && fc.getFeatures() != null) {
                setCohortA(fc.getFeatures(), "A");
                toast("Loaded " + fc.getFeatures().size() + " vectors into Cohort A.", false);
            }
        });
    }

    private void openClickedCell(PairGridPane.CellClick click) {
        PairGridPane.PairItem item = click.item;
        OpenKind kind = openKindCombo.getValue() == null ? OpenKind.PDF : openKindCombo.getValue();

        List<List<Double>> zGrid;
        double[] xCenters = null;
        double[] yCenters = null;

        if (item.res != null) {
            GridDensityResult res = item.res;
            if (kind == OpenKind.CDF) {
                zGrid = res.cdfAsListGrid();
            } else {
                zGrid = res.pdfAsListGrid();
            }
            xCenters = res.getxCenters();
            yCenters = res.getyCenters();
        } else {
            // Item was constructed with a raw grid (no centers available)
            zGrid = item.grid;
            // xCenters / yCenters left null (renderer should handle this case)
        }

        String label = (kind == OpenKind.CDF ? "CDF: " : "PDF: ") + item.xLabel + " | " + item.yLabel;

        scene.getRoot().fireEvent(new HypersurfaceGridEvent(
                kind == OpenKind.CDF ? HypersurfaceGridEvent.RENDER_CDF : HypersurfaceGridEvent.RENDER_PDF,
                zGrid, xCenters, yCenters, label));

        toast("Opened " + (kind == OpenKind.CDF ? "CDF" : "PDF") + " in 3D: " + item.xLabel + " | " + item.yLabel, false);
    }

    private static String axisLabel(AxisParams axis, int componentIndexIfAny) {
        if (axis == null || axis.getType() == null) return "Axis";
        StatisticEngine.ScalarType t = axis.getType();
        return switch (t) {
            case COMPONENT_AT_DIMENSION -> {
                int idx = axis.getComponentIndex() != null ? axis.getComponentIndex() : componentIndexIfAny;
                yield (idx >= 0) ? ("Comp[" + idx + "]") : "Component";
            }
            case METRIC_DISTANCE_TO_MEAN -> {
                String m = axis.getMetricName();
                yield (m != null && !m.isBlank()) ? (m + "→mean") : "metric→mean";
            }
            default -> t.name();
        };
    }

    private void toast(String msg, boolean isError) {
        Platform.runLater(() -> scene.getRoot().fireEvent(
                new CommandTerminalEvent(msg, new Font("Consolas", 18),
                        isError ? Color.ORANGERED : Color.LIGHTGREEN)));
    }
}
