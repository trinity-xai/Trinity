package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.utils.graph.GraphLayoutParams;
import edu.jhuapl.trinity.utils.graph.GraphLayoutParams.EdgePolicy;
import edu.jhuapl.trinity.utils.graph.GraphLayoutParams.LayoutKind;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * GraphControlsView
 * ------------------------------------------------------------
 * Exposes layout + edge-sparsification + FR knobs.
 * Fires:
 * - GraphEvent.PARAMS_CHANGED with a copy of GraphLayoutParams on any change
 * - GraphEvent.REBUILD_WITH_PARAMS on "Rebuild Graph"
 * - GraphEvent.RESET_PARAMS on "Reset Defaults"
 * <p>
 * NOTE: Weight mapping (DIRECT vs INVERSE_FOR_DIVERGENCE) is *not* in GraphLayoutParams.
 * Choose it at the call site based on matrix kind.
 */
public final class GraphControlsView extends VBox {

    // Layout
    private ComboBox<LayoutKind> layoutKindCombo;
    private Spinner<Double> radiusSpinner;
    private VBox algorithmControls;
    private boolean syncing;
    private Spinner<Integer> landmarkSpinner;
    private Spinner<Integer> seedSpinner;

    // Edge policy
    private ComboBox<EdgePolicy> edgePolicyCombo;
    private Spinner<Integer> kSpinner;
    private CheckBox knnSymmetrizeCheck;
    private Spinner<Double> epsSpinner;
    private CheckBox buildMstCheck;

    // Edge caps
    private Spinner<Integer> maxEdgesSpinner;
    private Spinner<Integer> maxDegreeSpinner;
    private Spinner<Double> minWeightSpinner;
    private CheckBox norm01Check;

    // Force-FR
    private Spinner<Integer> itersSpinner;
    private Spinner<Double> stepSpinner;
    private Spinner<Double> repulseSpinner;
    private Spinner<Double> attractSpinner;
    private Spinner<Double> gravitySpinner;
    private Spinner<Double> coolingSpinner;

    private final GraphLayoutParams params;
    private final Scene scene;

    public GraphControlsView(Scene scene) {
        this.scene = scene;
        this.params = new GraphLayoutParams(); // graph-only defaults
        this.params.radius = 30.0;
        setSpacing(10);
        setPadding(new Insets(6));
        getChildren().add(buildContent());
        // Do not publish default layout settings merely by opening controls.
    }

    public GraphLayoutParams getParamsCopy() {
        return copyParams(params);
    }

    public void setParams(GraphLayoutParams p) {
        if (p == null) return;
        copyInto(p, params);
        syncControlsFromParams();
        // External synchronization must not trigger a layout recalculation.
    }

    // UI -----------------------------------------------------------------

    private Pane buildContent() {
        algorithmControls = new VBox(8);
        VBox root = new VBox(10,
            titledBox("Layout", buildLayoutGrid()),
            algorithmControls,
            buildActionsBar()
        );
        updateAlgorithmControls();
        return root;
    }

    private GridPane buildLayoutGrid() {
        GridPane gp = formGrid();
        layoutKindCombo = new ComboBox<>();
        for (LayoutKind kind : LayoutKind.values()) {
            // MDS belongs in Projections; matrix sparsification is not a layout control.
            if (kind != LayoutKind.MDS_3D) layoutKindCombo.getItems().add(kind);
        }
        layoutKindCombo.getSelectionModel().select(params.kind);
        styleCombo(layoutKindCombo);
        addRow(gp, 0, "Algorithm", layoutKindCombo);
        layoutKindCombo.setOnAction(e -> {
            if (syncing || layoutKindCombo.getValue() == null) return;
            params.kind = layoutKindCombo.getValue();
            updateAlgorithmControls();
            fireParamsChanged();
        });
        return gp;
    }

