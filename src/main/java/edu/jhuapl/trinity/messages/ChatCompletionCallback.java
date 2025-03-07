package edu.jhuapl.trinity.messages;

import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsOutput;
import static edu.jhuapl.trinity.utils.MessageUtils.embeddingsToFeatureVector;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
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
public class ChatCompletionCallback extends RestCallback {
    int requestNumber;
    
    public enum STATUS { REQUESTED, SUCCEEDED, FAILED };
    
    public ChatCompletionCallback( Scene scene, int requestNumber) {
        super(scene);
        this.requestNumber = requestNumber;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Logger.getLogger(RestCallback.class.getName()).log(Level.SEVERE, null, e);
        ErrorEvent error = new ErrorEvent(ErrorEvent.REST_ERROR, getClass().getName() + " has failed.");
        scene.getRoot().fireEvent(error);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.ERROR_CHAT_COMPLETIONS, requestNumber));
        });  
    }

    @Override
    protected void processResponse(String responseBodyString) throws Exception {
//        System.out.println("Pretty Print of ChatCompletionCallback response... \n"
//            + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseBodyString));

        ChatCompletionsOutput output = objectMapper.readValue(responseBodyString, ChatCompletionsOutput.class);
        output.setRequestNumber(requestNumber);
//        List<FeatureVector> fvList = output.getData().stream()
//             .map(embeddingsToFeatureVector).toList();
//        if(null != inputFiles && inputFiles.size()>=fvList.size()) {
//            for(int imageIndex=0; imageIndex<fvList.size();imageIndex++){
//                fvList.get(imageIndex).setImageURL(
//                    inputFiles.get(imageIndex).getAbsolutePath());
//            }
//        }
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.NEW_CHAT_COMPLETION, output));
        });  
    }
}
