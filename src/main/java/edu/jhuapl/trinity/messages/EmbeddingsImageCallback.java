package edu.jhuapl.trinity.messages;

import static edu.jhuapl.trinity.utils.MessageUtils.embeddingsToFeatureVector;
import edu.jhuapl.trinity.data.messages.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.events.ErrorEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import java.io.File;
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
public class EmbeddingsImageCallback extends RestCallback {
    List<File> inputFiles;
    int requestNumber;
    
    public enum STATUS { REQUESTED, SUCCEEDED, FAILED };
    
    public EmbeddingsImageCallback(List<File> inputFiles, Scene scene, int requestNumber) {
        super(scene);
        this.inputFiles = inputFiles;
        this.requestNumber = requestNumber;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Logger.getLogger(RestCallback.class.getName()).log(Level.SEVERE, null, e);
        ErrorEvent error = new ErrorEvent(ErrorEvent.REST_ERROR, getClass().getName() + " has failed.");
        scene.getRoot().fireEvent(error);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.ERROR_EMBEDDINGS_IMAGE, inputFiles, requestNumber));
        });  
    }

    @Override
    protected void processResponse(String responseBodyString) throws Exception {
        System.out.println("EmbeddingsImageCallback response...");
        EmbeddingsImageOutput output = objectMapper.readValue(responseBodyString, EmbeddingsImageOutput.class);
        output.setRequestNumber(requestNumber);
        List<FeatureVector> fvList = output.getData().stream()
             .map(embeddingsToFeatureVector).toList();
        if(null != inputFiles && inputFiles.size()>=fvList.size()) {
            for(int imageIndex=0; imageIndex<fvList.size();imageIndex++){
                fvList.get(imageIndex).setImageURL(
                    inputFiles.get(imageIndex).getAbsolutePath());
            }
        }
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.NEW_EMBEDDINGS_IMAGE, output, fvList));
        });  
    }
}
