package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.utils.statistics.JpdfRecipe.BoundsPolicy;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe.OutputKind;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe.PairSelection;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe.ScoreMetric;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * PairwiseJpdfConfigPanel
 * -----------------------
 * Floating JavaFX pane that collects configuration for a JpdfRecipe and emits it via a callback.
 *
 * Notes:
 * - Pure UI: it does not perform computation. Use setOnRun(...) to receive a built JpdfRecipe.
 * - "Whitelist" entry is a free-text placeholder here; integrate with a dedicated AxisPair editor later.
 *
 * @author Sean Phillips
 */
public final class PairwiseJpdfConfigPanel extends BorderPane {

    // --- Callbacks ---
    private Consumer<JpdfRecipe> onRun;

    // --- General ---
    private final TextField recipeNameField = new TextField();
    private final TextArea descriptionArea = new TextArea();

    // --- Pair selection ---
    private final ComboBox<PairSelection> pairSelectionCombo = new ComboBox<>();
    private final ComboBox<ScoreMetric> scoreMetricCombo = new ComboBox<>();
    private final Spinner<Integer> topKSpinner = new Spinner<>();
    private final Spinner<Double> thresholdSpinner = new Spinner<>();
    private final CheckBox componentPairsCheck = new CheckBox("Enumerate Component Pairs (i,j)");
    private final Spinner<Integer> compStartSpinner = new Spinner<>();
    private final Spinner<Integer> compEndSpinner = new Spinner<>();
    private final CheckBox includeSelfPairsCheck = new CheckBox("Include (i,i)");
    private final CheckBox orderedPairsCheck = new CheckBox("Treat pairs as ordered (i,j) and (j,i)");

    // --- Grid / Bounds ---
    private final Spinner<Integer> binsXSpinner = new Spinner<>();
    private final Spinner<Integer> binsYSpinner = new Spinner<>();
    private final ComboBox<BoundsPolicy> boundsPolicyCombo = new ComboBox<>();
    private final TextField canonicalPolicyIdField = new TextField("default");

    // --- Data sufficiency guard ---
    private final Spinner<Double> minAvgCountPerCellSpinner = new Spinner<>();

    // --- Outputs & runtime ---
    private final ComboBox<OutputKind> outputKindCombo = new ComboBox<>();
    private final CheckBox cacheEnabledCheck = new CheckBox("Enable Cache");
    private final CheckBox saveThumbsCheck = new CheckBox("Save Thumbnails");

    // --- Cohort labels (for provenance/reporting) ---
    private final TextField cohortAField = new TextField("A");
    private final TextField cohortBField = new TextField("B");

    // --- Whitelist placeholder (optional future editor) ---
    private final TextArea whitelistArea = new TextArea();

    // --- Actions ---
    private final Button runButton = new Button("Run");
    private final Button resetButton = new Button("Reset");

