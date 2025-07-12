package edu.jhuapl.trinity.javafx.components.hyperdrive;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl;
import edu.jhuapl.trinity.data.messages.llm.Prompts;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl.imageUrlFromImage;


/**
 * @author Sean Phillips
 */
public class ChooseCaptionsTask extends HyperdriveTask {
    private static final Logger LOG = LoggerFactory.getLogger(ChooseCaptionsTask.class);
    String currentChatModel;
    List<EmbeddingsImageListItem> items;
    List<String> choices;

    public ChooseCaptionsTask(Scene scene, String currentChatModel) {
        super(scene, null, new AtomicInteger(), null);
        this.currentChatModel = currentChatModel;
        this.items = null;
    }

    public ChooseCaptionsTask(Scene scene, CircleProgressIndicator progressIndicator,
                              AtomicInteger requestNumber, String currentChatModel,
                              HashMap<Integer, REQUEST_STATUS> outstandingRequests,
                              List<EmbeddingsImageListItem> items, List<String> choices) {
        super(scene, progressIndicator, requestNumber, outstandingRequests);
        this.currentChatModel = currentChatModel;
        this.items = items;
        this.choices = choices;
    }

    @Override
    protected void processTask() throws Exception {
        if (null != progressIndicator) {
            progressIndicator.setFadeTimeMS(250);
            progressIndicator.setLabelLater("Requesting Auto-choose Captions...");
            progressIndicator.spin(true);
            progressIndicator.fadeBusy(false);
        }
        final double total = items.size();
        int currentIndex = 0;
        for (EmbeddingsImageListItem item : items) {
            EmbeddingsImageUrl url = imageUrlFromImage.apply(item.getCurrentImage());
            try {
                ChatCompletionsInput input = ChatCompletionsInput.defaultImageInput(
                    url.getImage_url(), ChatCompletionsInput.CAPTION_TYPE.AUTOCHOOOSE);
                String choosePrompt = input.getMessages().get(0).getContent().get(0).getText();
                choosePrompt = Prompts.insertAutochooseChoices(choosePrompt, choices);
                input.getMessages().get(0).getContent().get(0).setText(choosePrompt);
                if (null != currentChatModel)
                    input.setModel(currentChatModel);
                RestAccessLayer.requestChatCompletion(input, scene,
                    item.imageID, requestNumber.getAndIncrement());
            } catch (JsonProcessingException ex) {
                LOG.error(null, ex);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
            currentIndex++;
            double completed = Integer.valueOf(currentIndex).doubleValue();
            if (null != progressIndicator) {
                progressIndicator.setPercentComplete(completed / total);
                progressIndicator.setLabelLater("Requested " + completed + " of " + total);
            }
            Thread.sleep(getRequestDelay());
        }
        if (null != progressIndicator) {
            progressIndicator.setLabelLater("Complete");
            progressIndicator.spin(false);
            progressIndicator.fadeBusy(true);
        }
    }
}
