package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphEdge;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Derived view record; holds the original GraphEdge identities without graph mutation. */
public final class ContextGraphCommunityEdgeBundle {
    private final ContextGraphCommunityPair pair;
    private final List<GraphEdge> originals;
    private final Map<String, Integer> typeCounts;
    private final int semanticCount;
    private final int structuralCount;
    private final int administrativeCount;

    ContextGraphCommunityEdgeBundle(ContextGraphCommunityPair pair, List<GraphEdge> originals,
                                    Map<String, Integer> typeCounts, int semanticCount,
                                    int structuralCount, int administrativeCount) {
        this.pair = pair;
        this.originals = List.copyOf(originals);
        this.typeCounts = Collections.unmodifiableMap(new LinkedHashMap<>(typeCounts));
        this.semanticCount = semanticCount;
        this.structuralCount = structuralCount;
        this.administrativeCount = administrativeCount;
    }

    public ContextGraphCommunityPair pair() { return pair; }
    public List<GraphEdge> originals() { return originals; }
    public Map<String, Integer> typeCounts() { return typeCounts; }
    public int originalCount() { return originals.size(); }
    public int semanticCount() { return semanticCount; }
    public int structuralCount() { return structuralCount; }
    public int administrativeCount() { return administrativeCount; }

    public String summary() {
        return "Derived connection: community " + (pair.first() + 1) + " to "
            + (pair.second() + 1) + " | all original edges=" + originalCount()
            + " (semantic=" + semanticCount + ", structural=" + structuralCount
            + ", administrative=" + administrativeCount + ") | original types=" + typeCounts;
    }
}
