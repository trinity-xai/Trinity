package edu.jhuapl.trinity.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.javafx.events.ErrorEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Scene;

/**
 *
 * @author Sean Phillips
 */
public class EmbeddingsImageCallback extends RestConsumer {
    List<Integer> inputIDs;
    int requestNumber;
    
    public enum STATUS { REQUESTED, SUCCEEDED, FAILED };
    
    public EmbeddingsImageCallback(Scene scene, List<Integer> inputIDs, int requestNumber) {
        super(scene);
        this.inputIDs = inputIDs;
        this.requestNumber = requestNumber;
    }

    @Override
    public void onFailure() {
        LOG.error("REST RequestNumber: " + requestNumber + " returned with HTTP failure code.");
        ErrorEvent error = new ErrorEvent(ErrorEvent.REST_ERROR, getClass().getName() + " has failed.");
        scene.getRoot().fireEvent(error);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.ERROR_EMBEDDINGS_IMAGE, inputIDs, requestNumber));
        });  
    }

    @Override
    protected void processResponse(String responseBodyString) {
        //@DEBUG SMP useful for figuring stuff out
        //System.out.println("EmbeddingsImageCallback response...\n" + responseBodyString);
        EmbeddingsImageOutput output;
        try {
            output = objectMapper.readValue(responseBodyString, EmbeddingsImageOutput.class);
            output.setRequestNumber(requestNumber);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new RestEvent(RestEvent.NEW_EMBEDDINGS_IMAGE, output, inputIDs));
            });  
        } catch (JsonProcessingException ex) {
            Logger.getLogger(EmbeddingsImageCallback.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
