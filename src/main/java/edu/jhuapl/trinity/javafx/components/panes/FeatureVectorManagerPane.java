package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.FeatureVectorManagerView;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.services.FeatureVectorManagerService;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeatureVectorManagerPane extends LitPathPane {

    private final FeatureVectorManagerView view;
    private final FeatureVectorManagerService service;

    // simple debounce for search typing
    private javafx.animation.PauseTransition searchDebounce;

    public FeatureVectorManagerPane(Scene scene, Pane parent, FeatureVectorManagerService service) {
        super(scene, parent, 900, 640, new FeatureVectorManagerView(),
            "Feature Vectors", "Manager", 300.0, 400.0);

        this.view = (FeatureVectorManagerView) this.contentPane;
        this.service = service;

        wireViewToService();
        installCollectionContextMenu();
        installTableContextMenu();
        installSearchWiring();
    }

    @Override
    public void maximize() {
        this.scene.getRoot().fireEvent(new ApplicationEvent(
            ApplicationEvent.POPOUT_FEATUREVECTOR_MANAGER, Boolean.TRUE));
    }

    private void wireViewToService() {
        // Live-bind the table to service's displayed vectors
        view.getTable().setItems(service.getDisplayedVectors());

        // Live-bind the ComboBox to service's collection names
        var combo = view.getCollectionSelector();
        combo.setItems(service.getCollectionNames());
        combo.setVisibleRowCount(15);

        // Render "(unnamed)" for empty names, both in popup and button cell
        combo.setCellFactory(listView -> new ListCell<>() {
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

        // Two-way selection sync between view and service
        view.selectedCollectionProperty().addListener((obs, o, n) -> {
            if (n != null && !n.equals(service.activeCollectionNameProperty().get())) {
                service.activeCollectionNameProperty().set(n);
            }
        });
        service.activeCollectionNameProperty().addListener((obs, o, n) -> {
            if (n != null && !n.equals(view.getSelectedCollection())) {
                combo.getSelectionModel().select(n);
            }
        });

        // If names list changes and no selection, select the first
        service.getCollectionNames().addListener((ListChangeListener<String>) c -> {
            if (combo.getSelectionModel().isEmpty() && !service.getCollectionNames().isEmpty()) {
                combo.getSelectionModel().select(service.getCollectionNames().get(0));
            }
        });

        // Map sampling text to enum
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

        // Keep bottom status roughly in sync with rows shown
        view.getTable().itemsProperty().addListener((obs, o, n) -> {
            view.setStatus("Showing " + (n == null ? 0 : n.size()) + " vectors.");
        });
        view.getTable().itemsProperty().bind(Bindings.createObjectBinding(
            () -> service.getDisplayedVectors(), service.getDisplayedVectors()));
    }

    /**
     * Wire the header Search TextField to the service text filter (with debounce).
     */
    private void installSearchWiring() {
        TextField tf = view.getSearchField();

        // init debounce timer (~200ms after last keypress)
        searchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
        searchDebounce.setOnFinished(e -> service.setTextFilter(tf.getText()));

        // typing -> debounce -> set filter
        tf.textProperty().addListener((obs, o, n) -> searchDebounce.playFromStart());

        // ENTER applies immediately, ESC clears
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

        // Also update placeholder status text if you want visual feedback (optional)
        service.textFilterProperty().addListener((obs, o, n) -> {
            boolean active = n != null && !n.isBlank();
            view.setStatus(active ? "Showing filtered vectors." : "Showing " + service.getDisplayedVectors().size() + " vectors.");
        });
    }

    // ---------------- Context Menu: Collections ----------------

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
            dlg.getEditor().setText(current);
            dlg.getDialogPane().setPadding(new Insets(10));
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
            chooser.showAndWait().ifPresent(target -> {
                Alert dedup = new Alert(Alert.AlertType.CONFIRMATION,
                    "De-duplicate by entityId while merging?",
                    ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                dedup.setHeaderText("Merge Options");
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

        // Apply submenu (active collection)
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
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                service.exportCollection(current, file, fmt);
                info("Exported \"" + current + "\" to:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                error("Export failed:\n" + ex.getMessage());
            }
        }
    }

    // ---------------- Context Menu: Vectors (table) ----------------

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

            dlg.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    return parseKeyValues(ta.getText());
                }
                return null;
            });

            Optional<Map<String, String>> res = dlg.showAndWait();
            res.ifPresent(kv -> {
                if (!kv.isEmpty()) service.bulkEditMetadataInActive(sel, kv);
            });
        });

        MenuItem miLocate = new MenuItem("Locate in 3D");
        miLocate.setOnAction(e -> {
            var sel = table.getSelectionModel().getSelectedItems();
            if (sel == null || sel.isEmpty()) return;
            sel.forEach(fv ->
                getScene().getRoot().fireEvent(new FeatureVectorEvent(FeatureVectorEvent.LOCATE_FEATURE_VECTOR, fv))
            );
        });

        // Apply submenu — uses selection if present, else whole active collection
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
     * If there is a non-empty selection in the table, apply only those vectors to the workspace.
     * Otherwise, fall back to applying the whole active collection via the service.
     *
     * @param replace true = replace in workspace, false = append
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

            getScene().getRoot().fireEvent(evt);
        } else {
            service.applyActiveToWorkspace(replace);
        }
    }

    // ---------------- Helpers ----------------

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
        a.showAndWait();
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error");
        a.showAndWait();
    }

    // Convenience for tests / external triggers
    public void applyActiveToWorkspace() {
        service.applyActiveToWorkspace(false);
    }
}
