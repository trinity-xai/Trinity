package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Descriptions and drill-down membership use original source data only. */
public final class ContextGraphCommunityCatalogTest {
    @Test
    public void everyRepresentativeHasExactOriginalMembership() {
        GraphDirectedCollection graph = sample();
        var hierarchy = ContextGraphCommunityDetection.detect(graph, ContextGraphCommunityParams.defaults());
        var catalog = ContextGraphCommunityCatalog.build(graph, hierarchy);
        for (ContextGraphCommunityLevel level : hierarchy.levels()) {
            Set<String> covered = new java.util.HashSet<>();
            for (ContextGraphCommunityDescriptor descriptor : catalog.atLevel(level.index())) {
                Set<String> ids = catalog.members(descriptor.ref());
                check(ids.size() == descriptor.memberCount(), "Member count incorrect");
                for (String id : ids) check(covered.add(id), "Duplicate member within partition");
                check(!descriptor.title().isBlank(), "Missing representative description");
                if (level.index() > 0) {
                    for (int childId : descriptor.childIds()) {
                        var child = catalog.descriptor(new ContextGraphCommunityRef(level.index() - 1, childId));
                        check(child != null && child.parentId() == descriptor.ref().id(),
                            "Broken parent/child map");
                    }
                }
            }
            check(covered.size() == graph.getNodes().size(), "Lost source entity");
        }
    }

    @Test
    public void descriptionsAreDeterministicAndDoNotChangeSource() {
        GraphDirectedCollection graph = sample();
        var hierarchy = ContextGraphCommunityDetection.detect(graph, ContextGraphCommunityParams.defaults());
        var first = ContextGraphCommunityCatalog.build(graph, hierarchy);
        var oldEdges = List.copyOf(graph.getEdges());
        Collections.shuffle(graph.getNodes(), new Random(17));
        Collections.shuffle(graph.getEdges(), new Random(19));
        var second = ContextGraphCommunityCatalog.build(graph, hierarchy);
        for (ContextGraphCommunityLevel level : hierarchy.levels())
            check(first.atLevel(level.index()).equals(second.atLevel(level.index())),
                "Community descriptions depend on input ordering");
        check(graph.getEdges().containsAll(oldEdges), "Graph source objects modified");
        check(first.atLevel(0).stream().anyMatch(d -> d.title().contains("Technique")
            || d.title().contains("Intrusion Set")), "Source type labels were lost");
    }

    @Test
    public void interruptedCatalogStops() {
        GraphDirectedCollection graph = sample();
        var hierarchy = ContextGraphCommunityDetection.detect(graph, ContextGraphCommunityParams.defaults());
        Thread.currentThread().interrupt();
        try {
            boolean cancelled = false;
            try {
                ContextGraphCommunityCatalog.build(graph, hierarchy);
            } catch (java.util.concurrent.CancellationException expected) {
                cancelled = true;
            }
            check(cancelled, "Interrupted catalog continued");
        } finally {
            Thread.interrupted();
        }
    }

    private static GraphDirectedCollection sample() {
        var graph = new GraphDirectedCollection();
        for (int group = 0; group < 8; group++) {
            for (int i = 0; i < 5; i++) {
                GraphNode node = new GraphNode();
                node.setEntityID("g" + group + "n" + i);
                node.getLabels().add(group % 2 == 0 ? "Technique" : "Intrusion Set");
                node.getProperties().put("name", "Name " + group + " / " + i);
                graph.getNodes().add(node);
            }
            for (int i = 0; i < 5; i++) for (int j = i + 1; j < 5; j++)
                connect(graph, "g" + group + "n" + i, "g" + group + "n" + j);
            connect(graph, "g" + group + "n0", "g" + ((group + 1) % 8) + "n0");
        }
        return graph;
    }

    private static void connect(GraphDirectedCollection graph, String start, String end) {
        GraphEdge edge = new GraphEdge();
        edge.setStartID(start);
        edge.setEndID(end);
        GraphRelationship relationship = new GraphRelationship();
        relationship.getLabels().add("USES");
        edge.setRelationship(relationship);
        graph.getEdges().add(edge);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var test = new ContextGraphCommunityCatalogTest();
        test.everyRepresentativeHasExactOriginalMembership();
        test.descriptionsAreDeterministicAndDoNotChangeSource();
        test.interruptedCatalogStops();
        System.out.println("ContextGraphCommunityCatalogTest: 3 checks PASSED");
    }
}
