package edu.jhuapl.trinity.messages;

import edu.jhuapl.trinity.data.messages.EmbeddingsImageOutput;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import javafx.application.Platform;
import javafx.scene.Scene;

/**
 *
 * @author Sean Phillips
 */
public class EmbeddingsImageCallback extends RestCallback {

    public EmbeddingsImageCallback(Scene scene) {
        super(scene);
    }

    @Override
    protected void processResponse(String responseBodyString) throws Exception {
        System.out.println("EmbeddingsImageCallback response...");
//        System.out.println(responseBodyString);
        EmbeddingsImageOutput eventResponse = objectMapper.readValue(responseBodyString, EmbeddingsImageOutput.class);
//        System.out.println("Response mapped to object...");
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new RestEvent(RestEvent.NEW_EMBEDDINGS_IMAGE, eventResponse));
        });  
    }
}
