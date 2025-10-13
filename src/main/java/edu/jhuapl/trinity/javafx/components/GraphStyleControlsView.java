package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.utils.graph.GraphStyleParams;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * GraphStyleControlsView
 * ------------------------------------------------------------
 * Separate tab view for graph appearance (nodes/edges).
 * <p>
 * Fires:
 * - GraphEvent.GRAPH_STYLE_PARAMS_CHANGED (object = GraphStyleParams snapshot) on change
 * - GraphEvent.GRAPH_STYLE_RESET_DEFAULTS on "Reset Style"
 * <p>
 * Listens (GUI-sync, model → UI):
 * - GraphEvent.SET_STYLE_GUI (object = GraphStyleParams)
 * - GraphEvent.SET_NODE_COLOR_GUI (object = Color)
 * - GraphEvent.SET_NODE_RADIUS_GUI (object = Double)
 * - GraphEvent.SET_NODE_OPACITY_GUI (object = Double)
 * - GraphEvent.SET_EDGE_COLOR_GUI (object = Color)
 * - GraphEvent.SET_EDGE_WIDTH_GUI (object = Double)
 * - GraphEvent.SET_EDGE_OPACITY_GUI (object = Double)
 * <p>
 * Notes:
 * - Uses sliders for opacity.
 * - Updates controls only when incoming GUI-sync differs (prevents event ping-pong).
 * - No selection scope (global apply).
 */
public final class GraphStyleControlsView extends VBox {

    // Layout constants
    private static final double SPINNER_PREF_WIDTH = 125.0;
    private static final double COLOR_PICKER_PREF_WIDTH = 150.0;

    private final Scene scene;
    private final GraphStyleParams params = new GraphStyleParams();

    // Nodes
    private ColorPicker nodeColorPicker;
    private Spinner<Double> nodeRadiusSpinner;
    private Slider nodeOpacitySlider;

    // Edges
    private ColorPicker edgeColorPicker;
    private Spinner<Double> edgeWidthSpinner;
    private Slider edgeOpacitySlider;

    // Guard to avoid echo during GUI hydration
    private boolean isUpdatingFromGuiSync = false;

    public GraphStyleControlsView(Scene scene) {
        this.scene = scene;

        setSpacing(10);
        setPadding(new Insets(6));
        getChildren().add(buildContent());
        wireGuiSyncHandlers();

        // publish defaults once
        fireParamsChanged();
    }

    // ---------------------------------------------------------------------
    // UI construction
    // ---------------------------------------------------------------------

    private VBox buildContent() {
        return new VBox(10,
            titledBox("Nodes", buildNodesGrid()),
            titledBox("Edges", buildEdgesGrid()),
            buildActionsBar()
        );
    }

    private GridPane buildNodesGrid() {
        GridPane gp = formGrid();

        nodeColorPicker = new ColorPicker(params.nodeColor);
        styleColorPicker(nodeColorPicker);
        addRow(gp, 0, "Color", nodeColorPicker);

        nodeRadiusSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(1.0, 500.0, params.nodeRadius, 1.0));
        nodeRadiusSpinner.setEditable(true);
        styleSpinner(nodeRadiusSpinner);
        addRow(gp, 1, "Radius", nodeRadiusSpinner);

        nodeOpacitySlider = new Slider(0.0, 1.0, params.nodeOpacity);
        nodeOpacitySlider.setShowTickMarks(true);
        nodeOpacitySlider.setShowTickLabels(true);
        nodeOpacitySlider.setMajorTickUnit(0.25);
        nodeOpacitySlider.setBlockIncrement(0.05);
        addRow(gp, 2, "Opacity", nodeOpacitySlider);

        nodeColorPicker.setOnAction(e -> {
            params.nodeColor = nodeColorPicker.getValue();
            fireParamsChanged();
        });
        nodeRadiusSpinner.valueProperty().addListener((o, ov, nv) -> {
            params.nodeRadius = nv;
            fireParamsChanged();
        });
        nodeOpacitySlider.valueProperty().addListener((o, ov, nv) -> {
            params.nodeOpacity = clamp01(nv.doubleValue());
            fireParamsChanged();
        });

