package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.javafx.components.hyperdrive.LandmarkEmbeddingState;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

import java.util.List;
import java.util.Objects;

import static edu.jhuapl.trinity.javafx.events.CommandTerminalEvent.notifyTerminalWarning;

/** Editable text landmark whose embeddings are invalidated on any text change. */
public class LandmarkTextListItem extends LandmarkListItem {
    public static double PREF_TEXTFIELD_WIDTH = 200;
    private final TextField labelTextField;
    private final LandmarkEmbeddingState embeddingState = new LandmarkEmbeddingState();
    private boolean editedAfterEmbedding;
    private boolean discoveredCentroid;
    private String discoveredModel;
    private final Label sourceBadge = new Label();

    public LandmarkTextListItem(String landmarkLabel) {
        super(landmarkLabel);
        labelTextField = new TextField();
        labelTextField.setEditable(true);
        labelTextField.setPrefWidth(PREF_TEXTFIELD_WIDTH);
        labelTextField.setText(landmarkLabel);
        labelTextField.textProperty().addListener((obs, previous, current) -> {
            if (!discoveredCentroid) {
                if (hasEmbeddings()) editedAfterEmbedding = true;
                embeddingState.textChanged();
                invalidateEmbeddings();
            }
            getFeatureVector().setLabel(current);
        });
        // Do not spam the status line on each keystroke.
        labelTextField.focusedProperty().addListener((obs, oldFocus, focused) -> {
            if (!focused && editedAfterEmbedding && getScene() != null) {
                notifyTerminalWarning("Text landmark edited; refresh its embedding before similarity classification.", getScene());
                editedAfterEmbedding = false;
            }
        });
        getChildren().addAll(labelTextField, sourceBadge);
        setSpacing(20);
        setPrefHeight(32);
    }

    /** Called on the FX thread immediately before sending a landmark request. */
    public long beginEmbeddingRequest() {
        if (discoveredCentroid) throw new IllegalStateException("Cluster centroids cannot be refreshed as text embeddings");
        invalidateEmbeddings();
        editedAfterEmbedding = false;
        return embeddingState.beginRequest();
    }

    /** Returns false for late or mismatched replies, without changing the landmark. */
    public boolean acceptEmbeddingResponse(long revision, String requestedText,
                                           String requestedModel, String selectedModel,
                                           List<Double> embedding) {
        if (!embeddingState.canAccept(revision, requestedText, requestedModel,
                labelTextField.getText(), selectedModel)) return false;
        setEmbeddings(embedding);
        embeddingState.accepted(requestedText, requestedModel);
        return true;
    }

    public boolean isEmbeddingFresh(String selectedModel) {
        if (discoveredCentroid) return hasEmbeddings() && Objects.equals(discoveredModel, selectedModel);
        return hasEmbeddings() && embeddingState.isFresh(labelTextField.getText(), selectedModel);
    }

    /** A centroid is a numeric prototype, not an embedding of the displayed topic name. */
    public void installDiscoveredCentroid(List<Double> vector, String description,
                                          int chunkCount, String embeddingModel) {
        if (vector == null || vector.isEmpty() || vector.stream().anyMatch(v -> v == null || !Double.isFinite(v)))
            throw new IllegalArgumentException("Invalid discovered centroid");
        discoveredCentroid = true;
        discoveredModel = embeddingModel;
        editedAfterEmbedding = false;
        setEmbeddings(vector);
        sourceBadge.setText("Cluster");
        sourceBadge.setTooltip(new javafx.scene.control.Tooltip("Discovered topic centroid; refresh by rediscovering topics"));
        addMetaData("landmark_source", "discovered_topic_centroid");
        if (embeddingModel != null) addMetaData("embedding_model", embeddingModel);
        addMetaData("topic_chunk_count", Integer.toString(chunkCount));
        if (description != null) getFeatureVector().setText(description);
    }

    public boolean isDiscoveredCentroid() { return discoveredCentroid; }

    @Override
    public void setFeatureVectorLabel(String text) {
        labelTextField.setText(text);
        super.setFeatureVectorLabel(text);
    }

    @Override
    public String getFeatureVectorLabel() {
        return labelTextField.getText();
    }

    public void setLabelWidth(double width) {
        labelTextField.setPrefWidth(width);
    }
}
