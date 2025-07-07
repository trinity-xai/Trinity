package edu.jhuapl.trinity.javafx.components.hyperdrive;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageInput;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsTextListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import edu.jhuapl.trinity.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Sean Phillips
 */
public class RequestTextEmbeddingsTask extends HyperdriveTask {
    private static final Logger LOG = LoggerFactory.getLogger(RequestTextEmbeddingsTask.class);
    String currentEmbeddingsModel;
    List<EmbeddingsTextListItem> textEmbeddingsListItems;

    public RequestTextEmbeddingsTask(Scene scene, String currentEmbeddingsModel) {
        super(scene, null, new AtomicInteger(), null);
        this.currentEmbeddingsModel = currentEmbeddingsModel;
        this.textEmbeddingsListItems = null;
    }
    
    public RequestTextEmbeddingsTask(Scene scene, CircleProgressIndicator progressIndicator, 
        AtomicInteger requestNumber, String currentEmbeddingsModel,
        HashMap<Integer, REQUEST_STATUS> outstandingRequests,
        List<EmbeddingsTextListItem> textEmbeddingsListItems) {
        super(scene, progressIndicator, requestNumber, outstandingRequests);
        this.currentEmbeddingsModel = currentEmbeddingsModel;
        this.textEmbeddingsListItems = textEmbeddingsListItems;
    }

    @Override
    protected void processTask() throws Exception {
        if(null != progressIndicator) {
            progressIndicator.setFadeTimeMS(250);
            progressIndicator.setLabelLater("Encoding Text...");
            progressIndicator.spin(true);
            progressIndicator.fadeBusy(false);
        }
        LOG.info("Loading and Encoding Text...");
        long startTime = System.nanoTime();
        final int total = textEmbeddingsListItems.size();

        int percent = 100;
        if (total < 100)
            percent = 10;
        if (total < 10)
            percent = 1;
        int updatePercent = total / percent;

        double completed = 0;
        for (EmbeddingsTextListItem item : textEmbeddingsListItems) {
            if (null == item.contents)
                item.readText();
            EmbeddingsImageInput input = EmbeddingsImageInput.defaultTextInput(item.contents);
            if (null != currentEmbeddingsModel)
                input.setModel(currentEmbeddingsModel);
            List<Integer> inputIDs = new ArrayList<>();
            inputIDs.add(item.textID);
            try {
                int rn = requestNumber.getAndIncrement();
                RestAccessLayer.requestTextEmbeddings(
                    input, scene, inputIDs, rn);
                outstandingRequests.put(rn, REQUEST_STATUS.REQUESTED);
            } catch (JsonProcessingException ex) {
                LOG.error(null, ex);
            }
            completed++;
            if(null != progressIndicator) {
                if (completed % updatePercent == 0) {
                    progressIndicator.setPercentComplete(completed / total);
                }
                progressIndicator.setLabelLater("Encoding " + completed + " of " + total);
            }
            Thread.sleep(getRequestDelay());
        }
        Utils.logTotalTime(startTime);
        if(null != progressIndicator) {
            progressIndicator.setPercentComplete(completed / total);
            progressIndicator.setLabelLater("Requested " + completed + " of " + total);
        }
    }

//    public void requestEmbeddings(List<EmbeddingsImageUrl> currentBatch, List<Integer> inputIDs) {
//        EmbeddingsImageBatchInput input = new EmbeddingsImageBatchInput();
//        input.setInput(currentBatch);
//        input.setDimensions(512);
//        input.setEmbedding_type("all");
//        input.setEncoding_format("float");
//        input.setModel(currentEmbeddingsModel);
//        input.setUser("string");
//        try {
//            int rn = null != requestNumber ? requestNumber.incrementAndGet() : -1;
//            if(null != progressIndicator)
//                progressIndicator.setLabelLater("Embeddings Request " + requestNumber + "...");
//            LOG.info("Sending {} images for processing at {}", currentBatch.size(), LocalDateTime.now());
//            RestAccessLayer.requestImageEmbeddings(input, scene, inputIDs, rn);
//            if(null != outstandingRequests)
//                outstandingRequests.put(rn, REQUEST_STATUS.REQUESTED);
//        } catch (JsonProcessingException ex) {
//            LOG.error(null, ex);
//        }
//    }    
}
