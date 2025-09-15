package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.FeatureVectorManagerView;
import edu.jhuapl.trinity.javafx.services.FeatureVectorManagerService;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Pane;

public class FeatureVectorManagerPane extends LitPathPane {

    private final FeatureVectorManagerView view;
    private final FeatureVectorManagerService service;

    public FeatureVectorManagerPane(Scene scene, Pane parent, FeatureVectorManagerService service) {
        super(scene, parent, 900, 640, new FeatureVectorManagerView(),
            "Feature Vectors", "Manager", 300.0, 400.0);

        this.view = (FeatureVectorManagerView) this.contentPane;
        this.service = service;

        wireViewToService();
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
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null || item.trim().isEmpty() ? "(unnamed)" : item));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
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

    // Convenience for tests / external triggers
    public void applyActiveToWorkspace() {
        service.applyActiveToWorkspace();
    }
}
