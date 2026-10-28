package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.utils.clustering.ClusterMethod;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TopicKeywordContrastTest {
    @Test void sharedSchemaTermsDoNotDominateAllTopicNames() {
        var samples = List.of(
            new TopicDiscoveryEngine.Sample(1, "attribute containing properties satellite trajectory orbital", new double[]{1, 0}),
            new TopicDiscoveryEngine.Sample(2, "attribute containing properties orbital satellite trajectory", new double[]{.99, .01}),
            new TopicDiscoveryEngine.Sample(3, "attribute containing properties trajectory orbital satellite", new double[]{.98, .02}),
            new TopicDiscoveryEngine.Sample(4, "attribute containing properties intrusion network firewall", new double[]{0, 1}),
            new TopicDiscoveryEngine.Sample(5, "attribute containing properties network intrusion firewall", new double[]{.01, .99}),
            new TopicDiscoveryEngine.Sample(6, "attribute containing properties firewall intrusion network", new double[]{.02, .98})
        );
        var result = TopicDiscoveryEngine.discover(samples,
            new TopicDiscoveryEngine.Options(ClusterMethod.KMEANS, 2, 2, 2, 0.5, 100));
        assertEquals(2, result.topics().size());
        for (var topic : result.topics()) {
            assertFalse(topic.keywords().isEmpty());
            assertFalse(topic.name().contains("attribute, containing"));
            assertFalse(topic.keywords().subList(0, Math.min(2, topic.keywords().size())).contains("attribute"));
        }
    }
}
