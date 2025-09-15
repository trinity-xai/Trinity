package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * FeatureVectorManagerView
 * - Header with Collection selector + Sampling choice
 * - Center TableView of FeatureVectors (compact/full columns)
 * - Bottom stack: Details (values preview) and Metadata (formatted key/value), independent TitledPanes
 * - Status bar with progress indicator
 *
 * Note: The collection ComboBox should be bound to a live ObservableList by the container/pane:
 *   view.getCollectionSelector().setItems(service.getCollectionNames());
 */
public class FeatureVectorManagerView extends BorderPane {

    public enum DetailLevel { COMPACT, FULL }

    private final StringProperty samplingMode = new SimpleStringProperty("All");
    private final StringProperty selectedCollection = new SimpleStringProperty("");
    private final ObjectProperty<DetailLevel> detailLevel = new SimpleObjectProperty<>(DetailLevel.COMPACT);

    // Header controls
    private final ComboBox<String> collectionSelector = new ComboBox<>();
    private final ChoiceBox<String> samplingChoice = new ChoiceBox<>();

    // Table + columns
    private final TableView<FeatureVector> table = new TableView<>();
    private final TableColumn<FeatureVector, String> colIndex = new TableColumn<>("#");
    private final TableColumn<FeatureVector, String> colLabel = new TableColumn<>("Label");
    private final TableColumn<FeatureVector, String> colDim = new TableColumn<>("Dim");
    private final TableColumn<FeatureVector, String> colPreview = new TableColumn<>("Preview");
    private final TableColumn<FeatureVector, String> colScore = new TableColumn<>("Score");
    private final TableColumn<FeatureVector, String> colPfa = new TableColumn<>("PFA");
    private final TableColumn<FeatureVector, String> colLayer = new TableColumn<>("Layer");

    private final ObservableList<FeatureVector> items = FXCollections.observableArrayList();

    // Details + Metadata (independent panes)
    private final TitledPane detailsSection = new TitledPane();
    private final TitledPane metadataSection = new TitledPane();
    private final TextArea valuesPreviewArea = new TextArea();
    private final TextArea metaTextArea = new TextArea();

    // Status
    private final Label statusLabel = new Label("Ready.");
    private final ProgressBar progressBar = new ProgressBar();

