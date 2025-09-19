package edu.jhuapl.trinity.javafx.controllers;

import edu.jhuapl.trinity.javafx.components.panes.PairwiseJpdfPane;
import edu.jhuapl.trinity.utils.statistics.DensityCache;
import edu.jhuapl.trinity.utils.statistics.JpdfBatchEngine;
import edu.jhuapl.trinity.utils.statistics.PairwiseJpdfConfigPanel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
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
    private PairwiseJpdfPane pane;

    private final JpdfBatchEngine engine;
    private final DensityCache cache;
    private final PairwiseJpdfConfigPanel configPanel;

    public PairwiseJpdfPanePopoutController(Scene scene) {
        this(scene, null, null, null);
    }
    public PairwiseJpdfPanePopoutController(Scene appScene, JpdfBatchEngine engine, DensityCache cache, PairwiseJpdfConfigPanel configPanel) {
        this.appScene = appScene;
        this.engine = engine != null ? engine : new JpdfBatchEngine();
        this.cache = cache != null ? cache : new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
        this.configPanel = configPanel != null ? configPanel : new PairwiseJpdfConfigPanel();
    }

    /** Show the popout window (or focus if already open) */
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
        // IMPORTANT: Create a *fresh* PairwiseJpdfPane for the new window,
        // passing in a new Scene (will be set below), and null for parent.
        scene = new Scene(new BorderPane(), Color.BLACK);
        pane = new PairwiseJpdfPane(scene, null, engine, cache, configPanel);

        // Optional: if you want to sync state from the main window, expose methods to set cohorts/labels, etc.

        // Simple toolbar for popout controls
        ToolBar tb = new ToolBar();
        tb.setPadding(new Insets(5));
        Button btnSecond = new Button("Second Screen");
        btnSecond.setOnAction(e -> sendToSecondScreen());
        btnSecond.setDisable(Screen.getScreens().size() <= 1);
        Button btnFull = new Button("Full Screen");
        btnFull.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        tb.getItems().addAll(btnSecond, btnFull);

        BorderPane root = new BorderPane();
        root.setTop(tb);
        root.setCenter(pane);
        scene.setRoot(root);

        // Style if desired
        // scene.getStylesheets().add(...);

        stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("Trinity — Pairwise Joint Densities");
        stage.setScene(scene);
        stage.setMaximized(true);

        // Set owner for dialogs etc.
        Window owner = appScene.getWindow();
        if (owner instanceof Stage ownerStage) stage.initOwner(ownerStage);

        stage.setOnCloseRequest(e -> {
            saveWindowPrefs();
            pane = null;
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
        var b = screen.getVisualBounds();
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

        if (w > 0 && h > 0) { stage.setWidth(w); stage.setHeight(h); }
        if (!Double.isNaN(x) && !Double.isNaN(y)) { stage.setX(x); stage.setY(y); }
        stage.setFullScreen(fs);
    }
}
