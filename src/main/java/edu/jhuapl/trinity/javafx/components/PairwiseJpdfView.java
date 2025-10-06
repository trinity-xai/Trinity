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
import edu.jhuapl.trinity.utils.statistics.RecipeIo;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * PairwiseJpdfView
 * - Search/Sort are hosted in the PairGridPane header.
 * - Toolbar is slim: Run, Reset, Collapse, Actions (MenuButton), Appearance (MenuButton).
 * - Actions menu contains Save/Load/Clear and streaming controls (Flush N/ms, Incremental sort).
 * - Appearance menu controls visual/display options applied via PairGridPane.
 */
public class PairwiseJpdfView extends BorderPane {

    // Core GUI nodes
    private final PairwiseJpdfConfigPanel configPanel;
    private final PairGridPane gridPane;

    // Split layout
    private final SplitPane splitPane;
    private final VBox controlsBox;
    private final BorderPane topBar;
    private final HBox topLeft;
    private final Label progressLabel;
    private double lastDividerPos = 0.36;

    // Buttons (visible in toolbar)
    private final Button cfgResetBtn;
    private final Button cfgRunBtn;
    private final Button collapseBtn;

    // MenuButtons
    private final MenuButton actionsBtn;
    private final MenuButton appearanceBtn;

    // Streaming controls (inside Actions menu)
    private final Spinner<Integer> flushSizeSpinner;
    private final Spinner<Integer> flushIntervalSpinner;
    private final CheckBox incrementalSortCheck;

    // Search / Sort controls (in grid header)
    private final TextField searchField = new TextField();
    private final ComboBox<String> sortCombo = new ComboBox<>();
    private final CheckBox sortAscCheck = new CheckBox("Asc");

    // Appearance controls (inside Appearance menu)
    private final ComboBox<HeatmapThumbnailView.PaletteKind> paletteCombo = new ComboBox<>();
    private final CheckBox legendCheck = new CheckBox("Legend");
    private final ColorPicker backgroundPicker = new ColorPicker(Color.BLACK);

    private final ToggleGroup rangeModeGroup = new ToggleGroup();
    private final RadioButton rangeAutoBtn   = new RadioButton("Auto");
    private final RadioButton rangeGlobalBtn = new RadioButton("Global");
    private final RadioButton rangeFixedBtn  = new RadioButton("Fixed");
    private final TextField vminField = new TextField();
    private final TextField vmaxField = new TextField();
    private final Button normalizeAllBtn = new Button("Normalize All");

