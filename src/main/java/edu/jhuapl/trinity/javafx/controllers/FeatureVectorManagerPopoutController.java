package edu.jhuapl.trinity.javafx.controllers;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.FeatureVectorManagerView;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.services.FeatureVectorManagerService;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public final class FeatureVectorManagerPopoutController {
    public static double BUTTON_PREF_WIDTH = 200;
    private final FeatureVectorManagerService service;
    private final Scene appScene; // main/original app Scene for event dispatch
    private final Preferences prefs = Preferences.userNodeForPackage(FeatureVectorManagerPopoutController.class);

    private Stage stage;
    private Scene scene;
    private FeatureVectorManagerView view;
    private PauseTransition searchDebounce;

    public FeatureVectorManagerPopoutController(FeatureVectorManagerService service, Scene appScene) {
        this.service = Objects.requireNonNull(service, "service");
        this.appScene = Objects.requireNonNull(appScene, "appScene");
    }

    /**
     * Open (or focus) the pop-out window.
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
     * Close the pop-out window (no service teardown).
     */
    public void close() {
        if (stage != null) stage.close();
    }

    public boolean isOpen() {
        return stage != null && stage.isShowing();
    }

    /**
     * Move to a secondary screen if available.
     */
    public void sendToSecondScreen() {
        if (stage == null) return;
        Screen second = getSecondaryScreen();
        if (second != null) moveToScreen(second);
    }

    // -------------------- internals --------------------

    private void buildStageAndWire() {
        view = new FeatureVectorManagerView();
        view.setDetailLevel(FeatureVectorManagerView.DetailLevel.FULL);

        // simple window toolbar (outside the View)
        ToolBar tb = new ToolBar();
        tb.setBackground(Background.EMPTY);
        Button btnSecond = new Button("Second Screen");
        btnSecond.setPrefWidth(BUTTON_PREF_WIDTH);
        btnSecond.setOnAction(e -> sendToSecondScreen());
        btnSecond.setDisable(Screen.getScreens().size() <= 1);
        Button btnFull = new Button("Full Screen");
        btnFull.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        btnFull.setPrefWidth(BUTTON_PREF_WIDTH);
        tb.getItems().addAll(btnSecond, btnFull);

        BorderPane root = new BorderPane();
        root.setTop(tb);
        root.setCenter(view);
        root.setBackground(Background.EMPTY);

        scene = new Scene(root, Color.BLACK);

        //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = StyleResourceProvider.getResource("covalent.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = StyleResourceProvider.getResource("dialogstyles.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("Trinity — Feature Vectors");
        stage.setMaximized(true);
        stage.setScene(scene);

        // make dialogs owned by this window; keep main Scene for event routing
        Window owner = appScene.getWindow();
        if (owner instanceof Stage ownerStage) {
            stage.initOwner(ownerStage);
        }

        // wire view ↔ service exactly like FeatureVectorManagerPane
        wireViewToService();
        installSearchWiring();
        installCollectionContextMenu();
        installTableContextMenu();

        stage.setOnCloseRequest(e -> {
            saveWindowPrefs();
            // release references that hold UI (service is shared and remains alive)
            view = null;
            scene.setRoot(new VBox()); // help GC
            scene = null;
            stage = null;
        });
    }

    private void wireViewToService() {
        // live items list
        view.getTable().setItems(service.getDisplayedVectors());
        view.getTable().itemsProperty().addListener((obs, o, n) ->
            view.setStatus("Showing " + (n == null ? 0 : n.size()) + " vectors.")
        );
        view.getTable().itemsProperty().bind(Bindings.createObjectBinding(
            service::getDisplayedVectors, service.getDisplayedVectors()));

        // collections list + two-way sync
        var combo = view.getCollectionSelector();
        combo.setItems(service.getCollectionNames());
        combo.setVisibleRowCount(15);

        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null || item.trim().isEmpty() ? "(unnamed)" : item));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null || item.trim().isEmpty() ? "(unnamed)" : item));
            }
        });

        view.selectedCollectionProperty().addListener((obs, o, n) -> {
            if (n != null && !n.equals(service.activeCollectionNameProperty().get())) {
                service.activeCollectionNameProperty().set(n);
            }
        });
        service.activeCollectionNameProperty().addListener((obs, o, n) -> {
            if (n != null && null != view && !n.equals(view.getSelectedCollection())) {
                combo.getSelectionModel().select(n);
            }
        });
        service.getCollectionNames().addListener((ListChangeListener<String>) c -> {
            if (combo.getSelectionModel().isEmpty() && !service.getCollectionNames().isEmpty()) {
                combo.getSelectionModel().select(service.getCollectionNames().get(0));
            }
        });

        // sampling mapping
        view.samplingModeProperty().addListener((obs, o, s) -> {
            if (s == null) return;
            FeatureVectorManagerService.SamplingMode mode = switch (s) {
                case "Head (1000)" -> FeatureVectorManagerService.SamplingMode.HEAD_1000;
                case "Tail (1000)" -> FeatureVectorManagerService.SamplingMode.TAIL_1000;
                case "Random (1000)" -> FeatureVectorManagerService.SamplingMode.RANDOM_1000;
                default -> FeatureVectorManagerService.SamplingMode.ALL;
            };
            if (service.samplingModeProperty().get() != mode) {
                service.samplingModeProperty().set(mode);
            }
        });
    }

    /**
     * Debounced search → service.setTextFilter; Enter applies; Esc clears.
     */
    private void installSearchWiring() {
        TextField tf = view.getSearchField();

        searchDebounce = new PauseTransition(javafx.util.Duration.millis(200));
        searchDebounce.setOnFinished(e -> service.setTextFilter(tf.getText()));

        tf.textProperty().addListener((obs, o, n) -> searchDebounce.playFromStart());

        tf.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                searchDebounce.stop();
                service.setTextFilter(tf.getText());
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                tf.clear();
                searchDebounce.stop();
                service.setTextFilter("");
                e.consume();
            }
        });

        service.textFilterProperty().addListener((obs, o, n) -> {
            boolean active = n != null && !n.isBlank();
            view.setStatus(active
                ? "Showing filtered vectors."
                : "Showing " + service.getDisplayedVectors().size() + " vectors.");
        });
    }

    // ---- collection context menu (rename, duplicate, delete, merge, export, apply) ----

    private void installCollectionContextMenu() {
        ComboBox<String> combo = view.getCollectionSelector();
        ContextMenu ctx = new ContextMenu();

        MenuItem miRename = new MenuItem("Rename…");
        miRename.setOnAction(e -> {
            String current = service.activeCollectionNameProperty().get();
            if (current == null) return;
            TextInputDialog dlg = new TextInputDialog(current);
            dlg.setHeaderText("Rename Collection");
            dlg.setContentText("New name:");
            dlg.getDialogPane().setPadding(new Insets(10));
            dlg.initOwner(stage);
            dlg.showAndWait().ifPresent(newName -> {
                if (newName != null && !newName.trim().isEmpty()) {
                    service.renameCollection(current, newName.trim());
                }
            });
        });

        MenuItem miDuplicate = new MenuItem("Duplicate…");
        miDuplicate.setOnAction(e -> {
            String current = service.activeCollectionNameProperty().get();
            if (current == null) return;
            TextInputDialog dlg = new TextInputDialog("Copy of " + current);
            dlg.setHeaderText("Duplicate Collection");
            dlg.setContentText("New name:");
            dlg.getDialogPane().setPadding(new Insets(10));
            dlg.initOwner(stage);
            dlg.showAndWait().ifPresent(proposed -> service.duplicateCollection(current, proposed));
        });

        MenuItem miDelete = new MenuItem("Delete…");
        miDelete.setOnAction(e -> {
            String current = service.activeCollectionNameProperty().get();
            if (current == null) return;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete collection \"" + current + "\"?\nThis cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL);
            alert.setHeaderText("Delete Collection");
            alert.initOwner(stage);
            alert.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) service.deleteCollection(current);
            });
        });

        MenuItem miMergeInto = new MenuItem("Merge into…");
        miMergeInto.setOnAction(e -> {
            String current = service.activeCollectionNameProperty().get();
            if (current == null) return;
            List<String> options = service.getCollectionNames().stream()
                .filter(n -> !Objects.equals(n, current))
                .collect(Collectors.toList());
            if (options.isEmpty()) {
                info("No other collections to merge into.");
                return;
            }
            ChoiceDialog<String> chooser = new ChoiceDialog<>(options.get(0), options);
            chooser.setHeaderText("Merge \"" + current + "\" into…");
            chooser.setContentText("Target collection:");
            chooser.getDialogPane().setPadding(new Insets(10));
            chooser.initOwner(stage);
            chooser.showAndWait().ifPresent(target -> {
                Alert dedup = new Alert(Alert.AlertType.CONFIRMATION,
                    "De-duplicate by entityId while merging?",
                    ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                dedup.setHeaderText("Merge Options");
                dedup.initOwner(stage);
                dedup.showAndWait().ifPresent(resp -> {
                    if (resp == ButtonType.CANCEL) return;
                    boolean dd = (resp == ButtonType.YES);
                    service.mergeInto(target, current, dd);
                });
            });
        });

        Menu export = new Menu("Export");
        MenuItem miExportJson = new MenuItem("As JSON (FeatureCollection)");
        miExportJson.setOnAction(e -> exportCollection(FeatureVectorManagerService.ExportFormat.JSON));
        MenuItem miExportCsv = new MenuItem("As CSV");
        miExportCsv.setOnAction(e -> exportCollection(FeatureVectorManagerService.ExportFormat.CSV));
        export.getItems().addAll(miExportJson, miExportCsv);

        Menu applyMenu = new Menu("Apply to workspace");
        MenuItem miApplyAppend = new MenuItem("Apply (append)");
        miApplyAppend.setOnAction(e -> service.applyActiveToWorkspace(false));
        MenuItem miApplyReplace = new MenuItem("Apply (replace)");
        miApplyReplace.setOnAction(e -> service.applyActiveToWorkspace(true));

        MenuItem miSetAllAppend = new MenuItem("Set All (append)");
        miSetAllAppend.setOnAction(e -> service.applyAllToWorkspace(false));
        MenuItem miSetAllReplace = new MenuItem("Set All (replace)");
        miSetAllReplace.setOnAction(e -> service.applyAllToWorkspace(true));
        applyMenu.getItems().addAll(miApplyAppend, miApplyReplace, miSetAllAppend, miSetAllReplace);

        ctx.getItems().addAll(miRename, miDuplicate, miDelete, new SeparatorMenuItem(),
            miMergeInto, export, new SeparatorMenuItem(), applyMenu);

        combo.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, e -> {
            if (!ctx.isShowing()) ctx.show(combo, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    private void exportCollection(FeatureVectorManagerService.ExportFormat fmt) {
        String current = service.activeCollectionNameProperty().get();
        if (current == null) return;
        FileChooser fc = new FileChooser();
        fc.setInitialFileName(safeFilename(current) + (fmt == FeatureVectorManagerService.ExportFormat.JSON ? ".json" : ".csv"));
        fc.getExtensionFilters().setAll(
            new FileChooser.ExtensionFilter("JSON", "*.json"),
            new FileChooser.ExtensionFilter("CSV", "*.csv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try {
                service.exportCollection(current, file, fmt);
                info("Exported \"" + current + "\" to:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                error("Export failed:\n" + ex.getMessage());
            }
        }
    }

    // ---- table context menu (remove, copy, bulk edit, locate, apply) ----

    private void installTableContextMenu() {
        TableView<FeatureVector> table = view.getTable();
        ContextMenu ctx = new ContextMenu();

        MenuItem miRemoveSel = new MenuItem("Remove Selected");
        miRemoveSel.setOnAction(e -> {
            List<FeatureVector> sel = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (!sel.isEmpty()) service.removeFromActive(sel);
        });

        MenuItem miCopyTo = new MenuItem("Copy Selected to…");
        miCopyTo.setOnAction(e -> {
            List<FeatureVector> sel = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (sel.isEmpty()) return;

            String current = service.activeCollectionNameProperty().get();
            TextInputDialog dlg = new TextInputDialog(current == null ? "NewCollection" : (current + "-copy"));
            dlg.setHeaderText("Copy Selected to Collection");
            dlg.setContentText("Target name (existing or new):");
            dlg.getDialogPane().setPadding(new Insets(10));
            dlg.initOwner(stage);
            dlg.showAndWait().ifPresent(target -> {
                if (target != null && !target.trim().isEmpty()) {
                    service.copyToCollection(sel, target.trim());
                }
            });
        });

        MenuItem miEditLabel = new MenuItem("Edit Label(s)…");
        miEditLabel.setOnAction(e -> {
            List<FeatureVector> sel = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (sel.isEmpty()) return;
            TextInputDialog dlg = new TextInputDialog();
            dlg.setHeaderText("Bulk Set Label");
            dlg.setContentText("New label:");
            dlg.getDialogPane().setPadding(new Insets(10));
            dlg.initOwner(stage);
            dlg.showAndWait().ifPresent(lbl -> {
                if (lbl != null) service.bulkSetLabelInActive(sel, lbl);
            });
        });

        MenuItem miEditMeta = new MenuItem("Edit Metadata…");
        miEditMeta.setOnAction(e -> {
            List<FeatureVector> sel = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (sel.isEmpty()) return;

            Dialog<Map<String, String>> dlg = new Dialog<>();
            dlg.setTitle("Bulk Edit Metadata");
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            TextArea ta = new TextArea();
            ta.setPromptText("Enter key=value pairs, one per line.\nExample:\nsource=fileA\nuuid=override-123");
            ta.setPrefRowCount(10);
            ta.setWrapText(true);
            dlg.getDialogPane().setContent(ta);
            dlg.getDialogPane().setPadding(new Insets(10));
            dlg.initOwner(stage);

            dlg.setResultConverter(btn -> btn == ButtonType.OK ? parseKeyValues(ta.getText()) : null);
            dlg.showAndWait().ifPresent(kv -> {
                if (!kv.isEmpty()) service.bulkEditMetadataInActive(sel, kv);
            });
        });

        MenuItem miLocate = new MenuItem("Locate in 3D");
        miLocate.setOnAction(e -> {
            var sel = table.getSelectionModel().getSelectedItems();
            if (sel == null || sel.isEmpty()) return;
            sel.forEach(fv -> appScene.getRoot().fireEvent(
                new FeatureVectorEvent(FeatureVectorEvent.LOCATE_FEATURE_VECTOR, fv)
            ));
        });

        Menu applyMenu = new Menu("Apply to workspace");
        MenuItem miApplyAppend = new MenuItem("Apply (append)");
        miApplyAppend.setOnAction(e -> applySelectionOrActive(false));
        MenuItem miApplyReplace = new MenuItem("Apply (replace)");
        miApplyReplace.setOnAction(e -> applySelectionOrActive(true));
        applyMenu.getItems().addAll(miApplyAppend, miApplyReplace);

        ctx.getItems().addAll(miRemoveSel, miCopyTo, new SeparatorMenuItem(),
            miEditLabel, miEditMeta, new SeparatorMenuItem(),
            miLocate, applyMenu);

        table.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, e -> {
            if (!ctx.isShowing()) ctx.show(table, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    /**
     * Selection-aware apply: if selection present, fire NEW_FEATURE_COLLECTION to main scene; else use service.
     */
    private void applySelectionOrActive(boolean replace) {
        TableView<FeatureVector> table = view.getTable();
        List<FeatureVector> sel = new ArrayList<>(table.getSelectionModel().getSelectedItems());

        if (!sel.isEmpty()) {
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(sel);
            FeatureVectorEvent evt =
                new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc,
                    FeatureVectorManagerService.MANAGER_APPLY_TAG);
            evt.clearExisting = replace;
            appScene.getRoot().fireEvent(evt);
        } else {
            service.applyActiveToWorkspace(replace);
        }
    }

    // -------------------- window placement & prefs --------------------

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
        prefs.putDouble("fv_pop_x", stage.getX());
        prefs.putDouble("fv_pop_y", stage.getY());
        prefs.putDouble("fv_pop_w", stage.getWidth());
        prefs.putDouble("fv_pop_h", stage.getHeight());
        prefs.putBoolean("fv_pop_fs", stage.isFullScreen());
    }

    private void restoreWindowPrefs() {
        double w = prefs.getDouble("fv_pop_w", -1);
        double h = prefs.getDouble("fv_pop_h", -1);
        double x = prefs.getDouble("fv_pop_x", Double.NaN);
        double y = prefs.getDouble("fv_pop_y", Double.NaN);
        boolean fs = prefs.getBoolean("fv_pop_fs", false);

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

    // -------------------- small utils --------------------

    private static String safeFilename(String s) {
        if (s == null) return "collection";
        return s.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static Map<String, String> parseKeyValues(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return map;
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String ln = line.trim();
            if (ln.isEmpty()) continue;
            int eq = ln.indexOf('=');
            if (eq < 0) {
                map.put(ln, "");
            } else {
                String k = ln.substring(0, eq).trim();
                String v = ln.substring(eq + 1).trim();
                if (!k.isEmpty()) map.put(k, v);
            }
        }
        return map;
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.initOwner(stage);
        a.showAndWait();
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error");
        a.initOwner(stage);
        a.showAndWait();
    }
}
