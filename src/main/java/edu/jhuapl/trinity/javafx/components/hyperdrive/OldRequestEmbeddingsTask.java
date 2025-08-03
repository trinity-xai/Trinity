package edu.jhuapl.trinity.javafx.components.hyperdrive;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageBatchInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import edu.jhuapl.trinity.utils.Utils;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl.imageUrlFromImage;


/**
 * @author Sean Phillips
 */
public class OldRequestEmbeddingsTask extends HyperdriveTask {
    private static final Logger LOG = LoggerFactory.getLogger(OldRequestEmbeddingsTask.class);
    String currentEmbeddingsModel;
    List<EmbeddingsImageListItem> imageEmbeddingsListItems;

    public OldRequestEmbeddingsTask(Scene scene, String currentEmbeddingsModel) {
        super(scene, null, new AtomicInteger(), null);
        this.currentEmbeddingsModel = currentEmbeddingsModel;
        this.imageEmbeddingsListItems = null;
    }

    public OldRequestEmbeddingsTask(Scene scene, CircleProgressIndicator progressIndicator,
                                 AtomicInteger requestNumber, String currentEmbeddingsModel,
                                 HashMap<Integer, REQUEST_STATUS> outstandingRequests,
                                 List<EmbeddingsImageListItem> imageEmbeddingsListItems) {
        super(scene, progressIndicator, requestNumber, outstandingRequests);
        this.currentEmbeddingsModel = currentEmbeddingsModel;
        this.imageEmbeddingsListItems = imageEmbeddingsListItems;
    }

    @Override
    protected void processTask() throws Exception {
        AtomicInteger atomicCount = new AtomicInteger(0);
        if (null != progressIndicator) {
            progressIndicator.setFadeTimeMS(250);
            progressIndicator.setLabelLater("Encoding Images...");
            progressIndicator.spin(true);
            progressIndicator.fadeBusy(false);
        }
        LOG.info("Loading and Encoding Images...");
        long startTime = System.nanoTime();
        List<EmbeddingsImageUrl> inputs = new ArrayList<>();
        List<Integer> inputIDs = new ArrayList<>();
        final double total = imageEmbeddingsListItems.size();
        imageEmbeddingsListItems.parallelStream().forEach(item -> {
            inputs.add(imageUrlFromImage.apply(item.getCurrentImage()));
            inputIDs.add(item.imageID);
            if (null != progressIndicator) {
                double completed = inputs.size();
                progressIndicator.setPercentComplete(completed / total);
                progressIndicator.setLabelLater("Encoding " + completed + " of " + total);
            }
        });
        Utils.logTotalTime(startTime);
        double completed = atomicCount.incrementAndGet();
        if (null != progressIndicator) {
            progressIndicator.setPercentComplete(completed / total);
            progressIndicator.setLabelLater("Requested " + completed + " of " + total);
        }
        //break up the requests based on batch size
        int currentIndex = 0;
        while (currentIndex < inputs.size()) {
            int endCurrentIndex = currentIndex + getBatchSize();
            if (endCurrentIndex > inputs.size())
                endCurrentIndex = inputs.size();
            List<EmbeddingsImageUrl> currentBatch =
                inputs.subList(currentIndex, endCurrentIndex);
            LOG.info("Batch created: {}", currentBatch.size());
            requestEmbeddings(currentBatch, inputIDs.subList(currentIndex, endCurrentIndex));
            currentIndex += getBatchSize();
            completed = Integer.valueOf(currentIndex).doubleValue();
            if (null != progressIndicator) {
                progressIndicator.setPercentComplete(completed / total);
                progressIndicator.setLabelLater("Requested " + completed + " of " + total);
            }
            Thread.sleep(getRequestDelay());
        }
    }

    public void requestEmbeddings(List<EmbeddingsImageUrl> currentBatch, List<Integer> inputIDs) {
        EmbeddingsImageBatchInput input = new EmbeddingsImageBatchInput();
        input.setInput(currentBatch);
        input.setDimensions(512);
        input.setEmbedding_type("all");
        input.setEncoding_format("float");
        input.setModel(currentEmbeddingsModel);
        input.setUser("string");
        try {
            int rn = null != requestNumber ? requestNumber.incrementAndGet() : -1;
            if (null != progressIndicator)
                progressIndicator.setLabelLater("Embeddings Request " + requestNumber + "...");
            LOG.info("Sending {} images for processing at {}", currentBatch.size(), LocalDateTime.now());
            RestAccessLayer.requestImageEmbeddings(input, scene, inputIDs, rn);
            if (null != outstandingRequests)
                outstandingRequests.put(rn, REQUEST_STATUS.REQUESTED);
        } catch (JsonProcessingException ex) {
            LOG.error(null, ex);
        }
    }
}
