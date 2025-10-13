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
 *   - GraphEvent.PARAMS_CHANGED with a copy of GraphLayoutParams on any change
 *   - GraphEvent.REBUILD_WITH_PARAMS on "Rebuild Graph"
 *   - GraphEvent.RESET_PARAMS on "Reset Defaults"
 *
 * NOTE: Weight mapping (DIRECT vs INVERSE_FOR_DIVERGENCE) is *not* in GraphLayoutParams.
 * Choose it at the call site based on matrix kind.
 */
public final class GraphControlsView extends VBox {

    // Layout
    private ComboBox<LayoutKind> layoutKindCombo;
    private Spinner<Double> radiusSpinner;

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
        this.params = new GraphLayoutParams(); // your defaults
        setSpacing(10);
        setPadding(new Insets(6));
        getChildren().add(buildContent());
        fireParamsChanged(); // publish defaults once
    }

    public GraphLayoutParams getParamsCopy() {
        return copyParams(params);
    }
    public void setParams(GraphLayoutParams p) {
        if (p == null) return;
        copyInto(p, params);
        syncControlsFromParams();
        fireParamsChanged();
    }

    // UI -----------------------------------------------------------------

    private Pane buildContent() {
        return new VBox(10,
            titledBox("Layout", buildLayoutGrid()),
            titledBox("Edges / Sparsification", buildEdgesGrid()),
            titledBox("Force-Directed (FR)", buildForceGrid()),
            buildActionsBar()
        );
    }

    private GridPane buildLayoutGrid() {
        GridPane gp = formGrid();

        layoutKindCombo = new ComboBox<>();
        layoutKindCombo.getItems().addAll(LayoutKind.values());
        layoutKindCombo.getSelectionModel().select(params.kind);
        styleCombo(layoutKindCombo);
        addRow(gp, 0, "Kind", layoutKindCombo);

        radiusSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(10.0, 100_000.0, params.radius, 10.0));
        radiusSpinner.setEditable(true);
        styleSpinner(radiusSpinner);
        addRow(gp, 1, "Radius", radiusSpinner);

        layoutKindCombo.setOnAction(e -> {
            params.kind = layoutKindCombo.getValue();
            toggleForceSectionVisibility();
            fireParamsChanged();
        });
        radiusSpinner.valueProperty().addListener((o, ov, nv) -> {
            params.radius = nv;
            fireParamsChanged();
        });

        return gp;
    }

    private GridPane buildEdgesGrid() {
        GridPane gp = formGrid();

        edgePolicyCombo = new ComboBox<>();
        edgePolicyCombo.getItems().addAll(EdgePolicy.values());
        edgePolicyCombo.getSelectionModel().select(params.edgePolicy);
        styleCombo(edgePolicyCombo);
        addRow(gp, 0, "Policy", edgePolicyCombo);

        kSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, params.knnK, 1));
        kSpinner.setEditable(true);
        styleSpinner(kSpinner);
        addRow(gp, 1, "k (KNN/MST+K)", kSpinner);

        knnSymmetrizeCheck = new CheckBox("Symmetrize KNN");
        knnSymmetrizeCheck.setSelected(params.knnSymmetrize);
        styleCheck(knnSymmetrizeCheck);
        addRow(gp, 2, "", knnSymmetrizeCheck);

        epsSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e9, params.epsilon, 0.1));
        epsSpinner.setEditable(true);
        styleSpinner(epsSpinner);
        addRow(gp, 3, "ε (Epsilon)", epsSpinner);

        buildMstCheck = new CheckBox("Build MST (for MST+K)");
        buildMstCheck.setSelected(params.buildMst);
        styleCheck(buildMstCheck);
        addRow(gp, 4, "", buildMstCheck);

        maxEdgesSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 2_000_000, params.maxEdges, 100));
        maxEdgesSpinner.setEditable(true);
        styleSpinner(maxEdgesSpinner);
        addRow(gp, 5, "Max edges", maxEdgesSpinner);

        maxDegreeSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10_000, params.maxDegreePerNode, 1));
        maxDegreeSpinner.setEditable(true);
        styleSpinner(maxDegreeSpinner);
        addRow(gp, 6, "Max degree/node", maxDegreeSpinner);

        minWeightSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e9, params.minEdgeWeight, 0.01));
        minWeightSpinner.setEditable(true);
        styleSpinner(minWeightSpinner);
        addRow(gp, 7, "Min edge weight", minWeightSpinner);

        norm01Check = new CheckBox("Normalize weights [0,1]");
        norm01Check.setSelected(params.normalizeWeights01);
        styleCheck(norm01Check);
        addRow(gp, 8, "", norm01Check);

        edgePolicyCombo.setOnAction(e -> {
            params.edgePolicy = edgePolicyCombo.getValue();
            toggleEdgePolicyFields();
            fireParamsChanged();
        });
        kSpinner.valueProperty().addListener((o, ov, nv) -> { params.knnK = nv; fireParamsChanged(); });
        knnSymmetrizeCheck.setOnAction(e -> { params.knnSymmetrize = knnSymmetrizeCheck.isSelected(); fireParamsChanged(); });
        epsSpinner.valueProperty().addListener((o, ov, nv) -> { params.epsilon = nv; fireParamsChanged(); });
        buildMstCheck.setOnAction(e -> { params.buildMst = buildMstCheck.isSelected(); fireParamsChanged(); });
        maxEdgesSpinner.valueProperty().addListener((o, ov, nv) -> { params.maxEdges = nv; fireParamsChanged(); });
        maxDegreeSpinner.valueProperty().addListener((o, ov, nv) -> { params.maxDegreePerNode = nv; fireParamsChanged(); });
        minWeightSpinner.valueProperty().addListener((o, ov, nv) -> { params.minEdgeWeight = nv; fireParamsChanged(); });
        norm01Check.setOnAction(e -> { params.normalizeWeights01 = norm01Check.isSelected(); fireParamsChanged(); });

        toggleEdgePolicyFields();
        return gp;
    }

    private GridPane buildForceGrid() {
        GridPane gp = formGrid();

        itersSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200_000, params.iterations, 10));
        itersSpinner.setEditable(true);
        styleSpinner(itersSpinner);
        addRow(gp, 0, "Iterations", itersSpinner);

        stepSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0001, 1000.0, params.step, 0.01));
        stepSpinner.setEditable(true);
        styleSpinner(stepSpinner);
        addRow(gp, 1, "Step", stepSpinner);

        repulseSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e9, params.repulsion, 1.0));
        repulseSpinner.setEditable(true);
        styleSpinner(repulseSpinner);
        addRow(gp, 2, "Repulsion", repulseSpinner);

        attractSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1e6, params.attraction, 0.01));
        attractSpinner.setEditable(true);
        styleSpinner(attractSpinner);
        addRow(gp, 3, "Attraction", attractSpinner);

        gravitySpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10.0, params.gravity, 0.01));
        gravitySpinner.setEditable(true);
        styleSpinner(gravitySpinner);
        addRow(gp, 4, "Gravity", gravitySpinner);

        coolingSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 0.9999, params.cooling, 0.001));
        coolingSpinner.setEditable(true);
        styleSpinner(coolingSpinner);
        addRow(gp, 5, "Cooling", coolingSpinner);

        itersSpinner.valueProperty().addListener((o, ov, nv) -> { params.iterations = nv; fireParamsChanged(); });
        stepSpinner.valueProperty().addListener((o, ov, nv) -> { params.step = nv; fireParamsChanged(); });
        repulseSpinner.valueProperty().addListener((o, ov, nv) -> { params.repulsion = nv; fireParamsChanged(); });
        attractSpinner.valueProperty().addListener((o, ov, nv) -> { params.attraction = nv; fireParamsChanged(); });
        gravitySpinner.valueProperty().addListener((o, ov, nv) -> { params.gravity = nv; fireParamsChanged(); });
        coolingSpinner.valueProperty().addListener((o, ov, nv) -> { params.cooling = nv; fireParamsChanged(); });

        toggleForceSectionVisibility();
        return gp;
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

    private void toggleEdgePolicyFields() {
        EdgePolicy p = edgePolicyCombo.getValue();
        boolean useK = (p == EdgePolicy.KNN || p == EdgePolicy.MST_PLUS_K);
        boolean useEps = (p == EdgePolicy.EPSILON);

        kSpinner.setDisable(!useK);
        knnSymmetrizeCheck.setDisable(!(p == EdgePolicy.KNN)); // only meaningful for KNN
        epsSpinner.setDisable(!useEps);
        buildMstCheck.setDisable(p != EdgePolicy.MST_PLUS_K);
    }

    private void toggleForceSectionVisibility() {
        boolean fr = (layoutKindCombo.getValue() == LayoutKind.FORCE_FR);
        itersSpinner.setDisable(!fr);
        stepSpinner.setDisable(!fr);
        repulseSpinner.setDisable(!fr);
        attractSpinner.setDisable(!fr);
        gravitySpinner.setDisable(!fr);
        coolingSpinner.setDisable(!fr);
    }

    private void syncControlsFromParams() {
        layoutKindCombo.getSelectionModel().select(params.kind);
        radiusSpinner.getValueFactory().setValue(params.radius);

        edgePolicyCombo.getSelectionModel().select(params.edgePolicy);
        kSpinner.getValueFactory().setValue(params.knnK);
        knnSymmetrizeCheck.setSelected(params.knnSymmetrize);
        epsSpinner.getValueFactory().setValue(params.epsilon);
        buildMstCheck.setSelected(params.buildMst);

        maxEdgesSpinner.getValueFactory().setValue(params.maxEdges);
        maxDegreeSpinner.getValueFactory().setValue(params.maxDegreePerNode);
        minWeightSpinner.getValueFactory().setValue(params.minEdgeWeight);
        norm01Check.setSelected(params.normalizeWeights01);

        itersSpinner.getValueFactory().setValue(params.iterations);
        stepSpinner.getValueFactory().setValue(params.step);
        repulseSpinner.getValueFactory().setValue(params.repulsion);
        attractSpinner.getValueFactory().setValue(params.attraction);
        gravitySpinner.getValueFactory().setValue(params.gravity);
        coolingSpinner.getValueFactory().setValue(params.cooling);

        toggleEdgePolicyFields();
        toggleForceSectionVisibility();
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
        gp.setHgap(8); gp.setVgap(6); gp.setAlignment(Pos.TOP_LEFT);
        ColumnConstraints c0 = new ColumnConstraints(); c0.setPercentWidth(40);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(60); c1.setHgrow(Priority.ALWAYS);
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
        Label t = new Label(title); t.getStyleClass().add("section-title");
        VBox box = new VBox(6, t, new Separator(), content);
        box.setPadding(new Insets(4, 2, 6, 2));
        return box;
    }
    private static <T> void styleSpinner(Spinner<T> spinner) {
        spinner.setPrefWidth(125.0); spinner.setMaxWidth(125.0); spinner.setMinWidth(Region.USE_PREF_SIZE);
    }
    private static <T> void styleCombo(ComboBox<T> combo) {
        combo.setPrefWidth(220.0); combo.setMaxWidth(220.0); combo.setMinWidth(Region.USE_PREF_SIZE);
    }
    private static void styleCheck(CheckBox cb) {
        cb.setPrefWidth(100.0); cb.setMaxWidth(100.0); cb.setMinWidth(Region.USE_PREF_SIZE);
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
