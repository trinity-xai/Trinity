package edu.jhuapl.trinity.javafx.components.hyperdrive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.javafx.components.panes.LitPathPane;
import edu.jhuapl.trinity.javafx.events.HyperdriveEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Floating schema-aware JSON extraction editor. Applies changes via HyperdriveEvent. */
public final class JsonSchemaMappingPane extends LitPathPane {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_SAMPLE_BYTES = 16 * 1024 * 1024;
    private final Scene editorScene;
    private final TextField profileName = new TextField("JSON Mapping");
    private final Label sampleLabel = new Label("Load a representative JSON file to detect fields.");
    private final VBox fieldsBox = new VBox(5);
    private final ToggleGroup labelGroup = new ToggleGroup();
    private final Map<String, MappingRow> rows = new LinkedHashMap<>();
    private final TextArea preview = new TextArea();
    private final Spinner<Integer> recordIndex = new Spinner<>(0, 0, 0);
    private JsonNode sampleRoot;

    public JsonSchemaMappingPane(Scene scene, Pane parent, JsonExtractionProfile current) {
        super(scene, parent, 1120, 710, new BorderPane(),
            "Hyperdrive", "JSON Schema Mapping", 300.0, 400.0);
        this.editorScene = scene;
        getStylesheets().add(StyleResourceProvider.getResource("styles.css").toExternalForm());
        getStylesheets().add(StyleResourceProvider.getResource("covalent.css").toExternalForm());
        Button sampleButton = new Button("Load Sample JSON...");
        sampleButton.setOnAction(e -> loadSample());
        Button loadButton = new Button("Load Profile...");
        loadButton.setOnAction(e -> loadProfile());
        Button saveButton = new Button("Save Profile...");
        saveButton.setOnAction(e -> saveProfile());
        Button previewButton = new Button("Preview Extracted Text");
        previewButton.setOnAction(e -> refreshPreview());

        HBox actions = new HBox(10, sampleButton, loadButton, saveButton);
        HBox header = new HBox(12, new Label("Profile Name"), profileName);
        HBox.setHgrow(profileName, Priority.ALWAYS);
        VBox top = new VBox(10, header, actions, sampleLabel);

        HBox columnHead = new HBox(14);
        Label pathHead = new Label("JSON field path");
        pathHead.setMinWidth(340);
        pathHead.setMaxWidth(340);
        columnHead.getChildren().addAll(pathHead, new Label("Embed"), new Label("Label"), new Label("Metadata"));
        fieldsBox.setPadding(new Insets(6));
        ScrollPane fieldScroll = new ScrollPane(fieldsBox);
        fieldScroll.setFitToWidth(true);
        VBox left = new VBox(7, columnHead, fieldScroll);
        VBox.setVgrow(fieldScroll, Priority.ALWAYS);
        left.setPrefWidth(620);

        recordIndex.setEditable(true);
        recordIndex.setPrefWidth(95);
        preview.setEditable(false);
        preview.setWrapText(true);
        preview.setPrefRowCount(17);
        VBox right = new VBox(9, new Label("Sample record index (zero-based)"), recordIndex, previewButton,
            new Label("Exact embedding input / label / metadata preview"), preview);
        right.setPrefWidth(420);
        VBox.setVgrow(preview, Priority.ALWAYS);
        HBox body = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        VBox layout = new VBox(12, top, body);
        VBox.setVgrow(body, Priority.ALWAYS);
        layout.setPadding(new Insets(12));
        Button applyButton = new Button("Apply Mapping");
        applyButton.setOnAction(e -> {
            JsonExtractionProfile profile = profileFromControls();
            if (!profile.hasEmbeddingFields()) {
                showError("Select at least one field in the Embed column.");
                return;
            }
            editorScene.getRoot().fireEvent(new HyperdriveEvent(
                HyperdriveEvent.JSON_SCHEMA_MAPPING_APPLIED, profile.copy()));
            close();
        });
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());
        HBox footer = new HBox(12, applyButton, closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        layout.getChildren().add(footer);
        ((BorderPane) contentPane).setCenter(layout);

        if (current != null) applyProfile(current);
        recordIndex.valueProperty().addListener((obs, oldValue, value) -> refreshPreview());
        refreshPreview();
    }

    private void addPath(String path) {
        if (rows.containsKey(path)) return;
        MappingRow row = new MappingRow(path, labelGroup);
        rows.put(path, row);
        fieldsBox.getChildren().add(row.node);
    }

