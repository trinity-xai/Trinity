package edu.jhuapl.trinity.javafx.controllers;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.javafx.events.HypersurfaceGridEvent;
import edu.jhuapl.trinity.utils.statistics.GridDensityResult;
import edu.jhuapl.trinity.utils.statistics.StatPdfCdfChartPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Popout controller for the Statistics PDF/CDF panel.
 * <p>
 * - Requires an application Scene (owner & event routing) in the constructor.
 * - Accepts and applies a pure-data State snapshot for initialization.
 * - Exposes the current State snapshot for the caller to retrieve and use as needed.
 * - Forwards compute-surface events to the main Scene root.
 */
public final class StatPdfCdfPopoutController {
    public static final double BUTTON_PREF_WIDTH = 200.0;

    private final Scene appScene; // required; used for window ownership and event routing
    private final Preferences prefs = Preferences.userNodeForPackage(StatPdfCdfPopoutController.class);

    // Data-only state management
    private StatPdfCdfChartPanel.State pendingState; // applied on show() if set

    // Popout window members
    private Stage stage;
    private Scene scene;
    private StatPdfCdfChartPanel chart; // created on show()

    public StatPdfCdfPopoutController(Scene appScene) {
        this.appScene = Objects.requireNonNull(appScene, "appScene");
    }

    /**
     * Provide an initial state to apply to the popout chart.
     */
    public void setInitialState(StatPdfCdfChartPanel.State state) {
        if (state == null) return;
        if (isOpen() && chart != null) {
            chart.applyState(state);
        } else {
            this.pendingState = state;
        }
    }

    /**
     * Retrieve the latest state: live export if open, else the last pending/applied state if available.
     */
    public Optional<StatPdfCdfChartPanel.State> getCurrentState() {
        if (isOpen() && chart != null) {
            return Optional.of(chart.exportState());
        }
        return Optional.ofNullable(pendingState);
    }

    /**
     * Open (or focus) the popout window.
     */
    public void show() {
        if (stage != null && stage.isShowing()) {
            stage.toFront();
            stage.requestFocus();
            return;
        }
        buildStageAndWire();
        placeOnBestScreen();
        restoreWindowPrefs();
        stage.show();
    }

    /**
     * Close the window. The latest state remains accessible via getCurrentState().
     */
    public void close() {
        if (stage == null) return;
        saveWindowPrefs();
        if (chart != null) {
            // Cache the last state so the caller can retrieve and apply it externally if desired.
            pendingState = chart.exportState();
        }
        stage.close();
        teardown();
    }

    public boolean isOpen() {
        return stage != null && stage.isShowing();
    }

    /**
     * Move the window to a secondary screen if available.
     */
    public void sendToSecondScreen() {
        if (stage == null) return;
        Screen second = getSecondaryScreen();
        if (second != null) moveToScreen(second);
    }

    // -------------------- internals --------------------