    public PairwiseJpdfConfigPanel() {
        setPadding(new Insets(10));

        // Defaults
        recipeNameField.setPromptText("Recipe name (required)");
        descriptionArea.setPromptText("Optional description...");
        descriptionArea.setPrefRowCount(2);

        pairSelectionCombo.getItems().addAll(PairSelection.values());
        pairSelectionCombo.setValue(PairSelection.ALL);

        scoreMetricCombo.getItems().addAll(ScoreMetric.values());
        scoreMetricCombo.setValue(ScoreMetric.PEARSON);

        topKSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 20, 1));
        topKSpinner.setEditable(true);

        thresholdSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1.0, 0.2, 0.01));
        thresholdSpinner.setEditable(true);

        componentPairsCheck.setSelected(true);
        compStartSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, 0, 1));
        compStartSpinner.setEditable(true);
        compEndSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, 1, 1));
        compEndSpinner.setEditable(true);

        includeSelfPairsCheck.setSelected(false);
        orderedPairsCheck.setSelected(false);

        binsXSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 4096, 64, 2));
        binsXSpinner.setEditable(true);
        binsYSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 4096, 64, 2));
        binsYSpinner.setEditable(true);

        boundsPolicyCombo.getItems().addAll(BoundsPolicy.values());
        boundsPolicyCombo.setValue(BoundsPolicy.DATA_MIN_MAX);

        minAvgCountPerCellSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e9, 3.0, 0.5));
        minAvgCountPerCellSpinner.setEditable(true);

        outputKindCombo.getItems().addAll(OutputKind.values());
        outputKindCombo.setValue(OutputKind.PDF_AND_CDF);

        cacheEnabledCheck.setSelected(true);
        saveThumbsCheck.setSelected(true);

        whitelistArea.setPromptText("Optional whitelist of axis pairs (placeholder).\nIntegrate with an AxisPair editor later.");
        whitelistArea.setPrefRowCount(3);

        // Layout
        setCenter(buildForm());
        setBottom(buildButtons());

        // Reactive enable/disable
        pairSelectionCombo.valueProperty().addListener((obs, ov, nv) -> updateEnablement());
        updateEnablement();

        // Actions
        runButton.setOnAction(e -> onRun());
        resetButton.setOnAction(e -> resetToDefaults());
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /** Set a callback to receive a fully-built JpdfRecipe when the user clicks Run. */
    public void setOnRun(Consumer<JpdfRecipe> onRun) {
        this.onRun = onRun;
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private VBox buildForm() {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.setPadding(new Insets(8));

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(32);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(68);
        g.getColumnConstraints().addAll(c0, c1);

        int r = 0;
        g.add(new Label("Name"), 0, r); g.add(recipeNameField, 1, r++);

        g.add(new Label("Description"), 0, r); g.add(descriptionArea, 1, r++);

        g.add(new Label("Pair Selection"), 0, r); g.add(pairSelectionCombo, 1, r++);

        HBox scoreBox = new HBox(10, new Label("Score Metric"), scoreMetricCombo);
        HBox topkBox = new HBox(10, new Label("Top-K"), topKSpinner);
        HBox thBox = new HBox(10, new Label("Threshold"), thresholdSpinner);
        VBox selectBox = new VBox(6, scoreBox, topkBox, thBox);
        g.add(new Label("Preselection Options"), 0, r); g.add(selectBox, 1, r++);

        VBox compBox = new VBox(6,
                componentPairsCheck,
                new HBox(10, new Label("Component Start"), compStartSpinner),
                new HBox(10, new Label("Component End"), compEndSpinner),
                new HBox(10, includeSelfPairsCheck, orderedPairsCheck)
        );
        g.add(new Label("Components"), 0, r); g.add(compBox, 1, r++);

        VBox gridBox = new VBox(6,
                new HBox(10, new Label("Bins X"), binsXSpinner, new Label("Bins Y"), binsYSpinner),
                new HBox(10, new Label("Bounds Policy"), boundsPolicyCombo),
                new HBox(10, new Label("Canonical Policy Id"), canonicalPolicyIdField)
        );
        g.add(new Label("Grid / Bounds"), 0, r); g.add(gridBox, 1, r++);

        g.add(new Label("Min Avg Count / Cell"), 0, r); g.add(minAvgCountPerCellSpinner, 1, r++);

        VBox outBox = new VBox(6,
                new HBox(10, new Label("Output"), outputKindCombo),
                new HBox(10, cacheEnabledCheck, saveThumbsCheck)
        );
        g.add(new Label("Outputs"), 0, r); g.add(outBox, 1, r++);

        VBox cohortsBox = new VBox(6,
                new HBox(10, new Label("Cohort A"), cohortAField),
                new HBox(10, new Label("Cohort B"), cohortBField)
        );
        g.add(new Label("Cohorts"), 0, r); g.add(cohortsBox, 1, r++);

        g.add(new Label("Whitelist (optional)"), 0, r); g.add(whitelistArea, 1, r++);

        VBox root = new VBox(8,
                g,
                new Separator()
        );
        root.setPadding(new Insets(4));
        return root;
    }

    private HBox buildButtons() {
        HBox box = new HBox(10, resetButton, runButton);
        box.setAlignment(Pos.CENTER_RIGHT);
        BorderPane.setMargin(box, new Insets(8, 8, 8, 8));
        HBox.setHgrow(runButton, Priority.NEVER);
        return box;
    }

    private void updateEnablement() {
        PairSelection ps = pairSelectionCombo.getValue();
        boolean needTopK = ps == PairSelection.TOP_K_BY_SCORE;
        boolean needThreshold = ps == PairSelection.THRESHOLD_BY_SCORE;
        boolean needScoring = ps == PairSelection.TOP_K_BY_SCORE || ps == PairSelection.THRESHOLD_BY_SCORE;

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
                .description(descriptionArea.getText() == null ? "" : descriptionArea.getText())
                .pairSelection(ps)
                .scoreMetric(sm)
                .bins(bx, by)
                .boundsPolicy(bp)
                .canonicalPolicyId(canonicalId == null ? "" : canonicalId.trim())
                .minAvgCountPerCell(minAvgCountPerCellSpinner.getValue())
                .outputKind(outputKindCombo.getValue())
                .cacheEnabled(cacheEnabledCheck.isSelected())
                .saveThumbnails(saveThumbsCheck.isSelected())
                .cohortLabels(
                        safe(cohortAField.getText(), "A"),
                        safe(cohortBField.getText(), "B")
                )
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

        // NOTE: For WHITELIST mode, integrate an AxisPair editor later and call:
        //   b.clearAxisPairs(); b.addAxisPair(...);
        // For now we just emit the recipe with empty explicit list (builder will validate non-empty if WHITELIST is chosen).
        return b.build();
    }

    private static String safe(String s, String fallback) {
        String t = s == null ? "" : s.trim();
        return t.isEmpty() ? fallback : t;
    }

    private void resetToDefaults() {
        recipeNameField.clear();
        descriptionArea.clear();

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

        cohortAField.setText("A");
        cohortBField.setText("B");

        whitelistArea.clear();
        updateEnablement();
    }
}
