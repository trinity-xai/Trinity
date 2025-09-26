package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.utils.statistics.DivergenceComputer.DivergenceMetric;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe.BoundsPolicy;
import edu.jhuapl.trinity.utils.statistics.JpdfRecipe.ScoreMetric;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * PairwiseMatrixConfigPanel
 * -------------------------
 * Compact configuration panel for building a request to compute a pairwise matrix:
 *  - Mode: SIMILARITY (single cohort) or DIVERGENCE (A vs B)
 *  - Metric per mode (ScoreMetric for similarity, DivergenceMetric for divergence)
 *  - Component index range (inclusive), diagonal inclusion, ordered pairs
 *  - Grid bins and bounds policy (for underlying JPDF alignment)
 *  - Initial visualization hints (palette/range/legend)
 *
 * Emits a {@link Request} via {@link #setOnRun(Consumer)}.
 *
 * @author Sean Phillips
 */
public final class PairwiseMatrixConfigPanel extends BorderPane {

    /** Execution mode. */
    public enum Mode { SIMILARITY, DIVERGENCE }

    /**
     * Immutable request DTO produced by this panel.
     * Use {@link Builder} to construct internally; consumers just read fields.
     */
    public static final class Request {
        public final String name;
        public final Mode mode;

        // metrics
        public final ScoreMetric similarityMetric;         // when mode == SIMILARITY
        public final DivergenceMetric divergenceMetric;    // when mode == DIVERGENCE

        // component range & pairing
        public final int componentStart;
        public final int componentEnd;
        public final boolean includeDiagonal;
        public final boolean orderedPairs;

        // grid / bounds
        public final int binsX;
        public final int binsY;
        public final BoundsPolicy boundsPolicy;
        public final String canonicalPolicyId;

        // cohort labels (used by divergence; harmless otherwise)
        public final String cohortALabel;
        public final String cohortBLabel;

        // initial view hints
        public final boolean paletteDiverging;
        public final boolean autoRange;
        public final Double fixedMin;     // when !autoRange
        public final Double fixedMax;     // when !autoRange
        public final Double divergingCenter; // when paletteDiverging
        public final boolean showLegend;

        private Request(Builder b) {
            this.name = b.name;
            this.mode = b.mode;
            this.similarityMetric = b.similarityMetric;
            this.divergenceMetric = b.divergenceMetric;
            this.componentStart = b.componentStart;
            this.componentEnd = b.componentEnd;
            this.includeDiagonal = b.includeDiagonal;
            this.orderedPairs = b.orderedPairs;
            this.binsX = b.binsX;
            this.binsY = b.binsY;
            this.boundsPolicy = b.boundsPolicy;
            this.canonicalPolicyId = b.canonicalPolicyId;
            this.cohortALabel = b.cohortALabel;
            this.cohortBLabel = b.cohortBLabel;
            this.paletteDiverging = b.paletteDiverging;
            this.autoRange = b.autoRange;
            this.fixedMin = b.fixedMin;
            this.fixedMax = b.fixedMax;
            this.divergingCenter = b.divergingCenter;
            this.showLegend = b.showLegend;
        }

        public static final class Builder {
            private String name;
            private Mode mode = Mode.SIMILARITY;
            private ScoreMetric similarityMetric = ScoreMetric.PEARSON;
            private DivergenceMetric divergenceMetric = DivergenceMetric.JS;
            private int componentStart = 0;
            private int componentEnd = 1;
            private boolean includeDiagonal = false;
            private boolean orderedPairs = false;
            private int binsX = 64;
            private int binsY = 64;
            private BoundsPolicy boundsPolicy = BoundsPolicy.DATA_MIN_MAX;
            private String canonicalPolicyId = "default";
            private String cohortALabel = "A";
            private String cohortBLabel = "B";
            private boolean paletteDiverging = false;
            private boolean autoRange = true;
            private Double fixedMin = null;
            private Double fixedMax = null;
            private Double divergingCenter = 0.0;
            private boolean showLegend = true;

            public Builder name(String v) { this.name = v; return this; }
            public Builder mode(Mode v) { this.mode = v; return this; }
            public Builder similarityMetric(ScoreMetric v) { this.similarityMetric = v; return this; }
            public Builder divergenceMetric(DivergenceMetric v) { this.divergenceMetric = v; return this; }
            public Builder componentIndexRange(int s, int e) { this.componentStart = s; this.componentEnd = e; return this; }
            public Builder includeDiagonal(boolean v) { this.includeDiagonal = v; return this; }
            public Builder orderedPairs(boolean v) { this.orderedPairs = v; return this; }
            public Builder bins(int bx, int by) { this.binsX = bx; this.binsY = by; return this; }
            public Builder boundsPolicy(BoundsPolicy v) { this.boundsPolicy = v; return this; }
            public Builder canonicalPolicyId(String v) { this.canonicalPolicyId = v; return this; }
            public Builder cohortLabels(String a, String b) { this.cohortALabel = a; this.cohortBLabel = b; return this; }
            public Builder paletteDiverging(boolean v) { this.paletteDiverging = v; return this; }
            public Builder autoRange(boolean v) { this.autoRange = v; return this; }
            public Builder fixedRange(Double min, Double max) { this.fixedMin = min; this.fixedMax = max; return this; }
            public Builder divergingCenter(Double v) { this.divergingCenter = v; return this; }
            public Builder showLegend(boolean v) { this.showLegend = v; return this; }
            public Request build() {
                Objects.requireNonNull(name, "name");
                Objects.requireNonNull(mode, "mode");
                Objects.requireNonNull(boundsPolicy, "boundsPolicy");
                return new Request(this);
            }
        }
    }

    // ---- sizing knobs ----
    private static final double PANEL_PREF_WIDTH   = 390;
    private static final double LABEL_COL_WIDTH    = 150;
    private static final double FIELD_MIN_W        = 180;
    private static final double FIELD_PREF_W       = 240;

    private Consumer<Request> onRun;

    // General
    private final TextField nameField = new TextField();

    // Mode & Metric
    private final ComboBox<Mode> modeCombo = new ComboBox<>();
    private final ComboBox<ScoreMetric> simMetricCombo = new ComboBox<>();
    private final ComboBox<DivergenceMetric> divMetricCombo = new ComboBox<>();

    // Components
    private final Spinner<Integer> compStartSpinner = new Spinner<>();
    private final Spinner<Integer> compEndSpinner = new Spinner<>();
    private final CheckBox includeDiagonalCheck = new CheckBox("Include diagonal (i,i)");
    private final CheckBox orderedPairsCheck = new CheckBox("Ordered pairs (i,j) and (j,i)");

    // Grid / Bounds
    private final Spinner<Integer> binsXSpinner = new Spinner<>();
    private final Spinner<Integer> binsYSpinner = new Spinner<>();
    private final ComboBox<BoundsPolicy> boundsPolicyCombo = new ComboBox<>();
    private final TextField canonicalPolicyIdField = new TextField("default");

    // Visual defaults (for MatrixHeatmapView)
    private final ComboBox<String> paletteCombo = new ComboBox<>(); // "Sequential" | "Diverging"
    private final CheckBox autoRangeCheck = new CheckBox("Auto range");
    private final Spinner<Double> fixedMinSpinner = new Spinner<>();
    private final Spinner<Double> fixedMaxSpinner = new Spinner<>();
    private final Spinner<Double> divergingCenterSpinner = new Spinner<>();
    private final CheckBox showLegendCheck = new CheckBox("Show legend");

    // Cohort labels (used only in Divergence mode; harmless otherwise)
    private final TextField cohortALabelField = new TextField("A");
    private final TextField cohortBLabelField = new TextField("B");

    // Buttons (exposed)
    private final Button runButton = new Button("Run");
    private final Button resetButton = new Button("Reset");

    public PairwiseMatrixConfigPanel() {
        setPadding(new Insets(2));
        setPrefWidth(PANEL_PREF_WIDTH);
        setMaxWidth(PANEL_PREF_WIDTH);

        // General
        nameField.setPromptText("Matrix name (required)");
        nameField.setMinWidth(FIELD_MIN_W);
        nameField.setPrefWidth(FIELD_PREF_W);
        nameField.setMaxWidth(FIELD_PREF_W);

        // Mode & metrics
        modeCombo.getItems().addAll(Mode.values());
        modeCombo.setValue(Mode.SIMILARITY);
        widenCombo(modeCombo);

        simMetricCombo.getItems().addAll(ScoreMetric.values());
        simMetricCombo.setValue(ScoreMetric.PEARSON);
        widenCombo(simMetricCombo);

        divMetricCombo.getItems().addAll(DivergenceMetric.values());
        divMetricCombo.setValue(DivergenceMetric.JS);
        widenCombo(divMetricCombo);

        // Components
        compStartSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, 0, 1));
        compStartSpinner.setEditable(true);
        compEndSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, 1, 1));
        compEndSpinner.setEditable(true);
        includeDiagonalCheck.setSelected(false);
        orderedPairsCheck.setSelected(false);

        // Grid / Bounds
        binsXSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 4096, 64, 2));
        binsXSpinner.setEditable(true);
        binsYSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 4096, 64, 2));
        binsYSpinner.setEditable(true);
        boundsPolicyCombo.getItems().addAll(BoundsPolicy.values());
        boundsPolicyCombo.setValue(BoundsPolicy.DATA_MIN_MAX);
        widenCombo(boundsPolicyCombo);

        // Visual defaults
        paletteCombo.getItems().addAll("Sequential", "Diverging");
        paletteCombo.setValue("Sequential");
        widenCombo(paletteCombo);

        autoRangeCheck.setSelected(true);

        fixedMinSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-1_000_000, 1_000_000, 0.0, 0.05));
        fixedMinSpinner.setEditable(true);
        fixedMaxSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-1_000_000, 1_000_000, 1.0, 0.05));
        fixedMaxSpinner.setEditable(true);

        divergingCenterSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-1_000_000, 1_000_000, 0.0, 0.05));
        divergingCenterSpinner.setEditable(true);

        showLegendCheck.setSelected(true);

        // Cohort labels
        cohortALabelField.setPromptText("Cohort A label");
        cohortBLabelField.setPromptText("Cohort B label");
        cohortALabelField.setMinWidth(FIELD_MIN_W);
        cohortBLabelField.setMinWidth(FIELD_MIN_W);

        // Layout
        setCenter(buildForm());

        // Reactive enable/disable
        modeCombo.valueProperty().addListener((obs, ov, nv) -> updateEnablement());
        autoRangeCheck.selectedProperty().addListener((obs, ov, nv) -> updateEnablement());
        boundsPolicyCombo.valueProperty().addListener((obs, ov, nv) -> updateEnablement());
        updateEnablement();

        // Actions
        runButton.setOnAction(e -> onRun());
        resetButton.setOnAction(e -> resetToDefaults());
    }

    // ---- Public API ----

    /** Expose Run button (for toolbar placement). */
    public Button getRunButton() { return runButton; }
    /** Expose Reset button (for toolbar placement). */
    public Button getResetButton() { return resetButton; }
    /** Register callback to receive a fully-built Request when the user clicks Run. */
    public void setOnRun(Consumer<Request> onRun) { this.onRun = onRun; }
    /** Snapshot the current UI into a Request without running. */
    public Request snapshotRequest() { return buildRequestFromUI(); }

    // ---- Internals ----

    private VBox buildForm() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(6);
        g.setPadding(new Insets(2));
        g.setMaxWidth(Region.USE_PREF_SIZE);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(LABEL_COL_WIDTH);
        c0.setPrefWidth(LABEL_COL_WIDTH);
        c0.setMaxWidth(LABEL_COL_WIDTH);
        c0.setHgrow(Priority.NEVER);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(FIELD_MIN_W);
        c1.setPrefWidth(FIELD_PREF_W);
        c1.setMaxWidth(FIELD_PREF_W);
        c1.setHgrow(Priority.SOMETIMES);

        g.getColumnConstraints().addAll(c0, c1);

        int r = 0;

        // General
        g.add(compactLabel("Name"), 0, r);                        g.add(nameField, 1, r++);

        // Mode & metric
        g.add(compactLabel("Mode"), 0, r);                        g.add(modeCombo, 1, r++);
        g.add(compactLabel("Similarity Metric"), 0, r);           g.add(simMetricCombo, 1, r++);
        g.add(compactLabel("Divergence Metric"), 0, r);           g.add(divMetricCombo, 1, r++);

        // Components
        g.add(compactLabel("Component Start"), 0, r);             g.add(compStartSpinner, 1, r++);
        g.add(compactLabel("Component End"), 0, r);               g.add(compEndSpinner, 1, r++);
        g.add(compactLabel(""), 0, r);                            g.add(includeDiagonalCheck, 1, r++);
        g.add(compactLabel(""), 0, r);                            g.add(orderedPairsCheck, 1, r++);

        // Grid / Bounds
        g.add(compactLabel("Bins X"), 0, r);                      g.add(binsXSpinner, 1, r++);
        g.add(compactLabel("Bins Y"), 0, r);                      g.add(binsYSpinner, 1, r++);
        g.add(compactLabel("Bounds Policy"), 0, r);               g.add(boundsPolicyCombo, 1, r++);
        g.add(compactLabel("Canonical Policy Id"), 0, r);         g.add(canonicalPolicyIdField, 1, r++);

        // Visual defaults
        g.add(compactLabel("Palette"), 0, r);                     g.add(paletteCombo, 1, r++);
        g.add(compactLabel(""), 0, r);                            g.add(autoRangeCheck, 1, r++);
        g.add(compactLabel("Fixed Min"), 0, r);                   g.add(fixedMinSpinner, 1, r++);
        g.add(compactLabel("Fixed Max"), 0, r);                   g.add(fixedMaxSpinner, 1, r++);
        g.add(compactLabel("Diverging Center"), 0, r);            g.add(divergingCenterSpinner, 1, r++);
        g.add(compactLabel(""), 0, r);                            g.add(showLegendCheck, 1, r++);

        // Cohort labels
        g.add(compactLabel("Cohort A Label"), 0, r);              g.add(cohortALabelField, 1, r++);
        g.add(compactLabel("Cohort B Label"), 0, r);              g.add(cohortBLabelField, 1, r++);

        VBox root = new VBox(8, g);
        root.setPadding(new Insets(2));
        root.setFillWidth(false);
        return root;
    }

    private void updateEnablement() {
        Mode mode = modeCombo.getValue();
        boolean isSimilarity = mode == Mode.SIMILARITY;
        boolean isDivergence = mode == Mode.DIVERGENCE;

        simMetricCombo.setDisable(!isSimilarity);
        divMetricCombo.setDisable(!isDivergence);

        BoundsPolicy bp = boundsPolicyCombo.getValue();
        boolean needCanonical = bp == BoundsPolicy.CANONICAL_BY_FEATURE;
        canonicalPolicyIdField.setDisable(!needCanonical);

        boolean auto = autoRangeCheck.isSelected();
        fixedMinSpinner.setDisable(auto);
        fixedMaxSpinner.setDisable(auto);

        boolean diverging = "Diverging".equalsIgnoreCase(paletteCombo.getValue());
        divergingCenterSpinner.setDisable(!diverging);
    }

    private void onRun() {
        try {
            Request req = buildRequestFromUI();
            if (onRun != null) onRun.accept(req);
        } catch (IllegalArgumentException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage());
            a.setHeaderText("Invalid Matrix Configuration");
            a.setTitle("Pairwise Matrix Error");
            a.showAndWait();
        }
    }

    private Request buildRequestFromUI() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isBlank()) throw new IllegalArgumentException("Matrix name is required.");

        int start = compStartSpinner.getValue();
        int end = compEndSpinner.getValue();
        if (end < start) throw new IllegalArgumentException("Component index end must be ≥ start.");

        int bx = binsXSpinner.getValue();
        int by = binsYSpinner.getValue();
        if (bx < 2 || by < 2) throw new IllegalArgumentException("Bins must be ≥ 2.");

        BoundsPolicy bp = boundsPolicyCombo.getValue();
        String canonicalId = canonicalPolicyIdField.getText();
        if (bp == BoundsPolicy.CANONICAL_BY_FEATURE) {
            if (canonicalId == null || canonicalId.isBlank()) {
                throw new IllegalArgumentException("Canonical policy id is required for CANONICAL_BY_FEATURE.");
            }
        }

        Mode mode = modeCombo.getValue();
        ScoreMetric simMetric = simMetricCombo.getValue();
        DivergenceMetric divMetric = divMetricCombo.getValue();

        boolean auto = autoRangeCheck.isSelected();
        Double vmin = null, vmax = null, center = null;
        if (!auto) {
            vmin = fixedMinSpinner.getValue();
            vmax = fixedMaxSpinner.getValue();
            if (!(vmax > vmin)) {
                throw new IllegalArgumentException("When Auto range is off, Fixed Max must be > Fixed Min.");
            }
        }
        if ("Diverging".equalsIgnoreCase(paletteCombo.getValue())) {
            center = divergingCenterSpinner.getValue();
        }

        return new Request.Builder()
                .name(name)
                .mode(mode)
                .similarityMetric(simMetric == null ? ScoreMetric.PEARSON : simMetric)
                .divergenceMetric(divMetric == null ? DivergenceMetric.JS : divMetric)
                .componentIndexRange(start, end)
                .includeDiagonal(includeDiagonalCheck.isSelected())
                .orderedPairs(orderedPairsCheck.isSelected())
                .bins(bx, by)
                .boundsPolicy(bp)
                .canonicalPolicyId(canonicalId == null ? "" : canonicalId.trim())
                .cohortLabels(
                        safeText(cohortALabelField.getText(), "A"),
                        safeText(cohortBLabelField.getText(), "B")
                )
                .paletteDiverging(center != null)
                .autoRange(auto)
                .fixedRange(vmin, vmax)
                .divergingCenter(center)
                .showLegend(showLegendCheck.isSelected())
                .build();
    }

    private static String safeText(String s, String def) {
        return (s == null || s.isBlank()) ? def : s.trim();
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

    /** Reset fields to sensible defaults. */
    public void resetToDefaults() {
        nameField.clear();
        modeCombo.setValue(Mode.SIMILARITY);
        simMetricCombo.setValue(ScoreMetric.PEARSON);
        divMetricCombo.setValue(DivergenceMetric.JS);

        compStartSpinner.getValueFactory().setValue(0);
        compEndSpinner.getValueFactory().setValue(1);
        includeDiagonalCheck.setSelected(false);
        orderedPairsCheck.setSelected(false);

        binsXSpinner.getValueFactory().setValue(64);
        binsYSpinner.getValueFactory().setValue(64);
        boundsPolicyCombo.setValue(BoundsPolicy.DATA_MIN_MAX);
        canonicalPolicyIdField.setText("default");

        paletteCombo.setValue("Sequential");
        autoRangeCheck.setSelected(true);
        fixedMinSpinner.getValueFactory().setValue(0.0);
        fixedMaxSpinner.getValueFactory().setValue(1.0);
        divergingCenterSpinner.getValueFactory().setValue(0.0);
        showLegendCheck.setSelected(true);

        cohortALabelField.setText("A");
        cohortBLabelField.setText("B");

        updateEnablement();
    }
}
