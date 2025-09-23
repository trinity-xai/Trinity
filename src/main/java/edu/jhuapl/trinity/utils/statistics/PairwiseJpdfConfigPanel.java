package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.utils.statistics.JpdfRecipe.BoundsPolicy;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe.OutputKind;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe.PairSelection;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe.ScoreMetric;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class PairwiseJpdfConfigPanel extends BorderPane {

    // ---- sizing knobs ----
    private static final double PANEL_PREF_WIDTH   = 390;
    private static final double LABEL_COL_WIDTH    = 130; // label column
    private static final double FIELD_MIN_W        = 180; // generic control minimum
    private static final double FIELD_PREF_W       = 240; // generic control preferred
    private static final double FIELD_WIDE_W       = 300; // name field
    private static final double SPINNER_MIN_W      = 100;
    private static final double SPINNER_PREF_W     = 120;

    private Consumer<JpdfRecipe> onRun;

    // General
    private final TextField recipeNameField = new TextField();

    // Pair selection
    private final ComboBox<PairSelection> pairSelectionCombo = new ComboBox<>();
    private final ComboBox<ScoreMetric> scoreMetricCombo = new ComboBox<>();
    private final Spinner<Integer> topKSpinner = new Spinner<>();
    private final Spinner<Double> thresholdSpinner = new Spinner<>();
    private final CheckBox componentPairsCheck = new CheckBox("Enumerate Component Pairs (i,j)");
    private final Spinner<Integer> compStartSpinner = new Spinner<>();
    private final Spinner<Integer> compEndSpinner = new Spinner<>();
    private final CheckBox includeSelfPairsCheck = new CheckBox("Include (i,i)");
    private final CheckBox orderedPairsCheck = new CheckBox("Treat pairs as ordered (i,j) and (j,i)");

    // Grid / Bounds
    private final Spinner<Integer> binsXSpinner = new Spinner<>();
    private final Spinner<Integer> binsYSpinner = new Spinner<>();
    private final ComboBox<BoundsPolicy> boundsPolicyCombo = new ComboBox<>();
    private final TextField canonicalPolicyIdField = new TextField("default");

    // Guard
    private final Spinner<Double> minAvgCountPerCellSpinner = new Spinner<>();

    // Output/runtime
    private final ComboBox<OutputKind> outputKindCombo = new ComboBox<>();
    private final CheckBox cacheEnabledCheck = new CheckBox("Enable Cache");
    private final CheckBox saveThumbsCheck = new CheckBox("Save Thumbnails");

    // Whitelist (optional placeholder)
    private final TextArea whitelistArea = new TextArea();

    // Actions (exposed to parent top bar)
    private final Button runButton = new Button("Run");
    private final Button resetButton = new Button("Reset");

    public PairwiseJpdfConfigPanel() {
        setPadding(new Insets(2));
        setPrefWidth(PANEL_PREF_WIDTH);
        setMaxWidth(PANEL_PREF_WIDTH);

        // Defaults & widths
        recipeNameField.setPromptText("Recipe name (required)");
        recipeNameField.setPrefWidth(FIELD_WIDE_W);
        recipeNameField.setMinWidth(FIELD_MIN_W);
        recipeNameField.setMaxWidth(FIELD_WIDE_W);

        pairSelectionCombo.getItems().addAll(PairSelection.values());
        pairSelectionCombo.setValue(PairSelection.ALL);
        pairSelectionCombo.setMinWidth(FIELD_MIN_W);
        pairSelectionCombo.setPrefWidth(FIELD_PREF_W);
        pairSelectionCombo.setMaxWidth(FIELD_WIDE_W);

        scoreMetricCombo.getItems().addAll(ScoreMetric.values());
        scoreMetricCombo.setValue(ScoreMetric.PEARSON);
        widenCombo(scoreMetricCombo);

        topKSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 20, 1));
        topKSpinner.setEditable(true);
        topKSpinner.setMinWidth(SPINNER_MIN_W);
        topKSpinner.setPrefWidth(SPINNER_PREF_W);
        topKSpinner.setMaxWidth(SPINNER_PREF_W);

        thresholdSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1.0, 0.2, 0.01));
        thresholdSpinner.setEditable(true);
        thresholdSpinner.setMinWidth(SPINNER_MIN_W);
        thresholdSpinner.setPrefWidth(SPINNER_PREF_W);
        thresholdSpinner.setMaxWidth(SPINNER_PREF_W);

        componentPairsCheck.setSelected(true);
        includeSelfPairsCheck.setSelected(false);
        orderedPairsCheck.setSelected(false);

        compStartSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, 0, 1));
        compStartSpinner.setEditable(true);
        compStartSpinner.setMinWidth(SPINNER_MIN_W);
        compStartSpinner.setPrefWidth(SPINNER_PREF_W);
        compStartSpinner.setMaxWidth(SPINNER_PREF_W);

        compEndSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, 1, 1));
        compEndSpinner.setEditable(true);
        compEndSpinner.setMinWidth(SPINNER_MIN_W);
        compEndSpinner.setPrefWidth(SPINNER_PREF_W);
        compEndSpinner.setMaxWidth(SPINNER_PREF_W);

        binsXSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 4096, 64, 2));
        binsXSpinner.setEditable(true);
        binsXSpinner.setMinWidth(SPINNER_MIN_W);
        binsXSpinner.setPrefWidth(SPINNER_PREF_W);
        binsXSpinner.setMaxWidth(SPINNER_PREF_W);

        binsYSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 4096, 64, 2));
        binsYSpinner.setEditable(true);
        binsYSpinner.setMinWidth(SPINNER_MIN_W);
        binsYSpinner.setPrefWidth(SPINNER_PREF_W);
        binsYSpinner.setMaxWidth(SPINNER_PREF_W);

        boundsPolicyCombo.getItems().addAll(BoundsPolicy.values());
        boundsPolicyCombo.setValue(BoundsPolicy.DATA_MIN_MAX);
        widenCombo(boundsPolicyCombo);

        canonicalPolicyIdField.setPrefWidth(FIELD_PREF_W);
        canonicalPolicyIdField.setMinWidth(FIELD_MIN_W);
        canonicalPolicyIdField.setMaxWidth(FIELD_PREF_W);

        minAvgCountPerCellSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e9, 3.0, 0.5));
        minAvgCountPerCellSpinner.setEditable(true);
        minAvgCountPerCellSpinner.setMinWidth(SPINNER_MIN_W);
        minAvgCountPerCellSpinner.setPrefWidth(SPINNER_PREF_W);
        minAvgCountPerCellSpinner.setMaxWidth(SPINNER_PREF_W);

        outputKindCombo.getItems().addAll(OutputKind.values());
        outputKindCombo.setValue(OutputKind.PDF_AND_CDF);
        widenCombo(outputKindCombo);

        cacheEnabledCheck.setSelected(true);
        saveThumbsCheck.setSelected(true);

        whitelistArea.setPromptText("Optional whitelist of axis pairs (placeholder).");
        whitelistArea.setPrefRowCount(2);
        whitelistArea.setWrapText(true);
        whitelistArea.setPrefWidth(FIELD_PREF_W);
        whitelistArea.setMinWidth(FIELD_MIN_W);
        whitelistArea.setMaxWidth(FIELD_PREF_W);
        whitelistArea.setMaxHeight(Region.USE_PREF_SIZE);

        // Layout
        setCenter(buildForm());

        // Reactive enable/disable
        pairSelectionCombo.valueProperty().addListener((obs, ov, nv) -> updateEnablement());
        updateEnablement();

        // Actions
        runButton.setOnAction(e -> onRun());
        resetButton.setOnAction(e -> resetToDefaults());
    }

    /** Expose Run button so the parent pane can place it in the top bar. */
    public Button getRunButton() { return runButton; }
    /** Expose Reset button so the parent pane can place it in the top bar. */
    public Button getResetButton() { return resetButton; }

    /** Set a callback to receive a fully-built JpdfRecipe when the user clicks Run. */
    public void setOnRun(Consumer<JpdfRecipe> onRun) { this.onRun = onRun; }

    /**
     * Build a JpdfRecipe from current UI state without running.
     * @throws IllegalArgumentException if configuration is invalid
     */
    public JpdfRecipe snapshotRecipe() {
        return buildRecipeFromUI();
    }

    /**
     * Apply a recipe back into the UI controls.
     * Fields not represented in the panel are ignored.
     */
    public void applyRecipe(JpdfRecipe r) {
        if (r == null) return;
        recipeNameField.setText(r.getName());
        pairSelectionCombo.setValue(r.getPairSelection());
        scoreMetricCombo.setValue(r.getScoreMetric());
        topKSpinner.getValueFactory().setValue(r.getTopK());
        thresholdSpinner.getValueFactory().setValue(r.getScoreThreshold());
        componentPairsCheck.setSelected(r.isComponentPairsMode());
        compStartSpinner.getValueFactory().setValue(r.getComponentIndexStart());
        compEndSpinner.getValueFactory().setValue(r.getComponentIndexEnd());
        includeSelfPairsCheck.setSelected(r.isIncludeSelfPairs());
        orderedPairsCheck.setSelected(r.isOrderedPairs());
        binsXSpinner.getValueFactory().setValue(r.getBinsX());
        binsYSpinner.getValueFactory().setValue(r.getBinsY());
        boundsPolicyCombo.setValue(r.getBoundsPolicy());
        canonicalPolicyIdField.setText(r.getCanonicalPolicyId());
        minAvgCountPerCellSpinner.getValueFactory().setValue(r.getMinAvgCountPerCell());
        outputKindCombo.setValue(r.getOutputKind());
        cacheEnabledCheck.setSelected(r.isCacheEnabled());
        saveThumbsCheck.setSelected(r.isSaveThumbnails());
        updateEnablement();
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private VBox buildForm() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(6);
        g.setPadding(new Insets(2));
        g.setMaxWidth(Region.USE_PREF_SIZE);

        // Two columns: Label | Control
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(LABEL_COL_WIDTH);
        c0.setPrefWidth(LABEL_COL_WIDTH);
        c0.setMaxWidth(LABEL_COL_WIDTH);
        c0.setHgrow(Priority.NEVER);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(FIELD_MIN_W);
        c1.setPrefWidth(FIELD_PREF_W);
        c1.setMaxWidth(FIELD_WIDE_W);
        c1.setHgrow(Priority.SOMETIMES);

        g.getColumnConstraints().addAll(c0, c1);

        int r = 0;

        // General
        g.add(compactLabel("Name"), 0, r);                   g.add(recipeNameField, 1, r++);

        // Pair selection
        g.add(compactLabel("Pair Selection"), 0, r);         g.add(pairSelectionCombo, 1, r++);
        g.add(compactLabel("Score Metric"), 0, r);           g.add(scoreMetricCombo, 1, r++);
        g.add(compactLabel("Top-K"), 0, r);                  g.add(topKSpinner, 1, r++);
        g.add(compactLabel("Threshold"), 0, r);              g.add(thresholdSpinner, 1, r++);

        // Component options
        g.add(compactLabel(""), 0, r);                       g.add(componentPairsCheck, 1, r++); // checkbox row
        g.add(compactLabel("Component Start"), 0, r);        g.add(compStartSpinner, 1, r++);
        g.add(compactLabel("Component End"), 0, r);          g.add(compEndSpinner, 1, r++);
        g.add(compactLabel(""), 0, r);                       g.add(includeSelfPairsCheck, 1, r++); // checkbox row
        g.add(compactLabel(""), 0, r);                       g.add(orderedPairsCheck, 1, r++);    // checkbox row

        // Grid / Bounds
        g.add(compactLabel("Bins X"), 0, r);                 g.add(binsXSpinner, 1, r++);
        g.add(compactLabel("Bins Y"), 0, r);                 g.add(binsYSpinner, 1, r++);
        g.add(compactLabel("Bounds Policy"), 0, r);          g.add(boundsPolicyCombo, 1, r++);
        g.add(compactLabel("Canonical Policy Id"), 0, r);    g.add(canonicalPolicyIdField, 1, r++);

        // Guard
        g.add(compactLabel("Min Avg Count/Cell"), 0, r);     g.add(minAvgCountPerCellSpinner, 1, r++);

        // Outputs
        g.add(compactLabel("Output"), 0, r);                 g.add(outputKindCombo, 1, r++);
        g.add(compactLabel(""), 0, r);                       g.add(cacheEnabledCheck, 1, r++);    // checkbox row
        g.add(compactLabel(""), 0, r);                       g.add(saveThumbsCheck, 1, r++);      // checkbox row

        // Whitelist
        g.add(compactLabel("Whitelist (opt)"), 0, r);        g.add(whitelistArea, 1, r++);

        VBox root = new VBox(8, g);
        root.setPadding(new Insets(2));
        root.setFillWidth(false);
        return root;
    }

    private void updateEnablement() {
        PairSelection ps = pairSelectionCombo.getValue();
        boolean needTopK = ps == PairSelection.TOP_K_BY_SCORE;
        boolean needThreshold = ps == PairSelection.THRESHOLD_BY_SCORE;
        boolean needScoring = needTopK || needThreshold;

        scoreMetricCombo.setDisable(!needScoring);
        topKSpinner.setDisable(!needTopK);
        thresholdSpinner.setDisable(!needThreshold);
    }

    private void onRun() {
        try {
            JpdfRecipe recipe = buildRecipeFromUI();
            if (onRun != null) onRun.accept(recipe);
        } catch (IllegalArgumentException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage());
            a.setHeaderText("Invalid Configuration");
            a.setTitle("JPDF Recipe Error");
            a.showAndWait();
        }
    }

    private JpdfRecipe buildRecipeFromUI() {
        String name = recipeNameField.getText() == null ? "" : recipeNameField.getText().trim();
        if (name.isBlank()) throw new IllegalArgumentException("Recipe name is required.");

        int bx = binsXSpinner.getValue();
        int by = binsYSpinner.getValue();
        if (bx < 2 || by < 2) throw new IllegalArgumentException("Bins must be ≥ 2.");

        int start = compStartSpinner.getValue();
        int end = compEndSpinner.getValue();
        if (componentPairsCheck.isSelected() && end < start) {
            throw new IllegalArgumentException("Component index end must be ≥ start.");
        }

        PairSelection ps = pairSelectionCombo.getValue();
        int topK = topKSpinner.getValue();
        double thr = thresholdSpinner.getValue();
        ScoreMetric sm = scoreMetricCombo.getValue();

        BoundsPolicy bp = boundsPolicyCombo.getValue();
        String canonicalId = canonicalPolicyIdField.getText();
        if (bp == BoundsPolicy.CANONICAL_BY_FEATURE) {
            if (canonicalId == null || canonicalId.isBlank()) {
                throw new IllegalArgumentException("Canonical policy id is required for CANONICAL_BY_FEATURE.");
            }
        }

        JpdfRecipe.Builder b = JpdfRecipe.newBuilder(name)
                .pairSelection(ps)
                .scoreMetric(sm)
                .bins(bx, by)
                .boundsPolicy(bp)
                .canonicalPolicyId(canonicalId == null ? "" : canonicalId.trim())
                .minAvgCountPerCell(minAvgCountPerCellSpinner.getValue())
                .outputKind(outputKindCombo.getValue())
                .cacheEnabled(cacheEnabledCheck.isSelected())
                .saveThumbnails(saveThumbsCheck.isSelected())
                .componentPairsMode(componentPairsCheck.isSelected())
                .componentIndexRange(start, end)
                .includeSelfPairs(includeSelfPairsCheck.isSelected())
                .orderedPairs(orderedPairsCheck.isSelected());

        if (ps == PairSelection.TOP_K_BY_SCORE) {
            if (topK <= 0) throw new IllegalArgumentException("Top-K must be > 0.");
            b.topK(topK);
        } else if (ps == PairSelection.THRESHOLD_BY_SCORE) {
            if (!Double.isFinite(thr)) throw new IllegalArgumentException("Threshold must be finite.");
            b.scoreThreshold(thr);
        }

        return b.build();
    }

    private static Label compactLabel(String text) {
        Label l = new Label(text);
        l.setMinWidth(LABEL_COL_WIDTH);
        l.setPrefWidth(LABEL_COL_WIDTH);
        l.setMaxWidth(LABEL_COL_WIDTH);
        return l;
    }

    private static void widenCombo(ComboBox<?> cb) {
        cb.setMinWidth(FIELD_MIN_W);
        cb.setPrefWidth(FIELD_PREF_W);
        cb.setMaxWidth(FIELD_PREF_W);
    }

    public void resetToDefaults() {
        recipeNameField.clear();

        pairSelectionCombo.setValue(PairSelection.ALL);
        scoreMetricCombo.setValue(ScoreMetric.PEARSON);
        topKSpinner.getValueFactory().setValue(20);
        thresholdSpinner.getValueFactory().setValue(0.2);

        componentPairsCheck.setSelected(true);
        compStartSpinner.getValueFactory().setValue(0);
        compEndSpinner.getValueFactory().setValue(1);
        includeSelfPairsCheck.setSelected(false);
        orderedPairsCheck.setSelected(false);

        binsXSpinner.getValueFactory().setValue(64);
        binsYSpinner.getValueFactory().setValue(64);
        boundsPolicyCombo.setValue(BoundsPolicy.DATA_MIN_MAX);
        canonicalPolicyIdField.setText("default");

        minAvgCountPerCellSpinner.getValueFactory().setValue(3.0);

        outputKindCombo.setValue(OutputKind.PDF_AND_CDF);
        cacheEnabledCheck.setSelected(true);
        saveThumbsCheck.setSelected(true);

        updateEnablement();
    }
}
