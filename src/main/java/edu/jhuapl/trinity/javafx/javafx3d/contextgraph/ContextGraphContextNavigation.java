package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Navigation-only state for global/local context inspection.
 * Entity identities and the underlying graph never change; an anchor only scopes
 * derived relationship summaries at the currently viewed community level.
 */
public final class ContextGraphContextNavigation {
    public record Frame(int level, String anchorEntityId) { }

    private final Deque<Frame> history = new ArrayDeque<>();
    private String anchorEntityId;

    public String anchorEntityId() { return anchorEntityId; }
    public boolean canGoBack() { return !history.isEmpty(); }
    public int historyDepth() { return history.size(); }

    /** A selection is required; a finer level is chosen when one exists. */
    public int explore(String selectedEntityId, int currentLevel, int levelCount) {
        if (selectedEntityId == null || selectedEntityId.isBlank() || levelCount <= 0)
            throw new IllegalArgumentException("Select an original graph entity before exploring");
        int clamped = Math.max(0, Math.min(levelCount - 1, currentLevel));
        history.push(new Frame(clamped, anchorEntityId));
        anchorEntityId = selectedEntityId;
        return Math.max(0, clamped - 1);
    }

    public Frame back() {
        if (history.isEmpty()) return null;
        Frame previous = history.pop();
        anchorEntityId = previous.anchorEntityId();
        return previous;
    }

    public void reset() {
        anchorEntityId = null;
        history.clear();
    }

    /** Only bundles incident on the anchor's current community are shown. */
    public boolean includes(ContextGraphCommunityEdgeBundle bundle,
                            Integer anchorCommunityId) {
        if (anchorEntityId == null) return true;
        return anchorCommunityId != null && bundle != null
            && (bundle.pair().first() == anchorCommunityId
                || bundle.pair().second() == anchorCommunityId);
    }
}