    private final Slider gammaSlider =
            new Slider(0.1, 5.0, 1.0);
    private final Spinner<Double> clipLowPctSp  =
            new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 20.0, 0.0, 0.5));
    private final Spinner<Double> clipHighPctSp =
            new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 20.0, 0.0, 0.5));
    private final CheckBox smoothingCheck = new CheckBox("Smooth");

    private final Spinner<Integer> canvasWSp =
            new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(60, 800, 180, 10));
    private final Spinner<Integer> canvasHSp =
            new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(60, 800, 140, 10));

    // Cache (always non-null)
    private final DensityCache cache;

    // State
    private List<FeatureVector> cohortA = new ArrayList<>();
    private List<FeatureVector> cohortB = new ArrayList<>();
    private String cohortALabel = "A";
    private String cohortBLabel = "B";

    private Consumer<String> toastHandler;
    private Consumer<PairGridPane.PairItem> onCellClickHandler;

    private volatile Thread workerThread;

    public PairwiseJpdfView(JpdfBatchEngine engine, DensityCache cache, PairwiseJpdfConfigPanel configPanel) {
        this.cache = (cache != null) ? cache : new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
        this.configPanel = (configPanel != null) ? configPanel : new PairwiseJpdfConfigPanel();

        // Grid (right)
        this.gridPane = new PairGridPane();
        this.gridPane.setOnCellClick(click -> {
            if (onCellClickHandler != null) onCellClickHandler.accept(click.item);
        });
        // Wire export hook (context menu -> PNG)
        this.gridPane.setOnExportRequest(req -> {
            try {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
                fc.setInitialFileName(safeFilename(req.item.xLabel + "_" + req.item.yLabel) + ".png");
                File f = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
                if (f == null) return;
                WritableImage img = req.image;
                ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", f);
                toast("Saved " + f.getName(), false);
            } catch (Exception ex) {
                toast("Export failed: " + ex.getMessage(), true);
            }
        });

        // Buttons from config panel
        cfgResetBtn = this.configPanel.getResetButton();
        cfgRunBtn   = this.configPanel.getRunButton();

        collapseBtn = new Button("Collapse Controls");
        collapseBtn.setOnAction(e -> toggleControlsCollapsed());

        // Streaming controls (used inside the Actions menu)
        flushSizeSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 512, 6, 1));
        flushSizeSpinner.setEditable(true);
        flushSizeSpinner.getEditor().setPrefColumnCount(4);
        flushSizeSpinner.setMaxWidth(Region.USE_PREF_SIZE);

        flushIntervalSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 2000, 80, 10));
        flushIntervalSpinner.setEditable(true);
        flushIntervalSpinner.getEditor().setPrefColumnCount(5);
        flushIntervalSpinner.setMaxWidth(Region.USE_PREF_SIZE);

        incrementalSortCheck = new CheckBox("Incremental sort");

        // --- Build the compact grid header with Search + Sort ---
        searchField.setPromptText("Search: e.g., x3 vs y*");
        searchField.setPrefColumnCount(18);
        sortCombo.getItems().addAll("Score", "i", "j", "label");
        sortCombo.setValue("Score");
        sortAscCheck.setSelected(false);

        HBox searchSort = new HBox(8,
                new Label("Search"), searchField,
                new Label("Sort"), sortCombo, sortAscCheck
        );
        searchSort.setAlignment(Pos.CENTER_LEFT);
        searchSort.setPadding(new Insets(4, 8, 2, 8));

        // --- Actions (MenuButton) ---
        actionsBtn = buildActionsMenu();

        // --- Appearance (MenuButton) ---
        appearanceBtn = buildAppearanceMenu();

        // --- Slim toolbar ---
        topLeft = new HBox(10, cfgResetBtn, cfgRunBtn, collapseBtn, actionsBtn, appearanceBtn);
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
        controlsBox.setMinWidth(0);
        controlsBox.setPrefWidth(390);

        // SplitPane
        splitPane = new SplitPane(controlsBox, gridPane);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(lastDividerPos);
        splitPane.setBackground(Background.EMPTY);
        setCenter(splitPane);
        BorderPane.setMargin(splitPane, new Insets(6));

        // Wiring
        this.configPanel.setOnRun(this::runWithRecipe);
        searchField.textProperty().addListener((obs, ov, nv) -> applySearchPredicate(nv));
        sortCombo.valueProperty().addListener((obs, ov, nv) -> applySortComparator());
        sortAscCheck.selectedProperty().addListener((obs, ov, nv) -> applySortComparator());
        applySortComparator();

        // Inject header (search/sort row; appearance is in toolbar menu)
        VBox gridHeader = new VBox(4, searchSort);
        gridHeader.setAlignment(Pos.CENTER_LEFT);
        gridHeader.setPadding(new Insets(4, 8, 2, 8));
        gridPane.setHeader(gridHeader);
    }

    // Build the consolidated Actions menu
    private MenuButton buildActionsMenu() {
        MenuButton mb = new MenuButton("Actions");

        // Basic actions
        MenuItem saveItem = new MenuItem("Save Config…");
        saveItem.setOnAction(e -> doSaveRecipe());
        MenuItem loadItem = new MenuItem("Load Config…");
        loadItem.setOnAction(e -> doLoadRecipe());
        MenuItem clearItem = new MenuItem("Clear Grid");
        clearItem.setOnAction(e -> gridPane.clearItems());

        // Streaming controls inside menu (keep menu open while adjusting)
        CheckMenuItem incSortItem = new CheckMenuItem("Incremental sort");
        incSortItem.selectedProperty().bindBidirectional(incrementalSortCheck.selectedProperty());

        HBox flushNBox = new HBox(6, new Label("Flush N"), flushSizeSpinner);
        flushNBox.setAlignment(Pos.CENTER_LEFT);
        CustomMenuItem flushNItem = new CustomMenuItem(flushNBox);
        flushNItem.setHideOnClick(false);

        HBox flushMsBox = new HBox(6, new Label("Flush ms"), flushIntervalSpinner);
        flushMsBox.setAlignment(Pos.CENTER_LEFT);
        CustomMenuItem flushMsItem = new CustomMenuItem(flushMsBox);
        flushMsItem.setHideOnClick(false);

        mb.getItems().addAll(
                saveItem, loadItem, clearItem,
                new SeparatorMenuItem(),
                incSortItem, flushNItem, flushMsItem
        );
        return mb;
    }