    private void buildStageAndWire() {
        // Build fresh chart instance
        chart = new StatPdfCdfChartPanel();

        // Apply any pending state (if provided)
        if (pendingState != null) {
            chart.applyState(pendingState);
        }

        // Forward 3D compute results to the main Scene root
        chart.setOnComputeSurface(this::forwardSurfaceToMainScene);

        // Lightweight window toolbar
        ToolBar tb = new ToolBar();
        tb.setBackground(Background.EMPTY);

        Button btnSecond = new Button("Second Screen");
        btnSecond.setPrefWidth(BUTTON_PREF_WIDTH);
        btnSecond.setOnAction(e -> sendToSecondScreen());
        btnSecond.setDisable(Screen.getScreens().size() <= 1);

        Button btnFull = new Button("Full Screen");
        btnFull.setPrefWidth(BUTTON_PREF_WIDTH);
        btnFull.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));

        tb.getItems().addAll(btnSecond, btnFull);

        BorderPane root = new BorderPane();
        root.setTop(tb);
        root.setCenter(chart);
        root.setBackground(Background.EMPTY);

        scene = new Scene(root, Color.BLACK);
        applyStyles(scene);

        stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("Trinity — Statistics: PDF / CDF");
        stage.setMaximized(true);
        stage.setScene(scene);

        // Owner: main application window
        Window owner = appScene.getWindow();
        if (owner instanceof Stage ownerStage) {
            stage.initOwner(ownerStage);
        }

        stage.setOnCloseRequest(e -> {
            saveWindowPrefs();
            // Cache the final state so the caller can retrieve it
            pendingState = chart.exportState();
            teardown();
        });
    }

    private void teardown() {
        if (scene != null && scene.getRoot() instanceof BorderPane bp) {
            bp.setCenter(null);
        }
        if (scene != null) {
            scene.setRoot(new VBox()); // help GC
        }
        chart = null;
        scene = null;
        stage = null;
    }

    private void forwardSurfaceToMainScene(GridDensityResult result) {
        if (chart == null || appScene == null) return;

        boolean useCDF = chart.isSurfaceCDF();

        List<List<Double>> grid = useCDF
            ? result.cdfAsListGrid()
            : result.pdfAsListGrid();

        String label = (useCDF ? "CDF" : "PDF") + " : "
            + chart.getScalarType() + " vs "
            + chart.getYFeatureTypeForDisplay();

        appScene.getRoot().fireEvent(
            new HypersurfaceGridEvent(
                useCDF ? HypersurfaceGridEvent.RENDER_CDF : HypersurfaceGridEvent.RENDER_PDF,
                grid,
                result.xCenters(),
                result.yCenters(),
                label
            )
        );
    }

    private static void applyStyles(Scene target) {
        String css = StyleResourceProvider.getResource("styles.css").toExternalForm();
        target.getStylesheets().add(css);
        css = StyleResourceProvider.getResource("covalent.css").toExternalForm();
        target.getStylesheets().add(css);
        css = StyleResourceProvider.getResource("dialogstyles.css").toExternalForm();
        target.getStylesheets().add(css);
    }

    // -------------------- window placement & prefs --------------------

    private void placeOnBestScreen() {
        Screen target = pickBestScreen();
        moveToScreen(target);
    }

    private void moveToScreen(Screen s) {
        if (stage == null) return;
        var b = s.getVisualBounds();
        stage.setX(b.getMinX() + 40.0);
        stage.setY(b.getMinY() + 40.0);
        if (!stage.isShowing()) {
            stage.setWidth(Math.min(1200.0, b.getWidth() - 80.0));
            stage.setHeight(Math.min(900.0, b.getHeight() - 80.0));
        }
    }

    private static Screen pickBestScreen() {
        var all = Screen.getScreens();
        if (all.size() <= 1) return Screen.getPrimary();
        for (Screen s : all) if (!s.equals(Screen.getPrimary())) return s;
        return Screen.getPrimary();
    }

    private static Screen getSecondaryScreen() {
        for (Screen s : Screen.getScreens()) if (!s.equals(Screen.getPrimary())) return s;
        return null;
    }

    private void saveWindowPrefs() {
        if (stage == null) return;
        prefs.putDouble("statpdfcdf_pop_x", stage.getX());
        prefs.putDouble("statpdfcdf_pop_y", stage.getY());
        prefs.putDouble("statpdfcdf_pop_w", stage.getWidth());
        prefs.putDouble("statpdfcdf_pop_h", stage.getHeight());
        prefs.putBoolean("statpdfcdf_pop_fs", stage.isFullScreen());
    }

    private void restoreWindowPrefs() {
        double w = prefs.getDouble("statpdfcdf_pop_w", -1.0);
        double h = prefs.getDouble("statpdfcdf_pop_h", -1.0);
        double x = prefs.getDouble("statpdfcdf_pop_x", Double.NaN);
        double y = prefs.getDouble("statpdfcdf_pop_y", Double.NaN);
        boolean fs = prefs.getBoolean("statpdfcdf_pop_fs", false);

        if (w > 0.0 && h > 0.0) {
            stage.setWidth(w);
            stage.setHeight(h);
        }
        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            stage.setX(x);
            stage.setY(y);
        }
        stage.setFullScreen(fs);
    }
}