    private JsonExtractionProfile profileFromControls() {
        JsonExtractionProfile profile = new JsonExtractionProfile();
        profile.setName(profileName.getText().isBlank() ? "JSON Mapping" : profileName.getText().strip());
        for (Map.Entry<String, MappingRow> entry : rows.entrySet()) {
            MappingRow row = entry.getValue();
            if (row.embed.isSelected() || row.label.isSelected() || row.metadata.isSelected()) {
                profile.getFields().add(new JsonExtractionProfile.FieldMapping(entry.getKey(),
                    row.embed.isSelected(), row.label.isSelected(), row.metadata.isSelected()));
            }
        }
        return profile;
    }

    private void applyProfile(JsonExtractionProfile profile) {
        profileName.setText(profile.getName());
        for (MappingRow row : rows.values()) {
            row.embed.setSelected(false);
            row.label.setSelected(false);
            row.metadata.setSelected(false);
        }
        if (profile.getFields() != null) for (var mapping : profile.getFields()) {
            if (mapping.getPath() == null || mapping.getPath().isBlank()) continue;
            addPath(mapping.getPath());
            MappingRow row = rows.get(mapping.getPath());
            row.embed.setSelected(mapping.isEmbedding());
            row.label.setSelected(mapping.isLabel());
            row.metadata.setSelected(mapping.isMetadata());
        }
        refreshPreview();
    }

    private void loadSample() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Representative JSON Document");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json", "*.txt"));
        File file = chooser.showOpenDialog(editorScene.getWindow());
        if (file == null) return;
        try {
            if (Files.size(file.toPath()) > MAX_SAMPLE_BYTES) {
                showError("The preview sample must be smaller than 16 MB. Select a smaller representative file.");
                return;
            }
            JsonNode node = MAPPER.readTree(file);
            if (node == null || (!node.isArray() && !node.isObject())) {
                showError("The sample must contain a JSON object or an array of objects.");
                return;
            }
            sampleRoot = node;
            for (String path : JsonExtractionProfile.discoverPaths(node)) addPath(path);
            int max = node.isArray() ? Math.max(0, node.size() - 1) : 0;
            recordIndex.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max, 0));
            sampleLabel.setText(file.getName() + " — " + rows.size() + " detected scalar field paths");
            refreshPreview();
        } catch (Exception ex) {
            showError("Unable to inspect JSON sample: " + ex.getMessage());
        }
    }

    private void loadProfile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load JSON Extraction Profile");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Profiles", "*.json"));
        File file = chooser.showOpenDialog(editorScene.getWindow());
        if (file == null) return;
        try {
            applyProfile(MAPPER.readValue(file, JsonExtractionProfile.class));
        } catch (Exception ex) { showError("Unable to load profile: " + ex.getMessage()); }
    }

    private void saveProfile() {
        JsonExtractionProfile profile = profileFromControls();
        if (!profile.hasEmbeddingFields()) { showError("Choose at least one embedding field before saving."); return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save JSON Extraction Profile");
        chooser.setInitialFileName("hyperdrive-json-mapping.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Profiles", "*.json"));
        File file = chooser.showSaveDialog(editorScene.getWindow());
        if (file == null) return;
        try { MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, profile); }
        catch (Exception ex) { showError("Unable to save profile: " + ex.getMessage()); }
    }

    private void refreshPreview() {
        if (sampleRoot == null) {
            preview.setText("Load sample JSON to preview the exact text submitted for embeddings.");
            return;
        }
        JsonNode record = sampleRoot.isArray() ? sampleRoot.path(recordIndex.getValue()) : sampleRoot;
        JsonExtractionProfile.Extracted result = profileFromControls().extract(record, "Unlabeled JSON Record");
        StringBuilder content = new StringBuilder("EMBEDDING INPUT\n\n");
        content.append(result.text().isBlank() ? "(No selected field has usable content)" : result.text());
        content.append("\n\nLABEL\n").append(result.label());
        content.append("\n\nMETADATA\n");
        if (result.metadata().isEmpty()) content.append("(none)");
        else result.metadata().forEach((k, v) -> content.append(k).append(" = ").append(v).append('\n'));
        preview.setText(content.toString());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("JSON Schema Mapping");
        alert.setHeaderText("Mapping configuration");
        alert.getDialogPane().getStylesheets().add(StyleResourceProvider.getResource("dialogstyles.css").toExternalForm());
        alert.showAndWait();
    }
}
