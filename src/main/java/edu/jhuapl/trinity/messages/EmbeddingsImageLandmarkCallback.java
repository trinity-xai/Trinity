package edu.jhuapl.trinity.messages;

import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.javafx.events.ErrorEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Scene;
import okhttp3.Call;

/**
 *
 * @author Sean Phillips
 */
public class EmbeddingsImageLandmarkCallback extends RestCallback {
    List<Integer> inputIDs;
    int requestNumber;
    
    public enum STATUS { REQUESTED, SUCCEEDED, FAILED };
    
    public EmbeddingsImageLandmarkCallback(Scene scene, List<Integer> inputIDs, int requestNumber) {
        super(scene);
        this.inputIDs = inputIDs;
        this.requestNumber = requestNumber;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        LOG.error(e.getMessage());
        ErrorEvent error = new ErrorEvent(ErrorEvent.REST_ERROR, getClass().getName() + " has failed.");
        scene.getRoot().fireEvent(error);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.ERROR_EMBEDDINGS_IMAGE, inputIDs, requestNumber));
        });  
    }

    @Override
    protected void processResponse(String responseBodyString) throws Exception {
        //System.out.println("EmbeddingsImageCallback response...");
        EmbeddingsImageOutput output = objectMapper.readValue(responseBodyString, EmbeddingsImageOutput.class);
        output.setRequestNumber(requestNumber);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.NEW_EMBEDDINGS_LANDMARKIMAGE, output, inputIDs));
        });  
    }
}