    /** Only expose settings that are consumed by the selected implementation. */
    private void updateAlgorithmControls() {
        if (algorithmControls == null || layoutKindCombo == null) return;
        algorithmControls.getChildren().clear();
        LayoutKind kind = layoutKindCombo.getValue();
        if (kind == null) return;
        GridPane gp = formGrid();
        int row = 0;
        radiusSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(
            1.0, 100_000.0, Math.max(1, params.radius), 10.0));
        radiusSpinner.setEditable(true);
        styleSpinner(radiusSpinner);
        addRow(gp, row++, "Layout extent", radiusSpinner);
        radiusSpinner.valueProperty().addListener((o, ov, nv) -> {
            if (!syncing && nv != null) { params.radius = nv; fireParamsChanged(); }
        });
        boolean iterative = kind == LayoutKind.FORCE_FR || kind == LayoutKind.FORCE_ATLAS_3D
            || kind == LayoutKind.KAMADA_KAWAI_3D || kind == LayoutKind.STRESS_MAJOR_3D
            || kind == LayoutKind.SPECTRAL_3D;
        if (iterative) {
            itersSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 200_000, Math.max(1, params.iterations), 25));
            itersSpinner.setEditable(true);
            styleSpinner(itersSpinner);
            addRow(gp, row++, "Iterations", itersSpinner);
            itersSpinner.valueProperty().addListener((o, ov, nv) -> {
                if (!syncing && nv != null) { params.iterations = nv; fireParamsChanged(); }
            });
        }
        if (kind == LayoutKind.KAMADA_KAWAI_3D || kind == LayoutKind.STRESS_MAJOR_3D) {
            landmarkSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 50_000, Math.max(1, params.landmarkCount), 4));
            landmarkSpinner.setEditable(true);
            styleSpinner(landmarkSpinner);
            addRow(gp, row++, "Graph landmarks", landmarkSpinner);
            landmarkSpinner.valueProperty().addListener((o, ov, nv) -> {
                if (!syncing && nv != null) { params.landmarkCount = nv; fireParamsChanged(); }
            });
        }
        if (kind == LayoutKind.FORCE_ATLAS_3D || kind == LayoutKind.KAMADA_KAWAI_3D
            || kind == LayoutKind.STRESS_MAJOR_3D || kind == LayoutKind.SPECTRAL_3D) {
            seedSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, Integer.MAX_VALUE, (int)Math.max(0, Math.min(Integer.MAX_VALUE, params.randomSeed)), 1));
            seedSpinner.setEditable(true);
            styleSpinner(seedSpinner);
            addRow(gp, row++, "Random seed", seedSpinner);
            seedSpinner.valueProperty().addListener((o, ov, nv) -> {
                if (!syncing && nv != null) { params.randomSeed = nv; fireParamsChanged(); }
            });
        }
        if (kind == LayoutKind.FORCE_FR || kind == LayoutKind.FORCE_ATLAS_3D) {
            stepSpinner = doubleSetting(gp, row++, "Step", params.step, .0001, 1000, .01,
                v -> params.step = v);
            repulseSpinner = doubleSetting(gp, row++, "Repulsion", params.repulsion, 0, 1e9, 1,
                v -> params.repulsion = v);
            attractSpinner = doubleSetting(gp, row++, "Attraction", params.attraction, 0, 1e6, .01,
                v -> params.attraction = v);
            gravitySpinner = doubleSetting(gp, row++, "Gravity", params.gravity, 0, 10, .01,
                v -> params.gravity = v);
            coolingSpinner = doubleSetting(gp, row++, "Cooling", params.cooling, .5, .9999, .001,
                v -> params.cooling = v);
        }
        algorithmControls.getChildren().add(titledBox("Parameters — " + kind, gp));
    }

    private Spinner<Double> doubleSetting(GridPane gp, int row, String label, double initial,
                                           double min, double max, double increment,
                                           java.util.function.DoubleConsumer setter) {
        Spinner<Double> spinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(
            min, max, Math.max(min, Math.min(max, initial)), increment));
        spinner.setEditable(true);
        styleSpinner(spinner);
        addRow(gp, row, label, spinner);
        spinner.valueProperty().addListener((o, ov, nv) -> {
            if (!syncing && nv != null) { setter.accept(nv); fireParamsChanged(); }
        });
        return spinner;
    }

    private HBox buildActionsBar() {
        Button rebuildBtn = new Button("Rebuild Graph");
        rebuildBtn.setOnAction(e ->
            fireOnRoot(new GraphEvent(GraphEvent.GRAPH_REBUILD_PARAMS, copyParams(params)))
        );

        Button resetBtn = new Button("Reset Defaults");
        resetBtn.setOnAction(e -> {
            copyInto(new GraphLayoutParams(), params);
            syncControlsFromParams();
            fireOnRoot(new GraphEvent(GraphEvent.GRAPH_RESET_PARAMS, null));
            fireParamsChanged();
        });

        HBox box = new HBox(10, rebuildBtn, resetBtn);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    // Behavior ------------------------------------------------------------

    private void syncControlsFromParams() {
        syncing = true;
        try {
            layoutKindCombo.getSelectionModel().select(params.kind);
        } finally {
            syncing = false;
        }
        updateAlgorithmControls();
    }

    private void fireParamsChanged() {
        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_PARAMS_CHANGED, copyParams(params)));
    }

    private void fireOnRoot(javafx.event.Event evt) {
        if (scene != null && scene.getRoot() != null) scene.getRoot().fireEvent(evt);
        else this.fireEvent(evt);
    }

    // Utils ---------------------------------------------------------------

    private static GridPane formGrid() {
        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(6);
        gp.setAlignment(Pos.TOP_LEFT);
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(40);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(60);
        c1.setHgrow(Priority.ALWAYS);
        gp.getColumnConstraints().addAll(c0, c1);
        return gp;
    }

    private static void addRow(GridPane gp, int row, String label, javafx.scene.Node control) {
        Label l = new Label(label);
        gp.add(l, 0, row);
        if (control instanceof Spinner || control instanceof ComboBox) {
            GridPane.setHgrow(control, Priority.NEVER);
        } else {
            GridPane.setHgrow(control, Priority.ALWAYS);
            if (control instanceof Control c) c.setMaxWidth(Double.MAX_VALUE);
        }
        gp.add(control, 1, row);
    }

    private static VBox titledBox(String title, javafx.scene.Node content) {
        Label t = new Label(title);
        t.getStyleClass().add("section-title");
        VBox box = new VBox(6, t, new Separator(), content);
        box.setPadding(new Insets(4, 2, 6, 2));
        return box;
    }

    private static <T> void styleSpinner(Spinner<T> spinner) {
        spinner.setPrefWidth(125.0);
        spinner.setMaxWidth(125.0);
        spinner.setMinWidth(Region.USE_PREF_SIZE);
    }

    private static <T> void styleCombo(ComboBox<T> combo) {
        combo.setPrefWidth(220.0);
        combo.setMaxWidth(220.0);
        combo.setMinWidth(Region.USE_PREF_SIZE);
    }

    private static void styleCheck(CheckBox cb) {
        cb.setPrefWidth(100.0);
        cb.setMaxWidth(100.0);
        cb.setMinWidth(Region.USE_PREF_SIZE);
    }

    private static GraphLayoutParams copyParams(GraphLayoutParams in) {
        GraphLayoutParams p = new GraphLayoutParams();
        copyInto(in, p);
        return p;
    }

    private static void copyInto(GraphLayoutParams src, GraphLayoutParams dst) {
        // layout
        dst.kind = src.kind;
        dst.radius = src.radius;

        // force
        dst.iterations = src.iterations;
        dst.landmarkCount = src.landmarkCount;
        dst.randomSeed = src.randomSeed;
        dst.step = src.step;
        dst.repulsion = src.repulsion;
        dst.attraction = src.attraction;
        dst.gravity = src.gravity;
        dst.cooling = src.cooling;

        // edges
        dst.edgePolicy = src.edgePolicy;
        dst.knnK = src.knnK;
        dst.knnSymmetrize = src.knnSymmetrize;
        dst.epsilon = src.epsilon;
        dst.buildMst = src.buildMst;

        dst.maxEdges = src.maxEdges;
        dst.maxDegreePerNode = src.maxDegreePerNode;
        dst.minEdgeWeight = src.minEdgeWeight;
        dst.normalizeWeights01 = src.normalizeWeights01;
    }
}
