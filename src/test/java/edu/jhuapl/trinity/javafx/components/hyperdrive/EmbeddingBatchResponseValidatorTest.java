package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageData;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsTextBatchInput;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EmbeddingBatchResponseValidatorTest {
    private static EmbeddingsImageData item(int index) {
        var data = new EmbeddingsImageData();
        data.setIndex(index);
        data.setEmbedding(List.of(0.1, 0.2));
        return data;
    }

    @Test
    void acceptsReorderedDataAndRejectsMissingDuplicateOrOutOfRangeIndexes() {
        var response = new EmbeddingsImageOutput();
        response.setData(List.of(item(2), item(0), item(1)));
        assertDoesNotThrow(() -> EmbeddingBatchResponseValidator.validate(response, 3));
        assertThrows(IllegalArgumentException.class,
            () -> EmbeddingBatchResponseValidator.validate(response, 4));
        response.setData(List.of(item(0), item(0), item(2)));
        assertThrows(IllegalArgumentException.class,
            () -> EmbeddingBatchResponseValidator.validate(response, 3));
        response.setData(List.of(item(0), item(1), item(5)));
        assertThrows(IllegalArgumentException.class,
            () -> EmbeddingBatchResponseValidator.validate(response, 3));
    }

    @Test
    void supportsLegacySingleStringAndMultiStringInputs() {
        var input = new EmbeddingsTextBatchInput();
        input.setInput("one");
        assertInstanceOf(String.class, input.getInput());
        input.setInput(List.of("one", "two"));
        assertInstanceOf(List.class, input.getInput());
    }
}
