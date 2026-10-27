package edu.jhuapl.trinity.javafx.components.hyperdrive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LandmarkEmbeddingStateTest {
    @Test
    void acceptsOnlyTheMatchingRequestVersionAndModel() {
        LandmarkEmbeddingState state = new LandmarkEmbeddingState();
        long version = state.beginRequest();
        assertTrue(state.canAccept(version, "intrusion", "model-a", "intrusion", "model-a"));
        assertFalse(state.canAccept(version, "intrusion", "model-a", "intrusion", "model-b"));
        state.accepted("intrusion", "model-a");
        assertTrue(state.isFresh("intrusion", "model-a"));
        assertFalse(state.isFresh("intrusion", "model-b"));
    }

    @Test
    void editingInvalidatesEvenWhenRequestIsStillInFlight() {
        LandmarkEmbeddingState state = new LandmarkEmbeddingState();
        long version = state.beginRequest();
        state.textChanged();
        assertFalse(state.canAccept(version, "old", "m", "new", "m"));
        assertFalse(state.isFresh("new", "m"));
    }

    @Test
    void newestRefreshWinsWhenResponsesArriveOutOfOrder() {
        LandmarkEmbeddingState state = new LandmarkEmbeddingState();
        long first = state.beginRequest();
        long second = state.beginRequest();
        assertFalse(state.canAccept(first, "X", "m", "X", "m"));
        assertTrue(state.canAccept(second, "X", "m", "X", "m"));
    }
}
