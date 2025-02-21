package edu.jhuapl.trinity.messages;

import edu.jhuapl.trinity.data.messages.EmbeddingsImageOutput;
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
        EmbeddingsImageOutput eventResponse = objectMapper.readValue(responseBodyString, EmbeddingsImageOutput.class);
        System.out.println("Response mapped to object...");
    }
}
