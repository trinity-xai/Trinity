package edu.jhuapl.trinity.messages;

import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsOutput;
import edu.jhuapl.trinity.javafx.events.ErrorEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import java.io.IOException;
import javafx.application.Platform;
import javafx.scene.Scene;
import okhttp3.Call;

/**
 *
 * @author Sean Phillips
 */
public class ChatCompletionCallback extends RestCallback {
    int requestNumber;
    int inputID;
    
    public enum STATUS { REQUESTED, SUCCEEDED, FAILED };
    
    public ChatCompletionCallback( Scene scene, int inputID, int requestNumber) {
        super(scene);
        this.requestNumber = requestNumber;
        this.inputID = inputID;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        LOG.error(e.getMessage());
        ErrorEvent error = new ErrorEvent(ErrorEvent.REST_ERROR, getClass().getName() + " has failed.");
        scene.getRoot().fireEvent(error);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.ERROR_CHAT_COMPLETIONS, requestNumber, inputID));
        });  
    }

    @Override
    protected void processResponse(String responseBodyString) throws Exception {
//        System.out.println("Pretty Print of ChatCompletionCallback response... \n"
//            + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseBodyString));
        ChatCompletionsOutput output = objectMapper.readValue(responseBodyString, ChatCompletionsOutput.class);
        output.setInputID(inputID);
        output.setRequestNumber(requestNumber);

        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.NEW_CHAT_COMPLETION, output));
        });  
    }
}