    public FeatureVectorManagerView() {
        buildHeader();
        buildTable();
        buildDetailAndMetadataPanes();

        BorderPane.setMargin(table, Insets.EMPTY);
        setTop(buildHeaderBar());
        setCenter(table);

        VBox bottomStack = new VBox(4, buildBottomBlock(), buildStatusBar());
        bottomStack.setPadding(new Insets(2, 2, 2, 2));
        setBottom(bottomStack);

        // Wiring (internal)
        samplingChoice.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> {
            if (newV != null) samplingMode.set(newV);
        });
        collectionSelector.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> {
            if (newV != null) selectedCollection.set(newV);
        });
        detailLevel.addListener((o, oldV, newV) -> applyDetailLevel(newV));

        detailsSection.setExpanded(false);
        metadataSection.setExpanded(false);
        progressBar.setVisible(false);
        applyDetailLevel(detailLevel.get());
    }

    // Header -------------------------------------------------

    private HBox buildHeaderBar() {
        Label lblCollection = new Label("Collection:");
        Label lblSampling   = new Label("Sampling:");

        HBox header = new HBox(8, lblCollection, collectionSelector, lblSampling, samplingChoice);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 4, 4, 4));
        header.setBackground(new Background(new BackgroundFill(
            Color.color(0, 0, 0, 0.08), CornerRadii.EMPTY, Insets.EMPTY
        )));
        header.setBorder(new Border(new BorderStroke(
            Color.color(1, 1, 1, 0.18), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT
        )));

        // Let inputs expand/shrink with space
        collectionSelector.setMaxWidth(Double.MAX_VALUE);
        samplingChoice.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(collectionSelector, Priority.ALWAYS);
        HBox.setHgrow(samplingChoice,    Priority.ALWAYS);

        return header;
    }

    private void buildHeader() {
        collectionSelector.setPromptText("No collections loaded");
        collectionSelector.setPrefWidth(250);
        collectionSelector.setMinWidth(100);
        collectionSelector.setMaxWidth(Double.MAX_VALUE);

        samplingChoice.getItems().addAll("All", "Head (1000)", "Tail (1000)", "Random (1000)");
        samplingChoice.getSelectionModel().selectFirst();
        samplingChoice.setPrefWidth(250);
        samplingChoice.setMinWidth(100);
        samplingChoice.setMaxWidth(Double.MAX_VALUE);
    }

    // Table --------------------------------------------------

    private void buildTable() {
        table.setItems(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setPlaceholder(new Label("No vectors loaded."));

        colIndex.setMinWidth(50);
        colIndex.setMaxWidth(70);
        colIndex.setCellValueFactory(cd ->
            javafx.beans.binding.Bindings.createStringBinding(
                () -> String.valueOf(table.getItems().indexOf(cd.getValue()) + 1), table.itemsProperty()
            )
        );

        colLabel.setMinWidth(120);
        colLabel.setCellValueFactory(cd -> new ReadOnlyStringWrapper(opt(cd.getValue().getLabel())));

        colDim.setMinWidth(70);
        colDim.setMaxWidth(90);
        colDim.setCellValueFactory(cd -> {
            var data = cd.getValue().getData();
            int dim = (data == null) ? 0 : data.size();
            return new ReadOnlyStringWrapper(String.valueOf(dim));
        });

        colPreview.setCellValueFactory(cd -> new ReadOnlyStringWrapper(previewList(cd.getValue().getData(), 8)));

        colScore.setMinWidth(80);
        colScore.setCellValueFactory(cd -> new ReadOnlyStringWrapper(trim(cd.getValue().getScore())));
        colPfa.setMinWidth(70);
        colPfa.setCellValueFactory(cd -> new ReadOnlyStringWrapper(trim(cd.getValue().getPfa())));
        colLayer.setMinWidth(70);
        colLayer.setCellValueFactory(cd -> new ReadOnlyStringWrapper(String.valueOf(cd.getValue().getLayer())));

        table.getColumns().setAll(colIndex, colLabel, colDim, colPreview);

        table.getSelectionModel()
             .getSelectedItems()
             .addListener((ListChangeListener<FeatureVector>) change -> updateDetailsPreview());
    }

    // Details & Metadata panes -------------------------------

    private void buildDetailAndMetadataPanes() {
        // Details (values preview)
        valuesPreviewArea.setEditable(false);
        valuesPreviewArea.setPrefRowCount(8);
        valuesPreviewArea.setWrapText(true);

        VBox detailsBox = new VBox(6, valuesPreviewArea);
        detailsBox.setPadding(new Insets(6));

        detailsSection.setText("Details");
        detailsSection.setContent(detailsBox);
        detailsSection.setExpanded(false);

        // Metadata (formatted key/value pairs)
        metaTextArea.setEditable(false);
        metaTextArea.setPrefRowCount(8);
        metaTextArea.setWrapText(true);

        VBox metaBox = new VBox(6, metaTextArea);
        metaBox.setPadding(new Insets(6));

        metadataSection.setText("Metadata");
        metadataSection.setContent(metaBox);
        metadataSection.setExpanded(false);
    }

    private Node buildBottomBlock() {
        // Independent (non-Accordion) so both panes can be open at once
        VBox stack = new VBox(6, detailsSection, metadataSection);
        stack.setPadding(new Insets(6));

        BorderPane wrapper = new BorderPane(stack);
        wrapper.setBorder(new Border(new BorderStroke(
            Color.color(1, 1, 1, 0.15), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT
        )));
        wrapper.setBackground(new Background(new BackgroundFill(
            Color.color(0, 0, 0, 0.04), CornerRadii.EMPTY, Insets.EMPTY
        )));
        return wrapper;
    }

    // Status -------------------------------------------------

    private HBox buildStatusBar() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        progressBar.setPrefWidth(140);
        progressBar.setVisible(false);

        HBox status = new HBox(8, statusLabel, spacer, progressBar);
        status.setPadding(new Insets(4, 6, 4, 6));
        status.setAlignment(Pos.CENTER_LEFT);
        status.setBackground(new Background(new BackgroundFill(
            Color.color(0, 0, 0, 0.06), CornerRadii.EMPTY, Insets.EMPTY
        )));
        status.setBorder(new Border(new BorderStroke(
            Color.color(1, 1, 1, 0.15), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT
        )));
        return status;
    }

    // Helpers -----------------------------------------------

    private static String opt(String s) { return s == null ? "" : s; }

    private static String trim(double d) {
        String s = String.format(Locale.ROOT, "%.6f", d);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    private static String previewList(List<Double> data, int firstN) {
        if (data == null || data.isEmpty()) return "[]";
        int n = Math.min(firstN, data.size());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            Double v = data.get(i);
            if (v == null) sb.append("NaN");
            else {
                String s = String.format(Locale.ROOT, "%.6f", v);
                if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
                sb.append(s);
            }
        }
        if (data.size() > n) sb.append(", …");
        sb.append("]");
        return sb.toString();
    }

    private void updateDetailsPreview() {
        var sel = table.getSelectionModel().getSelectedItems();
        if (sel == null || sel.isEmpty()) {
            valuesPreviewArea.clear();
            metaTextArea.setText("(no metadata)");
            // do not auto-collapse; let user control the panes independently
            return;
        }
        FeatureVector fv = sel.get(0);
        var data = fv.getData();

        // Details text
        StringBuilder sb = new StringBuilder();
        sb.append("Label: ").append(opt(fv.getLabel())).append("\n");
        sb.append("Dim: ").append(data == null ? 0 : data.size()).append("\n");
        sb.append("Score: ").append(trim(fv.getScore())).append("   PFA: ").append(trim(fv.getPfa())).append("\n");
        sb.append("Layer: ").append(fv.getLayer()).append("   FrameId: ").append(fv.getFrameId()).append("\n");
        sb.append("BBox: ").append(fv.getBbox() == null ? "[]" : fv.getBbox().toString()).append("\n\n");

        sb.append("Values (preview up to 64):\n");
        if (data != null) {
            int n = Math.min(64, data.size());
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(", ");
                Double v = data.get(i);
                if (v == null) sb.append("NaN");
                else {
                    String s = String.format(Locale.ROOT, "%.6f", v);
                    if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
                    sb.append(s);
                }
            }
            if (data.size() > n) sb.append(", …");
        }
        valuesPreviewArea.setText(sb.toString());

        // Metadata text (key/value per line)
        if (fv.getMetaData() == null || fv.getMetaData().isEmpty()) {
            metaTextArea.setText("(no metadata)");
        } else {
            StringBuilder sbMeta = new StringBuilder();
            fv.getMetaData().forEach((k, v) -> {
                sbMeta.append(k == null ? "(null)" : k)
                      .append(": ")
                      .append(v == null ? "(null)" : v)
                      .append("\n");
            });
            metaTextArea.setText(sbMeta.toString().trim());
        }
    }

    private void applyDetailLevel(DetailLevel level) {
        if (level == DetailLevel.COMPACT) {
            table.getColumns().setAll(colIndex, colLabel, colDim, colPreview);
        } else {
            table.getColumns().setAll(colIndex, colLabel, colDim, colPreview, colScore, colPfa, colLayer);
        }
    }

    // Public API --------------------------------------------

    public void setVectors(List<FeatureVector> vectors) {
        items.setAll(vectors == null ? Collections.emptyList() : vectors);
        setStatus("Loaded " + items.size() + " vectors.");
    }
    public void addVectors(Collection<FeatureVector> vectors) {
        if (vectors != null) items.addAll(vectors);
        setStatus("Loaded " + items.size() + " vectors.");
    }
    /** Snapshot setter (optional). If you’re binding live in the pane, you can ignore this. */
    public void setCollections(List<String> names) {
        collectionSelector.getItems().setAll(names == null ? Collections.emptyList() : names);
        if (!collectionSelector.getItems().isEmpty()) collectionSelector.getSelectionModel().selectFirst();
    }
    public void setStatus(String message) { statusLabel.setText(message == null ? "" : message); }
    public void showProgress(boolean show) {
        progressBar.setVisible(show);
        if (!show) progressBar.setProgress(0);
    }

    // Exposed controls/properties for container wiring
    public TableView<FeatureVector> getTable() { return table; }
    public ComboBox<String> getCollectionSelector() { return collectionSelector; }
    public ChoiceBox<String> getSamplingChoice() { return samplingChoice; }
    public TitledPane getDetailsSection() { return detailsSection; }
    public TitledPane getMetadataSection() { return metadataSection; }

    public StringProperty samplingModeProperty() { return samplingMode; }
    public String getSamplingMode() { return samplingMode.get(); }

    public StringProperty selectedCollectionProperty() { return selectedCollection; }
    public String getSelectedCollection() { return selectedCollection.get(); }

    public ObjectProperty<DetailLevel> detailLevelProperty() { return detailLevel; }
    public void setDetailLevel(DetailLevel level) { this.detailLevel.set(level); }
    public DetailLevel getDetailLevel() { return detailLevel.get(); }
}
