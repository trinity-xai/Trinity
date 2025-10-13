package edu.jhuapl.trinity.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.javafx.events.ErrorEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * @author Sean Phillips
 * BatchRequestManager integration.
 * Notifies completion via callback map keyed by requestNumber.
 */
public class EmbeddingsImageCallback extends RestConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddingsImageCallback.class);

    // Accessible map to register completion callbacks for each requestNumber
    public static final Map<Integer, BiConsumer<Boolean, Exception>> completionCallbacks = new ConcurrentHashMap<>();

    private final List<Integer> inputIDs;
    private final int requestNumber;

    public EmbeddingsImageCallback(Scene scene, List<Integer> inputIDs, int requestNumber) {
        super(scene);
        this.inputIDs = inputIDs;
        this.requestNumber = requestNumber;
    }

    @Override
    public void onFailure() {
        LOG.error("REST RequestNumber: {} returned with HTTP failure code.", requestNumber);
        ErrorEvent error = new ErrorEvent(ErrorEvent.REST_ERROR, getClass().getName() + " has failed.");
        scene.getRoot().fireEvent(error);

        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.ERROR_EMBEDDINGS_IMAGE, inputIDs, requestNumber));
            notifyCompletion(false, new Exception("REST HTTP Failure"));
        });
    }

    @Override
    protected void processResponse(String responseBodyString) {
        try {
            EmbeddingsImageOutput output = objectMapper.readValue(responseBodyString, EmbeddingsImageOutput.class);
            output.setRequestNumber(requestNumber);

            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new RestEvent(RestEvent.NEW_EMBEDDINGS_IMAGE, output, inputIDs));
                notifyCompletion(true, null);
            });
        } catch (JsonProcessingException ex) {
            LOG.error("Failed to parse response JSON for request {}.", requestNumber, ex);
            Platform.runLater(() -> {
                notifyCompletion(false, ex);
            });
        }
    }

    /**
     * Utility to retrieve and invoke the completion callback for this request.
     */
    private void notifyCompletion(boolean success, Exception ex) {
        BiConsumer<Boolean, Exception> callback = completionCallbacks.remove(requestNumber);
        if (callback != null) {
            callback.accept(success, ex);
        } else {
            LOG.warn("No completion callback registered for requestNumber {}.", requestNumber);
        }
    }
}
