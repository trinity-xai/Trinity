package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageData;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import java.util.HashSet;
import java.util.Set;

/** Reject missing, extra, duplicate or out-of-range indexes before committing a batch. */
public final class EmbeddingBatchResponseValidator {
    private EmbeddingBatchResponseValidator() { }

    public static void validate(EmbeddingsImageOutput output, int expectedCount) {
        if (output == null || output.getData() == null || output.getData().size() != expectedCount) {
            throw new IllegalArgumentException("Embedding response count does not match the submitted batch");
        }
        Set<Integer> seen = new HashSet<>();
        for (EmbeddingsImageData item : output.getData()) {
            if (item == null || item.getIndex() < 0 || item.getIndex() >= expectedCount
                || !seen.add(item.getIndex()) || item.getEmbedding() == null || item.getEmbedding().isEmpty()) {
                throw new IllegalArgumentException("Invalid or duplicate embedding response index/data");
            }
        }
    }
}
