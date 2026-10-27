package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.javafx.components.hyperdrive.LandmarkEmbeddingState;
import javafx.scene.control.TextField;

import java.util.List;

import static edu.jhuapl.trinity.javafx.events.CommandTerminalEvent.notifyTerminalWarning;

/** Editable text landmark whose embeddings are invalidated on any text change. */
public class LandmarkTextListItem extends LandmarkListItem {
    public static double PREF_TEXTFIELD_WIDTH = 250;
    private final TextField labelTextField;
    private final LandmarkEmbeddingState embeddingState = new LandmarkEmbeddingState();
    private boolean editedAfterEmbedding;

    public LandmarkTextListItem(String landmarkLabel) {
        super(landmarkLabel);
        labelTextField = new TextField();
        labelTextField.setEditable(true);
        labelTextField.setPrefWidth(PREF_TEXTFIELD_WIDTH);
        labelTextField.setText(landmarkLabel);
        labelTextField.textProperty().addListener((obs, previous, current) -> {
            if (hasEmbeddings()) editedAfterEmbedding = true;
            embeddingState.textChanged();
            invalidateEmbeddings();
            getFeatureVector().setLabel(current);
        });
        // Do not spam the status line on each keystroke.
        labelTextField.focusedProperty().addListener((obs, oldFocus, focused) -> {
            if (!focused && editedAfterEmbedding && getScene() != null) {
                notifyTerminalWarning("Text landmark edited; refresh its embedding before similarity classification.", getScene());
                editedAfterEmbedding = false;
            }
        });
        getChildren().add(labelTextField);
        setSpacing(20);
        setPrefHeight(32);
    }

    /** Called on the FX thread immediately before sending a landmark request. */
    public long beginEmbeddingRequest() {
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
        return hasEmbeddings() && embeddingState.isFresh(labelTextField.getText(), selectedModel);
    }

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
