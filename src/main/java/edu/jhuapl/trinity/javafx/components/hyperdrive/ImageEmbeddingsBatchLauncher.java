package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageBatchInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;

import static edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl.imageUrlFromImage;
import edu.jhuapl.trinity.messages.EmbeddingsImageCallback;
import java.util.ArrayList;

/**
 * Utility for launching a batch of image embedding requests and registering
 * completion callbacks with the BatchRequestManager.
 */
public class ImageEmbeddingsBatchLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(ImageEmbeddingsBatchLauncher.class);

    private final Scene scene;
    private final String currentEmbeddingsModel;

    public ImageEmbeddingsBatchLauncher(Scene scene, String model) {
        this.scene = scene;
        this.currentEmbeddingsModel = model;
    }

    /**
     * Launch a batch of embedding requests and register the completion
     * callback.
     *
     * @param batch List of EmbeddingsImageListItem to process in this batch.
     * @param batchNumber The unique batch number for this batch.
     * @param reqId The request number for this REST batch submission.
     * @param callback Completion callback, signature: (success, Exception)
     */
    public void launchBatch(List<EmbeddingsImageListItem> batch, int batchNumber, int reqId,
            BiConsumer<Boolean, Exception> callback) {
        //register the callback and fire the REST request.
        EmbeddingsImageCallback.completionCallbacks.put(reqId, callback);

        List<EmbeddingsImageUrl> inputs = new ArrayList<>();

        //Serial encoding (preserves GUI thread order)
        for (int i = 0; i < batch.size(); i++) {
            EmbeddingsImageListItem item = batch.get(i);
            inputs.add(imageUrlFromImage.apply(item.getCurrentImage()));
        }

        // Now build and send the request as before:
        EmbeddingsImageBatchInput input = new EmbeddingsImageBatchInput();
        input.setInput(inputs);
        input.setDimensions(512);
        input.setEmbedding_type("all");
        input.setEncoding_format("float");
        input.setModel(currentEmbeddingsModel);
        input.setUser("string");

        try {
            RestAccessLayer.requestImageEmbeddings(
                    input,
                    scene,
                    batch.stream().map(i -> i.imageID).toList(),
                    reqId
            );
            LOG.info("Image batch (reqId={}, batchNumber={}) sent: {} items.", reqId, batchNumber, batch.size());
        } catch (Exception ex) {
            LOG.error("Failed to send image embedding batch (reqId={}, batchNumber={}): {}", reqId, batchNumber, ex.getMessage());
            EmbeddingsImageCallback.completionCallbacks.remove(reqId);
            callback.accept(false, ex);
        }
    }
}
