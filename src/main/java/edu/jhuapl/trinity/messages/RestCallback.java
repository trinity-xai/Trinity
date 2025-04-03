package edu.jhuapl.trinity.messages;

/**
 *
 * @author Sean Phillips
 */

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.ErrorEvent;
import java.io.IOException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phillsm1
 */
public abstract class RestCallback extends Task implements Callback {
    static final Logger LOG = LoggerFactory.getLogger(RestCallback.class);
    Response response;
    ObjectMapper objectMapper;
    Scene scene;

    public RestCallback(Scene scene) {
        this.scene = scene;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        LOG.error(e.getMessage());
        ErrorEvent error = new ErrorEvent(ErrorEvent.REST_ERROR, getClass().getName() + " has failed.");
        scene.getRoot().fireEvent(error);
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        //Requesting a response body will be blocking even within an asynch callback
        this.response = response;
        Thread responseThread = new Thread(this);
        responseThread.setDaemon(true);
        responseThread.start();
    }

    @Override
    protected Object call() throws Exception {
        try ( ResponseBody responseBody = response.body()) {
            if(!response.isRedirect()) {
                if (!response.isSuccessful()) {
                    Platform.runLater(() -> {
                        CommandTerminalEvent cte = new CommandTerminalEvent(
                        "REST Request Error: " + response.code() + " - " + response.message(), 
                            new Font("Consolas", 20), Color.RED);
                            scene.getRoot().fireEvent(cte);
                    });                                
                    throw new IOException("Unexpected code " + response);
                }
                String responseBodyString = response.body().string();
                processResponse(responseBodyString);                
            } else {
                System.out.println("Redirected...");
            }
        } 
//        catch (Exception ex) {
//            throw ex;
//        }
        return null;
    }
    protected abstract void processResponse(String responseBodyString) throws Exception;    
}
