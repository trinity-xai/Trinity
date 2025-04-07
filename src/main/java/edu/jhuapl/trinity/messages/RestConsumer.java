package edu.jhuapl.trinity.messages;

/**
 *
 * @author Sean Phillips
 */

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phillsm1
 */
public abstract class RestConsumer {
    static final Logger LOG = LoggerFactory.getLogger(RestConsumer.class);
    ObjectMapper objectMapper;
    Scene scene;

    public RestConsumer(Scene scene) {
        this.scene = scene;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    protected abstract void onFailure();    
    protected abstract void processResponse(String responseBodyString);    
}
