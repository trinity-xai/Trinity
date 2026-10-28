package edu.jhuapl.trinity.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsOutput;
import edu.jhuapl.trinity.javafx.events.ErrorEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import javafx.application.Platform;
import javafx.scene.Scene;

/**
 * @author Sean Phillips
 */
public class ChatCompletionCallback extends RestConsumer {
    int requestNumber;
    int inputID;

    public enum STATUS {REQUESTED, SUCCEEDED, FAILED}

    public ChatCompletionCallback(Scene scene, int inputID, int requestNumber) {
        super(scene);
        this.requestNumber = requestNumber;
        this.inputID = inputID;
    }

    @Override
    public void onFailure() {
        String message = "Chat completion request " + requestNumber
            + " failed (HTTP or transport error).";
        LOG.error(message);
        reportFailure(message);
    }

    private void reportFailure(String message) {
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new ErrorEvent(ErrorEvent.REST_ERROR, message));
            scene.getRoot().fireEvent(new RestEvent(RestEvent.ERROR_CHAT_COMPLETIONS, requestNumber, inputID));
        });
    }

    @Override
    protected void processResponse(String responseBodyString) {
//        System.out.println("Pretty Print of ChatCompletionCallback response... \n"
//            + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseBodyString));
        ChatCompletionsOutput output;
        try {
            output = objectMapper.readValue(responseBodyString, ChatCompletionsOutput.class);
            output.setInputID(inputID);
            output.setRequestNumber(requestNumber);

            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new RestEvent(RestEvent.NEW_CHAT_COMPLETION, output));
            });
        } catch (JsonProcessingException ex) {
            LOG.error("Cannot deserialize chat completion response for request {}", requestNumber, ex);

            // TEMPORARY DIAGNOSTIC: Log the unmodified JSON response
            LOG.error("Raw Chat Completion JSON for request {}:\n{}",
                requestNumber, responseBodyString);

            reportFailure("Chat completion response could not be deserialized for request "
                + requestNumber + ": " + ex.getOriginalMessage());
        }
    }
}