        return gp;
    }

    private GridPane buildEdgesGrid() {
        GridPane gp = formGrid();

        edgeColorPicker = new ColorPicker(params.edgeColor);
        styleColorPicker(edgeColorPicker);
        addRow(gp, 0, "Color", edgeColorPicker);

        edgeWidthSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 200.0, params.edgeWidth, 0.5));
        edgeWidthSpinner.setEditable(true);
        styleSpinner(edgeWidthSpinner);
        addRow(gp, 1, "Width", edgeWidthSpinner);

        edgeOpacitySlider = new Slider(0.0, 1.0, params.edgeOpacity);
        edgeOpacitySlider.setShowTickMarks(true);
        edgeOpacitySlider.setShowTickLabels(true);
        edgeOpacitySlider.setMajorTickUnit(0.25);
        edgeOpacitySlider.setBlockIncrement(0.05);
        addRow(gp, 2, "Opacity", edgeOpacitySlider);

        edgeColorPicker.setOnAction(e -> {
            params.edgeColor = edgeColorPicker.getValue();
            fireParamsChanged();
        });
        edgeWidthSpinner.valueProperty().addListener((o, ov, nv) -> {
            params.edgeWidth = nv;
            fireParamsChanged();
        });
        edgeOpacitySlider.valueProperty().addListener((o, ov, nv) -> {
            params.edgeOpacity = clamp01(nv.doubleValue());
            fireParamsChanged();
        });

        return gp;
    }

    private HBox buildActionsBar() {
        Button resetBtn = new Button("Reset Style");
        resetBtn.setOnAction(e -> fireOnRoot(new GraphEvent(GraphEvent.GRAPH_STYLE_RESET_DEFAULTS, null)));

        HBox box = new HBox(10, resetBtn);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    // ---------------------------------------------------------------------
    // GUI-sync wiring (model → UI)
    // ---------------------------------------------------------------------

    private void wireGuiSyncHandlers() {
        if (scene == null) return;

        // Coarse hydrate
        scene.addEventHandler(GraphEvent.SET_STYLE_GUI, e -> {
            GraphStyleParams p = (GraphStyleParams) e.object;
            if (p == null) return;
            isUpdatingFromGuiSync = true;
            try {
                // assign directly, no copyInto()
                params.nodeColor = p.nodeColor;
                params.nodeRadius = p.nodeRadius;
                params.nodeOpacity = clamp01(p.nodeOpacity);
                params.edgeColor = p.edgeColor;
                params.edgeWidth = p.edgeWidth;
                params.edgeOpacity = clamp01(p.edgeOpacity);
                syncControlsFromParams();
            } finally {
                isUpdatingFromGuiSync = false;
            }
        });

        // Fine-grained
        scene.addEventHandler(GraphEvent.SET_NODE_COLOR_GUI, e -> {
            Color c = (Color) e.object;
            if (c == null) return;
            isUpdatingFromGuiSync = true;
            try {
                if (!Objects.equals(nodeColorPicker.getValue(), c)) nodeColorPicker.setValue(c);
                params.nodeColor = c;
            } finally {
                isUpdatingFromGuiSync = false;
            }
        });

        scene.addEventHandler(GraphEvent.SET_NODE_RADIUS_GUI, e -> {
            Double v = (Double) e.object;
            if (v == null) return;
            isUpdatingFromGuiSync = true;
            try {
                if (!Objects.equals(nodeRadiusSpinner.getValue(), v)) nodeRadiusSpinner.getValueFactory().setValue(v);
                params.nodeRadius = v;
            } finally {
                isUpdatingFromGuiSync = false;
            }
        });

        scene.addEventHandler(GraphEvent.SET_NODE_OPACITY_GUI, e -> {
            Double v = (Double) e.object;
            if (v == null) return;
            isUpdatingFromGuiSync = true;
            try {
                if (differs(nodeOpacitySlider.getValue(), v)) nodeOpacitySlider.setValue(v);
                params.nodeOpacity = clamp01(v);
            } finally {
                isUpdatingFromGuiSync = false;
            }
        });

        scene.addEventHandler(GraphEvent.SET_EDGE_COLOR_GUI, e -> {
            Color c = (Color) e.object;
            if (c == null) return;
            isUpdatingFromGuiSync = true;
            try {
                if (!Objects.equals(edgeColorPicker.getValue(), c)) edgeColorPicker.setValue(c);
                params.edgeColor = c;
            } finally {
                isUpdatingFromGuiSync = false;
            }
        });

        scene.addEventHandler(GraphEvent.SET_EDGE_WIDTH_GUI, e -> {
            Double v = (Double) e.object;
            if (v == null) return;
            isUpdatingFromGuiSync = true;
            try {
                if (!Objects.equals(edgeWidthSpinner.getValue(), v)) edgeWidthSpinner.getValueFactory().setValue(v);
                params.edgeWidth = v;
            } finally {
                isUpdatingFromGuiSync = false;
            }
        });

        scene.addEventHandler(GraphEvent.SET_EDGE_OPACITY_GUI, e -> {
            Double v = (Double) e.object;
            if (v == null) return;
            isUpdatingFromGuiSync = true;
            try {
                if (differs(edgeOpacitySlider.getValue(), v)) edgeOpacitySlider.setValue(v);
                params.edgeOpacity = clamp01(v);
            } finally {
                isUpdatingFromGuiSync = false;
            }
        });
    }

    // ---------------------------------------------------------------------
    // Behavior
    // ---------------------------------------------------------------------

    private void fireParamsChanged() {
        if (isUpdatingFromGuiSync) return; // don't echo during GUI hydration
        GraphStyleParams snap = snapshotParams();
        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_STYLE_PARAMS_CHANGED, snap));
    }

    private void syncControlsFromParams() {
        if (!Objects.equals(nodeColorPicker.getValue(), params.nodeColor))
            nodeColorPicker.setValue(params.nodeColor);
        if (!Objects.equals(nodeRadiusSpinner.getValue(), params.nodeRadius))
            nodeRadiusSpinner.getValueFactory().setValue(params.nodeRadius);
        if (differs(nodeOpacitySlider.getValue(), params.nodeOpacity))
            nodeOpacitySlider.setValue(params.nodeOpacity);

        if (!Objects.equals(edgeColorPicker.getValue(), params.edgeColor))
            edgeColorPicker.setValue(params.edgeColor);
        if (!Objects.equals(edgeWidthSpinner.getValue(), params.edgeWidth))
            edgeWidthSpinner.getValueFactory().setValue(params.edgeWidth);
        if (differs(edgeOpacitySlider.getValue(), params.edgeOpacity))
            edgeOpacitySlider.setValue(params.edgeOpacity);
    }

    private GraphStyleParams snapshotParams() {
        GraphStyleParams p = new GraphStyleParams();
        p.nodeColor = nodeColorPicker.getValue();
        p.nodeRadius = nodeRadiusSpinner.getValue();
        p.nodeOpacity = clamp01(nodeOpacitySlider.getValue());
        p.edgeColor = edgeColorPicker.getValue();
        p.edgeWidth = edgeWidthSpinner.getValue();
        p.edgeOpacity = clamp01(edgeOpacitySlider.getValue());
        return p;
    }

    private void fireOnRoot(javafx.event.Event evt) {
        if (scene != null && scene.getRoot() != null) scene.getRoot().fireEvent(evt);
        else this.fireEvent(evt);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

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
            if (control instanceof Control c) {
                c.setMaxWidth(Double.MAX_VALUE);
            }
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
        spinner.setPrefWidth(SPINNER_PREF_WIDTH);
        spinner.setMaxWidth(SPINNER_PREF_WIDTH);
        spinner.setMinWidth(Region.USE_PREF_SIZE);
    }

    private static void styleColorPicker(ColorPicker cp) {
        cp.setPrefWidth(COLOR_PICKER_PREF_WIDTH);
        cp.setMaxWidth(COLOR_PICKER_PREF_WIDTH);
        cp.setMinWidth(Region.USE_PREF_SIZE);
    }

    private static boolean differs(double a, double b) {
        return Math.abs(a - b) > 1e-9;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
