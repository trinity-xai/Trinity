package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageBatchInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import edu.jhuapl.trinity.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl.imageUrlFromImage;

/** Sends an image batch without applying results until its manager accepts completion. */
public class ImageEmbeddingsBatchLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(ImageEmbeddingsBatchLauncher.class);
    private String currentEmbeddingsModel;

    public ImageEmbeddingsBatchLauncher(javafx.scene.Scene scene, String model) {
        this.currentEmbeddingsModel = model;
    }

    public void launchBatch(List<EmbeddingsImageListItem> batch, int batchNumber, int reqId,
                            long timeoutMillis,
                            BiConsumer<EmbeddingsImageOutput, Exception> callback) {
        List<EmbeddingsImageUrl> inputs = new ArrayList<>();
        try {
            for (EmbeddingsImageListItem item : batch) {
                inputs.add(imageUrlFromImage.apply(item.isImageLoaded()
                    ? item.getCurrentImage() : ResourceUtils.loadImageFile(item.getFile())));
            }
        } catch (IOException | RuntimeException ex) {
            LOG.error("Unable to encode image batch {}", batchNumber, ex);
            callback.accept(null, ex);
            return;
        }
        EmbeddingsImageBatchInput input = new EmbeddingsImageBatchInput();
        input.setInput(inputs);
        input.setDimensions(512);
        input.setEmbedding_type("all");
        input.setEncoding_format("float");
        input.setModel(currentEmbeddingsModel);
        input.setUser("string");
        RestAccessLayer.requestManagedImageEmbeddings(input, timeoutMillis, (output, error) -> {
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

    public String getCurrentEmbeddingsModel() { return currentEmbeddingsModel; }
    public void setCurrentEmbeddingsModel(String model) { currentEmbeddingsModel = model; }
}