private MenuButton buildAppearanceMenu() {
    MenuButton mb = new MenuButton("Appearance");

    // ---- Palette & Legend & Background ----
    // (paletteCombo, legendCheck, backgroundPicker are class fields)
    paletteCombo.getItems().setAll(
            HeatmapThumbnailView.PaletteKind.SEQUENTIAL,
            HeatmapThumbnailView.PaletteKind.DIVERGING
    );
    if (paletteCombo.getValue() == null) {
        paletteCombo.setValue(HeatmapThumbnailView.PaletteKind.SEQUENTIAL);
    }
    if (backgroundPicker.getValue() == null) {
        backgroundPicker.setValue(Color.BLACK);
    }

    // Per-scheme selectors (locals; applied to all current tiles via forEachView)
    ComboBox<HeatmapThumbnailView.PaletteScheme> seqSchemeCombo = new ComboBox<>();
    seqSchemeCombo.getItems().addAll(HeatmapThumbnailView.PaletteScheme.values());
    seqSchemeCombo.setValue(HeatmapThumbnailView.PaletteScheme.VIRIDIS);

    ComboBox<HeatmapThumbnailView.PaletteScheme> divSchemeCombo = new ComboBox<>();
    divSchemeCombo.getItems().addAll(HeatmapThumbnailView.PaletteScheme.values());
    divSchemeCombo.setValue(HeatmapThumbnailView.PaletteScheme.BLUE_YELLOW);

    GridPane paletteGrid = new GridPane();
    paletteGrid.setHgap(8); paletteGrid.setVgap(6); paletteGrid.setPadding(new Insets(6, 8, 6, 8));
    paletteGrid.add(new Label("Palette"), 0, 0); paletteGrid.add(paletteCombo, 1, 0);
    paletteGrid.add(new Label("Seq Scheme"), 0, 1); paletteGrid.add(seqSchemeCombo, 1, 1);
    paletteGrid.add(new Label("Div Scheme"), 0, 2); paletteGrid.add(divSchemeCombo, 1, 2);
    paletteGrid.add(new Label("Legend"), 0, 3); paletteGrid.add(legendCheck, 1, 3);
    paletteGrid.add(new Label("Background"), 0, 4); paletteGrid.add(backgroundPicker, 1, 4);

    CustomMenuItem paletteItem = new CustomMenuItem(paletteGrid); paletteItem.setHideOnClick(false);

    // ---- Range (Auto / Global / Fixed) ----
    // (rangeAutoBtn, rangeGlobalBtn, rangeFixedBtn, vminField, vmaxField, normalizeAllBtn are class fields)
    rangeAutoBtn.setToggleGroup(rangeModeGroup);
    rangeGlobalBtn.setToggleGroup(rangeModeGroup);
    rangeFixedBtn.setToggleGroup(rangeModeGroup);
    if (rangeModeGroup.getSelectedToggle() == null) rangeAutoBtn.setSelected(true);

    vminField.setPromptText("vmin");
    vmaxField.setPromptText("vmax");
    vminField.setPrefColumnCount(6);
    vmaxField.setPrefColumnCount(6);
    vminField.setDisable(true);
    vmaxField.setDisable(true);

    GridPane rangeGrid = new GridPane();
    rangeGrid.setHgap(8); rangeGrid.setVgap(6); rangeGrid.setPadding(new Insets(6, 8, 6, 8));
    HBox modeRow = new HBox(10, rangeAutoBtn, rangeGlobalBtn, rangeFixedBtn);
    rangeGrid.add(new Label("Range"), 0, 0); rangeGrid.add(modeRow, 1, 0);
    HBox fixedRow = new HBox(6, new Label("Min"), vminField, new Label("Max"), vmaxField, normalizeAllBtn);
    rangeGrid.add(new Label("Fixed"), 0, 1); rangeGrid.add(fixedRow, 1, 1);

    CustomMenuItem rangeItem = new CustomMenuItem(rangeGrid); rangeItem.setHideOnClick(false);

    // ---- Contrast & Rendering basics ----
    // (gammaSlider, smoothingCheck are class fields)
    gammaSlider.setMin(0.1);
    gammaSlider.setMax(5.0);
    gammaSlider.setValue(1.0);

    // Low-end alpha: lets background show through low values (v ~ vmin)
    Slider lowEndAlpha = new Slider(0.0, 1.0, 1.0);
    lowEndAlpha.setBlockIncrement(0.05);

    HBox gammaRow = new HBox(8, new Label("Gamma"), gammaSlider);
    HBox alphaRow = new HBox(8, new Label("Low-end α"), lowEndAlpha);
    VBox contrastBox = new VBox(8, gammaRow, alphaRow, smoothingCheck);
    contrastBox.setPadding(new Insets(6, 8, 6, 8));
    CustomMenuItem contrastItem = new CustomMenuItem(contrastBox); contrastItem.setHideOnClick(false);

    // ---- Canvas size (thumbnail sizing) ----
    // (canvasWSp, canvasHSp are class fields and initialized in the constructor)
    HBox sizeRow = new HBox(6, new Label("Canvas W"), canvasWSp, new Label("H"), canvasHSp);
    VBox renderBox = new VBox(8, sizeRow);
    renderBox.setPadding(new Insets(6, 8, 6, 8));
    CustomMenuItem renderItem = new CustomMenuItem(renderBox); renderItem.setHideOnClick(false);

    // ---- Add blocks to menu ----
    mb.getItems().addAll(paletteItem, rangeItem, contrastItem, renderItem);

    // ---- Wiring to grid ----
    paletteCombo.valueProperty().addListener((o,ov,nv) -> gridPane.setPalette(nv));
    legendCheck.selectedProperty().addListener((o,ov,nv) -> gridPane.setLegendVisible(nv));
    backgroundPicker.valueProperty().addListener((o,ov,nv) -> gridPane.setBackgroundColor(nv));

    // Scheme changes apply to all existing views
    seqSchemeCombo.valueProperty().addListener((o,ov,nv) ->
            gridPane.forEachView(v -> v.setSequentialScheme(nv)));
    divSchemeCombo.valueProperty().addListener((o,ov,nv) ->
            gridPane.forEachView(v -> v.setDivergingScheme(nv)));

    smoothingCheck.setSelected(true);
    smoothingCheck.selectedProperty().addListener((o,ov,nv) -> gridPane.setImageSmoothing(nv));
    gammaSlider.valueProperty().addListener((o,ov,nv) -> gridPane.setGamma(nv.doubleValue()));
    lowEndAlpha.valueProperty().addListener((o,ov,nv) ->
            gridPane.forEachView(v -> v.setMinAlphaAtVmin(nv.doubleValue())));

    rangeModeGroup.selectedToggleProperty().addListener((o,ov,nv) -> {
        boolean fixed = nv == rangeFixedBtn;
        boolean global = nv == rangeGlobalBtn;
        vminField.setDisable(!fixed);
        vmaxField.setDisable(!fixed);
        if (fixed) {
            double mn = parseDoubleSafe(vminField.getText(), Double.NaN);
            double mx = parseDoubleSafe(vmaxField.getText(), Double.NaN);
            if (Double.isFinite(mn) && Double.isFinite(mx)) {
                gridPane.setRangeModeFixed(mn, mx);
            }
        } else if (global) {
            gridPane.setRangeModeGlobal();
        } else {
            gridPane.setRangeModeAuto();
        }
    });

    normalizeAllBtn.setOnAction(e -> {
        PairGridPane.Range r = gridPane.computeGlobalRange();
        if (r != null) {
            vminField.setText(String.format("%.6g", r.vmin));
            vmaxField.setText(String.format("%.6g", r.vmax));
            rangeFixedBtn.setSelected(true);
            gridPane.setRangeModeFixed(r.vmin, r.vmax);
        }
    });

    vminField.setOnAction(e -> applyFixedRangeFromFields());
    vmaxField.setOnAction(e -> applyFixedRangeFromFields());

    canvasWSp.valueProperty().addListener((o,ov,nv) -> gridPane.setCellSize(nv, canvasHSp.getValue()));
    canvasHSp.valueProperty().addListener((o,ov,nv) -> gridPane.setCellSize(canvasWSp.getValue(), nv));

    return mb;
}

    private void applyFixedRangeFromFields() {
        if (rangeModeGroup.getSelectedToggle() != rangeFixedBtn) return;
        double mn = parseDoubleSafe(vminField.getText(), Double.NaN);
        double mx = parseDoubleSafe(vmaxField.getText(), Double.NaN);
        if (Double.isFinite(mn) && Double.isFinite(mx)) {
            gridPane.setRangeModeFixed(mn, mx);
        }
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

    public void setToastHandler(Consumer<String> handler) { this.toastHandler = handler; }

    public void setOnCellClick(Consumer<PairGridPane.PairItem> handler) { this.onCellClickHandler = handler; }

    // --- Run ---

    public void runWithRecipe(JpdfRecipe recipe) {
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

        gridPane.clearItems();
        setControlsDisabled(true);
        setProgressText("Preparing…");
        toast("Computing pairwise densities…", false);

        AtomicInteger completed = new AtomicInteger(0);
        final int flushN = safeSpinnerInt(flushSizeSpinner, 6);
        final int flushMs = safeSpinnerInt(flushIntervalSpinner, 80);
        final boolean doIncrementalSort = incrementalSortCheck.isSelected();

        final java.util.function.Consumer<JpdfBatchEngine.PairJobResult> onResult = job -> {
            if (job == null || job.density == null) return;

            String xLbl = (job.i >= 0) ? ("Comp " + job.i) : "X";
            String yLbl = (job.j >= 0) ? ("Comp " + job.j) : "Y";

            PairGridPane.PairItem.Builder b = PairGridPane.PairItem
                    .newBuilder(xLbl, yLbl)
                    .indices(job.i >= 0 ? job.i : null, job.j >= 0 ? job.j : null)
                    .from(job.density, false, true)
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
                                batch = JpdfBatchEngine.runWhitelistPairs(cohortA, runRecipe, policy, PairwiseJpdfView.this.cache, onResult);
                        default ->
                                batch = JpdfBatchEngine.runComponentPairs(cohortA, runRecipe, policy, PairwiseJpdfView.this.cache, onResult);
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

    private int safeSpinnerInt(Spinner<Integer> sp, int fallback) {
        try {
            sp.commitValue();
            Integer v = sp.getValue();
            return (v == null) ? fallback : v;
        } catch (Throwable ignore) {
            return fallback;
        }
    }

    private static double parseDoubleSafe(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception ex) { return def; }
    }

    private static String safeFilename(String s) {
        String base = (s == null ? "image" : s);
        return base.replaceAll("[^a-zA-Z0-9._-]+", "_");
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

    public PairwiseJpdfConfigPanel getConfigPanel() { return configPanel; }
    public PairGridPane getGridPane() { return gridPane; }
    public SplitPane getSplitPane() { return splitPane; }

    public void cancelRun() {
        Thread t = workerThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
    }

    public List<FeatureVector> getCohortA() { return cohortA; }
    public List<FeatureVector> getCohortB() { return cohortB; }
    public String getCohortALabel() { return cohortALabel; }
    public String getCohortBLabel() { return cohortBLabel; }

    // --- Search / Sort helpers ---
    private void applySearchPredicate(String query) {
        final String q = (query == null) ? "" : query.trim();
        if (q.isEmpty()) {
            gridPane.setFilter(null);
            return;
        }
        final String regex = globToRegex(q.toLowerCase());
        gridPane.setFilter(pi -> {
            String a = (pi.xLabel == null ? "" : pi.xLabel.toLowerCase());
            String b = (pi.yLabel == null ? "" : pi.yLabel.toLowerCase());
            String both = (a + " vs " + b);
            return a.matches(regex) || b.matches(regex) || both.matches(regex);
        });
    }

    private void applySortComparator() {
        final String key = sortCombo.getValue();
        final boolean asc = sortAscCheck.isSelected();
        gridPane.setSorter((a, b) -> {
            int s;
            switch (key) {
                case "i" -> {
                    int ai = a.iIndex == null ? Integer.MIN_VALUE : a.iIndex;
                    int bi = b.iIndex == null ? Integer.MIN_VALUE : b.iIndex;
                    s = Integer.compare(ai, bi);
                }
                case "j" -> {
                    int aj = a.jIndex == null ? Integer.MIN_VALUE : a.jIndex;
                    int bj = b.jIndex == null ? Integer.MIN_VALUE : b.jIndex;
                    s = Integer.compare(aj, bj);
                }
                case "label" -> {
                    String al = (a.xLabel + " | " + a.yLabel);
                    String bl = (b.xLabel + " | " + b.yLabel);
                    s = al.compareToIgnoreCase(bl);
                }
                default -> {
                    double sa = (a.score == null ? Double.NEGATIVE_INFINITY : a.score);
                    double sb = (b.score == null ? Double.NEGATIVE_INFINITY : b.score);
                    s = Double.compare(sa, sb);
                }
            }
            return asc ? s : -s;
        });
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                case '.', '(', ')', '+', '|', '^', '$', '@', '%', '{', '}', '[', ']', '\\' -> sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        return "^" + sb + "$";
    }

    // --- Save/Load handlers ---
    private void doSaveRecipe() {
        try {
            JpdfRecipe r = configPanel.snapshotRecipe();
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            fc.setInitialFileName(r.getName().replaceAll("\\s+", "_") + ".json");
            var f = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
            if (f == null) return;
            try (FileOutputStream fos = new FileOutputStream(f)) {
                RecipeIo.write(fos, r);
            }
            toast("Saved recipe to " + f.getName(), false);
        } catch (Exception ex) {
            toast("Save failed: " + ex.getMessage(), true);
        }
    }

    private void doLoadRecipe() {
        try {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            var f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f == null) return;
            JpdfRecipe r;
            try (FileInputStream fis = new FileInputStream(f)) {
                r = RecipeIo.read(fis);
            }
            configPanel.applyRecipe(r);
            toast("Loaded recipe from " + f.getName(), false);
        } catch (Exception ex) {
            toast("Load failed: " + ex.getMessage(), true);
        }
    }
}
