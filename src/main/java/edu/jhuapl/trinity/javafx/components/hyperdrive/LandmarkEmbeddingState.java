package edu.jhuapl.trinity.javafx.components.hyperdrive;

import java.util.Objects;

/**
 * Pure Java request-version tracking for an editable text landmark.
 * In-flight replies are rejected after an edit or a newer refresh request.
 */
public final class LandmarkEmbeddingState {
    private long revision;
    private boolean ready;
    private String embeddedText;
    private String embeddedModel;

    public void textChanged() {
        revision++;
        ready = false;
    }

    public long beginRequest() {
        revision++;
        ready = false;
        return revision;
    }

    public boolean canAccept(long expectedRevision, String requestedText,
                             String requestedModel, String currentText, String currentModel) {
        return revision == expectedRevision
            && Objects.equals(requestedText, currentText)
            && Objects.equals(requestedModel, currentModel);
    }

    public void accepted(String text, String model) {
        embeddedText = text;
        embeddedModel = model;
        ready = true;
    }

    public boolean isFresh(String text, String model) {
        return ready && Objects.equals(embeddedText, text)
            && Objects.equals(embeddedModel, model);
    }
}
