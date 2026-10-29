package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsTextBatchInput;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsTextListItem;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import java.util.List;
import java.util.function.BiConsumer;

/** Submits a batch of text chunks with stable, positional input IDs. */
public class TextEmbeddingsBatchLauncher {
    private String model;
    public TextEmbeddingsBatchLauncher(String model) { this.model = model; }
    public void setModel(String model) { this.model = model; }

    public void launchBatch(List<EmbeddingsTextListItem> batch, int batchNumber, int reqId,
                            long timeoutMillis, BiConsumer<EmbeddingsImageOutput, Exception> callback) {
        EmbeddingsTextBatchInput input = new EmbeddingsTextBatchInput();
        input.setModel(model);
        if (batch.size() == 1) {
            input.setInput(batch.get(0).contents); // preserve legacy single-string server compatibility
        } else {
            input.setInput(batch.stream().map(item -> item.contents).toList());
        }
        RestAccessLayer.requestManagedTextEmbeddings(input, timeoutMillis, (output, error) -> {
            if (error == null) {
                try {
                    EmbeddingBatchResponseValidator.validate(output, batch.size());
                } catch (Exception ex) {
                    error = ex;
                }
            }
            if (output != null) output.setRequestNumber(reqId);
            callback.accept(output, error);
        });
    }
}
