package edu.jhuapl.trinity.javafx.components.hyperdrive;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl.imageUrlFromImage;


/**
 * @author Sean Phillips
 */
public class RequestCaptionsTask extends HyperdriveTask {
    private static final Logger LOG = LoggerFactory.getLogger(RequestCaptionsTask.class);
    String currentChatModel;
    List<EmbeddingsImageListItem> items;

    public RequestCaptionsTask(Scene scene, String currentChatModel) {
        super(scene, null, new AtomicInteger(), null);
        this.currentChatModel = currentChatModel;
        this.items = null;
    }

    public RequestCaptionsTask(Scene scene, CircleProgressIndicator progressIndicator,
                               AtomicInteger requestNumber, String currentChatModel,
                               Map<Integer, REQUEST_STATUS> outstandingRequests,
                               List<EmbeddingsImageListItem> items) {
        super(scene, progressIndicator, requestNumber, outstandingRequests);
        this.currentChatModel = currentChatModel;
        this.items = items;
    }

    @Override
    protected void processTask() throws Exception {
        if (null != progressIndicator) {
            progressIndicator.setFadeTimeMS(250);
            progressIndicator.setLabelLater("Requesting Captions...");
            progressIndicator.spin(true);
            progressIndicator.fadeBusy(false);
        }
        //@DEBUG SMP
        //System.out.println("Requesting Captions...");
        for (EmbeddingsImageListItem item : items) {
            EmbeddingsImageUrl url = imageUrlFromImage.apply(item.getCurrentImage());
            try {
                ChatCompletionsInput input = ChatCompletionsInput.defaultImageInput(url.getImage_url(), ChatCompletionsInput.CAPTION_TYPE.DEFAULT);
                if (null != currentChatModel)
                    input.setModel(currentChatModel);
                RestAccessLayer.requestChatCompletion(input, scene,
                    item.imageID, requestNumber.getAndIncrement());
            } catch (JsonProcessingException ex) {
                LOG.error(null, ex);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
            Thread.sleep(getRequestDelay());
        }
    }
}
