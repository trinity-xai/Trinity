package edu.jhuapl.trinity.data.graph.construction;

import edu.jhuapl.trinity.data.analysis.FeatureVectorAnalysisSnapshot;
import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import edu.jhuapl.trinity.utils.umap.UmapGraphSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A small, non-GUI bridge from UMAP's already-computed fuzzy topology to a
 * Context Graph-compatible representation. Neither UMAP coordinates nor
 * high-dimensional feature values are used as Poincare layout coordinates.
 *
 * Fuzzy memberships are not semantic facts. Reciprocal entries are combined
 * into one canonical graph edge, tagged as an undirected derived association.
 */
public final class UmapFuzzyGraphBuilder {
    public static final String RELATIONSHIP_LABEL = "UMAP_FUZZY_ASSOCIATION";

    private UmapFuzzyGraphBuilder() { }

    public static GraphDirectedCollection build(FeatureVectorAnalysisSnapshot source,
                                                 UmapGraphSnapshot umap,
                                                 double minimumMembership,
                                                 String graphId) {
        if (source == null || umap == null) throw new IllegalArgumentException("Missing source or UMAP snapshot");
        if (!source.inputRowIds().equals(umap.inputRowIds())) {
            throw new IllegalArgumentException("UMAP rows do not match the source collection's ordered entity IDs");
        }
        if (!Double.isFinite(minimumMembership) || minimumMembership < 0 || minimumMembership > 1) {
            throw new IllegalArgumentException("minimumMembership must lie between 0 and 1");
        }
        GraphDirectedCollection graph = new GraphDirectedCollection();
        graph.setGraphId(graphId == null || graphId.isBlank()
            ? "umap-fuzzy:" + source.collectionName() : graphId);

        List<GraphNode> nodes = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            FeatureVectorAnalysisSnapshot.Row row = source.rows().get(i);
            GraphNode node = new GraphNode();
            node.setIdentity(i + 1L);
            node.setEntityID(row.entityId());
            node.getLabels().add(row.label() == null || row.label().isBlank() ? "Passage" : row.label());
            node.getProperties().put("sourceEntityId", row.entityId());
            node.getProperties().put("sourceCollection", source.collectionName());
            if (row.text() != null && !row.text().isBlank()) {
                node.getProperties().put("name", row.text().substring(0, Math.min(100, row.text().length())));
            } else {
                node.getProperties().put("name", row.entityId());
            }
            // Preserve source metadata without copying the high-dimensional vector into a 3D position.
            node.getProperties().put("sourceMetadata", row.metadata());
            nodes.add(node);
        }
        graph.setNodes(nodes);

        // Row-index ordered canonical pair keys; deterministic edges regardless of sparse iteration order.
        Map<Long, Double> strengths = new TreeMap<>();
        for (UmapGraphSnapshot.WeightedEdge edge : umap.fuzzyEdges()) {
            int left = Math.min(edge.sourceRow(), edge.targetRow());
            int right = Math.max(edge.sourceRow(), edge.targetRow());
            if (left == right || edge.membership() < minimumMembership) continue;
            long key = ((long) left << 32) | (right & 0xffffffffL);
            strengths.merge(key, edge.membership(), Math::max);
        }
        List<GraphEdge> edges = new ArrayList<>(strengths.size());
        long identity = 1;
        for (Map.Entry<Long, Double> entry : strengths.entrySet()) {
            long key = entry.getKey();
            int left = (int) (key >>> 32);
            int right = (int) key;
            GraphEdge edge = new GraphEdge();
            edge.setStartID(source.inputRowIds().get(left));
            edge.setEndID(source.inputRowIds().get(right));
            GraphRelationship relationship = new GraphRelationship();
            relationship.setIdentity(identity++);
            relationship.getLabels().add(RELATIONSHIP_LABEL);
            relationship.getProperties().put("membership", entry.getValue());
            relationship.getProperties().put("undirected", true);
            relationship.getProperties().put("derived", true);
            relationship.getProperties().put("construction", "UMAP_FUZZY_GRAPH");
            relationship.getProperties().put("fitParameters", umap.fitParameters());
            edge.setRelationship(relationship);
            edges.add(edge);
        }
        graph.setEdges(edges);
        return graph;
    }
}
