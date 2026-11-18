package edu.jhuapl.trinity.data.graph.construction;

import edu.jhuapl.trinity.data.analysis.FeatureVectorAnalysisSnapshot;
import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import edu.jhuapl.trinity.utils.umap.UmapGraphSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * Build the same fuzzy topology, but place initial Euclidean nodes using
     * actual UMAP coordinates captured from the matching fit. A finite bounding
     * span is normalized to 600 world units, independent of raw coordinate
     * magnitudes; the Context Graph's coordinate scale remains functional.
     */
    public static GraphDirectedCollection build(UmapFittedGraphData fitted,
                                                double minimumMembership, String graphId) {
        if (fitted == null) throw new IllegalArgumentException("Missing fitted UMAP data");
        GraphDirectedCollection graph = build(fitted.source(), fitted.graph(),
            minimumMembership, graphId);
        if (fitted.hasProjectedCoordinates()) {
            placeInitialEuclideanNodes(graph.getNodes(), fitted.projectedCoordinates());
        }
        return graph;
    }

    private static void placeInitialEuclideanNodes(List<GraphNode> nodes,
                                                    List<List<Double>> rows) {
        final int dimensions = Math.min(3, rows.get(0).size());
        double[] mins = new double[dimensions];
        double[] maxs = new double[dimensions];
        Arrays.fill(mins, Double.POSITIVE_INFINITY);
        Arrays.fill(maxs, Double.NEGATIVE_INFINITY);
        for (List<Double> row : rows) {
            for (int d = 0; d < dimensions; d++) {
                mins[d] = Math.min(mins[d], row.get(d));
                maxs[d] = Math.max(maxs[d], row.get(d));
            }
        }
        double longestSpan = 0.0;
        for (int d = 0; d < dimensions; d++) {
            longestSpan = Math.max(longestSpan, maxs[d] - mins[d]);
        }
        // UMAP can produce very small coordinates; avoid using their magnitudes
        // as world units. Exact duplicates remain duplicates, not invented facts.
        double scale = longestSpan > 0.0 ? 600.0 / longestSpan : 1.0;
        for (int i = 0; i < nodes.size(); i++) {
            List<Double> row = rows.get(i);
            double[] position = new double[3];
            for (int d = 0; d < dimensions; d++) {
                position[d] = (row.get(d) - (mins[d] + maxs[d]) / 2.0) * scale;
                if (!Double.isFinite(position[d])) {
                    throw new IllegalArgumentException("Cannot normalize fitted UMAP coordinates");
                }
            }
            // Match Projections 3D's inverted Y screen convention.
            position[1] = -position[1];
            GraphNode node = nodes.get(i);
            node.setVector(new ArrayList<>(List.of(position[0], position[1], position[2])));
            node.getProperties().put("umapProjectedCoordinates", row);
            node.getProperties().put("coordinateSource", "UMAP_PROJECTION");
        }
    }

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
            if (row.text() != null && !row.text().isBlank()) {
                node.getProperties().put("text", row.text());
            }
            String imageURL = row.imageURL();
            if ((imageURL == null || imageURL.isBlank()) && row.metadata() != null) {
                for (String key : List.of("imageURL", "imageUrl", "imagePath", "image_path")) {
                    String candidate = row.metadata().get(key);
                    if (candidate != null && !candidate.isBlank()) {
                        imageURL = candidate;
                        break;
                    }
                }
            }
            if (imageURL != null && !imageURL.isBlank()) node.getProperties().put("imageURL", imageURL);
            if (row.mediaURL() != null && !row.mediaURL().isBlank())
                node.getProperties().put("mediaURL", row.mediaURL());
            if (row.imageId() != 0) node.getProperties().put("imageId", row.imageId());
            if (row.frameId() != 0) node.getProperties().put("frameId", row.frameId());
            if (!row.bbox().isEmpty()) node.getProperties().put("bbox", row.bbox());
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
