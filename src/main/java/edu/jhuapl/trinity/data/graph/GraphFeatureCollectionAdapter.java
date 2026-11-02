package edu.jhuapl.trinity.data.graph;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/** Maintains original graph objects alongside FeatureVectors for shared Trinity tools. */
public final class GraphFeatureCollectionAdapter {
    private final FeatureCollection featureCollection = new FeatureCollection();
    private final Map<String, GraphNode> nodesById = new LinkedHashMap<>();
    private final Map<String, FeatureVector> featuresById = new LinkedHashMap<>();
    private final Map<FeatureVector, GraphNode> nodesByFeature = new IdentityHashMap<>();

    public GraphFeatureCollectionAdapter(GraphDirectedCollection graph) {
        if (graph == null || graph.getNodes() == null) return;
        for (GraphNode node : graph.getNodes()) {
            if (node == null || node.getEntityID() == null) continue;
            String id = node.getEntityID();
            if (nodesById.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate graph node ID: " + id);
            }
            FeatureVector feature = new FeatureVector(node.getVector());
            feature.setEntityId(id);
            feature.setLabel(node.getLabels() == null || node.getLabels().isEmpty()
                ? id : String.join(", ", node.getLabels()));
            feature.getMetaData().put("graph.nodeId", id);
            if (node.getProperties() != null) {
                node.getProperties().forEach((key, value) -> {
                    if (key != null && value != null)
                        feature.getMetaData().put(key, String.valueOf(value));
                });
                Object text = node.getProperties().get("text");
                if (text != null) feature.setText(String.valueOf(text));
            }
            if (feature.getText() == null || feature.getText().isBlank()) {
                StringBuilder summary = new StringBuilder("Graph Node: ").append(id);
                if (node.getLabels() != null && !node.getLabels().isEmpty())
                    summary.append("\nLabels: ").append(String.join(", ", node.getLabels()));
                if (node.getProperties() != null) node.getProperties().forEach((key,value) -> {
                    if (key != null && value != null) summary.append("\n").append(key).append(": ").append(value);
                });
                feature.setText(summary.toString());
            }
            nodesById.put(id, node);
            featuresById.put(id, feature);
            nodesByFeature.put(feature, node);
            featureCollection.getFeatures().add(feature);
        }
    }

    public FeatureCollection getFeatureCollection() { return featureCollection; }
    public FeatureVector featureFor(GraphNode node) {
        return node == null ? null : featuresById.get(node.getEntityID());
    }
    public GraphNode graphNodeFor(FeatureVector feature) { return nodesByFeature.get(feature); }
    public GraphNode graphNodeFor(String id) { return nodesById.get(id); }
    public Map<String, FeatureVector> getFeaturesById() {
        return Collections.unmodifiableMap(featuresById);
    }
}
