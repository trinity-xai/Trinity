package edu.jhuapl.trinity.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.javafx.events.ErrorEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Scene;

/**
 *
 * @author Sean Phillips
 */
public class EmbeddingsTextLandmarkCallback extends RestConsumer {
    List<Integer> inputIDs;
    int requestNumber;
    
    public enum STATUS { REQUESTED, SUCCEEDED, FAILED };
    
    public EmbeddingsTextLandmarkCallback(Scene scene, List<Integer> inputIDs, int requestNumber) {
        super(scene);
        this.inputIDs = inputIDs;
        this.requestNumber = requestNumber;
    }

    @Override
    public void onFailure() {
//        LOG.error(e.getMessage());
        ErrorEvent error = new ErrorEvent(ErrorEvent.REST_ERROR, getClass().getName() + " has failed.");
        scene.getRoot().fireEvent(error);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.ERROR_EMBEDDINGS_TEXT, inputIDs, requestNumber));
        });  
    }

    @Override
    protected void processResponse(String responseBodyString) {
        try {
            //System.out.println("EmbeddingsImageCallback response...");
            EmbeddingsImageOutput output = objectMapper.readValue(responseBodyString, EmbeddingsImageOutput.class);
            output.setRequestNumber(requestNumber);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new RestEvent(RestEvent.NEW_EMBEDDINGS_LANDMARKTEXT, output, inputIDs));  
            });
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage());
        }
    }
}
