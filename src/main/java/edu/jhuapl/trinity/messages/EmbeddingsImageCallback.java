package edu.jhuapl.trinity.messages;

import static edu.jhuapl.trinity.utils.MessageUtils.embeddingsToFeatureVector;
import edu.jhuapl.trinity.data.messages.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import java.io.File;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Scene;

/**
 *
 * @author Sean Phillips
 */
public class EmbeddingsImageCallback extends RestCallback {
    List<File> inputFiles;
    
    public EmbeddingsImageCallback(List<File> inputFiles, Scene scene) {
        super(scene);
        this.inputFiles = inputFiles;
    }

    @Override
    protected void processResponse(String responseBodyString) throws Exception {
        System.out.println("EmbeddingsImageCallback response...");
        EmbeddingsImageOutput output = objectMapper.readValue(responseBodyString, EmbeddingsImageOutput.class);
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
