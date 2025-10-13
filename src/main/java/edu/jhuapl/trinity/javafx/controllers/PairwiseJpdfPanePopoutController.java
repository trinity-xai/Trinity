package edu.jhuapl.trinity.javafx.controllers;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.javafx.components.PairwiseJpdfView;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceGridEvent;
import edu.jhuapl.trinity.utils.statistics.DensityCache;
import edu.jhuapl.trinity.utils.statistics.GridDensityResult;
import edu.jhuapl.trinity.utils.statistics.JpdfBatchEngine;
import edu.jhuapl.trinity.utils.statistics.PairwiseJpdfConfigPanel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.prefs.Preferences;

public class PairwiseJpdfPanePopoutController {
    private final Scene appScene;
    private final Preferences prefs = Preferences.userNodeForPackage(PairwiseJpdfPanePopoutController.class);

    private Stage stage;
    private Scene scene;
    private PairwiseJpdfView view;

    private final JpdfBatchEngine engine;
    private final DensityCache cache;
    private final PairwiseJpdfConfigPanel configPanel;

    public PairwiseJpdfPanePopoutController(Scene scene) {
        this(scene, null, null, null);
    }

    public PairwiseJpdfPanePopoutController(
        Scene appScene,
        JpdfBatchEngine engine,
        DensityCache cache,
        PairwiseJpdfConfigPanel configPanel) {
        this.appScene = appScene;
        this.engine = (engine != null) ? engine : new JpdfBatchEngine();
        this.cache = (cache != null) ? cache : new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
        this.configPanel = (configPanel != null) ? configPanel : new PairwiseJpdfConfigPanel();
    }

    /**
     * Show the popout window (or focus if already open)
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

    public void close() {
        if (stage != null) stage.close();
    }

    public boolean isOpen() {
        return stage != null && stage.isShowing();
    }

    public void sendToSecondScreen() {
        if (stage == null) return;
        Screen second = getSecondaryScreen();
        if (second != null) moveToScreen(second);
    }

    private void buildStageAndWire() {
        view = new PairwiseJpdfView(engine, cache, configPanel);

        view.setOnCellClick(item -> {
            if (item == null || item.res == null) return;
            GridDensityResult res = item.res;
            var gridList = res.pdfAsListGrid();
            // Use appScene.getRoot() so the event goes to the main app’s event system
            appScene.getRoot().fireEvent(new HypersurfaceGridEvent(
                HypersurfaceGridEvent.RENDER_PDF,
                gridList,
                res.getxCenters(),
                res.getyCenters(),
                item.xLabel + " | " + item.yLabel + " (PDF)"
            ));
            // Optionally, show a toast:
            view.toast("Opened PDF in 3D.", false);
        });
        // ---- Optional: wire up toast handler if desired ----
        // view.setToastHandler(msg -> { /* show in status bar or dialog */ });
        appScene.addEventHandler(FeatureVectorEvent.NEW_FEATURE_COLLECTION, e -> {
            if (view == null) return; // Defensive, in case view is not yet built
            if (e.object instanceof FeatureCollection fc && fc.getFeatures() != null) {
                view.setCohortA(fc.getFeatures(), "A");
                // Optionally, show a toast to user
                view.toast("Loaded " + fc.getFeatures().size() + " vectors into Cohort A.", false);
            }
        });

        // ---- ToolBar for popout controls ----
        ToolBar tb = new ToolBar();
        tb.setPadding(new Insets(5));
        tb.setBackground(Background.EMPTY);

        Button btnSecond = new Button("Second Screen");
        btnSecond.setOnAction(e -> sendToSecondScreen());
        btnSecond.setDisable(Screen.getScreens().size() <= 1);
        Button btnFull = new Button("Full Screen");
        btnFull.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        tb.getItems().addAll(btnSecond, btnFull);

        BorderPane root = new BorderPane();
        root.setTop(tb);
        root.setCenter(view);
        root.setBackground(Background.EMPTY);
        scene = new Scene(root, 1000, 800, Color.BLACK);

        //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = StyleResourceProvider.getResource("covalent.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = StyleResourceProvider.getResource("dialogstyles.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("Trinity — Pairwise Joint Densities");
        stage.setScene(scene);
        stage.setMaximized(true);

        // Set owner for dialogs etc.
        Window owner = appScene.getWindow();
        if (owner instanceof Stage ownerStage) stage.initOwner(ownerStage);

        stage.setOnCloseRequest(e -> {
            saveWindowPrefs();
            view = null;
            scene.setRoot(new BorderPane()); // help GC
            scene = null;
            stage = null;
        });
    }

    private void placeOnBestScreen() {
        Screen target = pickBestScreen();
        moveToScreen(target);
    }

    private void moveToScreen(Screen screen) {
        javafx.geometry.Rectangle2D b = screen.getVisualBounds();
        stage.setX(b.getMinX() + 40);
        stage.setY(b.getMinY() + 40);
        if (!stage.isShowing()) {
            stage.setWidth(Math.min(1200, b.getWidth() - 80));
            stage.setHeight(Math.min(900, b.getHeight() - 80));
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
        prefs.putDouble("jpdf_pop_x", stage.getX());
        prefs.putDouble("jpdf_pop_y", stage.getY());
        prefs.putDouble("jpdf_pop_w", stage.getWidth());
        prefs.putDouble("jpdf_pop_h", stage.getHeight());
        prefs.putBoolean("jpdf_pop_fs", stage.isFullScreen());
    }

    private void restoreWindowPrefs() {
        double w = prefs.getDouble("jpdf_pop_w", -1);
        double h = prefs.getDouble("jpdf_pop_h", -1);
        double x = prefs.getDouble("jpdf_pop_x", Double.NaN);
        double y = prefs.getDouble("jpdf_pop_y", Double.NaN);
        boolean fs = prefs.getBoolean("jpdf_pop_fs", false);

        if (w > 0 && h > 0) {
            stage.setWidth(w);
            stage.setHeight(h);
        }
        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            stage.setX(x);
            stage.setY(y);
        }
        stage.setFullScreen(fs);
    }

    // ---- Optional: add public getter for the view, to allow parent to set state after popout ----
    public PairwiseJpdfView getView() {
        return view;
    }
}
