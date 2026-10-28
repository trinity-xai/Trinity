package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.utils.clustering.ClusterMethod;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TopicDiscoveryEngineTest {
    private static List<TopicDiscoveryEngine.Sample> sampleData() {
        return List.of(
            new TopicDiscoveryEngine.Sample(1, "satellite signal interference", new double[]{1, 0, 0}),
            new TopicDiscoveryEngine.Sample(2, "signal jamming interference", new double[]{0.98, 0.02, 0}),
            new TopicDiscoveryEngine.Sample(3, "battery power anomaly", new double[]{0, 1, 0}),
            new TopicDiscoveryEngine.Sample(4, "power battery failure", new double[]{0.02, 0.98, 0})
        );
    }

    @Test void discoversCentroidsOnOriginalVectorDimensions() {
        var result = TopicDiscoveryEngine.discover(sampleData(),
            new TopicDiscoveryEngine.Options(ClusterMethod.KMEANS, 2, 2, 2, 0.5, 100));
        assertEquals(2, result.topics().size());
        assertEquals(4, result.topics().stream().mapToInt(TopicDiscoveryEngine.Topic::size).sum());
        for (var topic : result.topics()) {
            assertEquals(3, topic.centroid().length);
            assertTrue(java.util.Arrays.stream(topic.centroid()).allMatch(Double::isFinite));
            assertFalse(topic.representative().isEmpty());
            assertTrue(Double.isFinite(topic.cohesion()));
            assertTrue(Double.isFinite(topic.distinctiveness()));
        }
    }

    @Test void mergeCombinesMembersAndDoesNotMutateExistingTopics() {
        var result = TopicDiscoveryEngine.discover(sampleData(),
            new TopicDiscoveryEngine.Options(ClusterMethod.KMEANS, 2, 2, 2, 0.5, 100));
        var original = result.topics();
        var merged = TopicDiscoveryEngine.merge(1000, original);
        assertEquals(4, merged.size());
        assertEquals(2, original.size());
        assertNotSame(original.get(0).centroid(), merged.centroid());
        assertEquals(4, merged.members().stream().map(TopicDiscoveryEngine.Sample::id).distinct().count());
    }

    @Test void rejectedInvalidOrIncompatibleEmbeddings() {
        var inputs = new ArrayList<>(sampleData());
        inputs.add(new TopicDiscoveryEngine.Sample(5, "bad dimensions", new double[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> TopicDiscoveryEngine.discover(inputs,
            new TopicDiscoveryEngine.Options(ClusterMethod.KMEANS, 2, 2, 2, 0.5, 100)));
        inputs.remove(4);
        inputs.add(new TopicDiscoveryEngine.Sample(5, "zero", new double[]{0, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> TopicDiscoveryEngine.discover(inputs,
            new TopicDiscoveryEngine.Options(ClusterMethod.KMEANS, 2, 2, 2, 0.5, 100)));
    }

    @Test void topicTitleEditsDoNotModifyNumericPrototypes() {
        var topic = TopicDiscoveryEngine.discover(sampleData(),
            new TopicDiscoveryEngine.Options(ClusterMethod.KMEANS, 2, 2, 2, 0.5, 100)).topics().get(0);
        double[] original = topic.centroid();
        topic.setName("Reviewed Category");
        topic.setSummary("An analyst-supplied description");
        assertArrayEquals(original, topic.centroid());
        assertEquals("Reviewed Category", topic.name());
    }
}
