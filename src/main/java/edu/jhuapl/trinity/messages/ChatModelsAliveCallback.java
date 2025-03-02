package edu.jhuapl.trinity.messages;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.llm.AliveModels;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.ErrorEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.StageStyle;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 *
 * @author Sean Phillips
 */
public class ChatModelsAliveCallback extends Task implements Callback {

    Response response;
    ObjectMapper objectMapper;
    Scene scene;

    public ChatModelsAliveCallback(Scene scene) {
        this.scene = scene;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Logger.getLogger(ChatModelsAliveCallback.class.getName()).log(Level.SEVERE, null, e);
        ErrorEvent error = new ErrorEvent(ErrorEvent.REST_ERROR,"The IsAlive callback has failed.");
        SwingNode errorNode = new SwingNode();
        errorNode.fireEvent(error);

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
            if (!response.isSuccessful()) {
                Platform.runLater(() -> {
                    CommandTerminalEvent cte = new CommandTerminalEvent(
                    "Chat Model service could not be reached: " + response.code() + " - " + response.message(), 
                        new Font("Consolas", 20), Color.RED);
                        scene.getRoot().fireEvent(cte);
                    Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Functionality will be limited without Chat Model access.", ButtonType.OK );
                    alert.setTitle("Embedding Service Failure");
                    alert.setHeaderText("Is Alive check for LLM Service failed");
                    alert.setGraphic(ResourceUtils.loadIcon("alert", 75));
                    alert.initStyle(StageStyle.TRANSPARENT);
                    DialogPane dialogPane = alert.getDialogPane();
                    dialogPane.setBackground(Background.EMPTY);
                    dialogPane.getScene().setFill(Color.TRANSPARENT);
                    String DIALOGCSS = ChatModelsAliveCallback.class.getResource(
                        "/edu/jhuapl/trinity/css/dialogstyles.css").toExternalForm();
                    dialogPane.getStylesheets().add(DIALOGCSS);
                    alert.showAndWait();
                });                
                
                throw new IOException("Unexpected code " + response);
            }
            String responseBodyString = response.body().string();
            try {
                System.out.println("Response from 'ChatModelsAlive' check: " + responseBodyString);
                AliveModels chatModelsAliveReesponse = objectMapper.readValue(responseBodyString, AliveModels.class);
                Platform.runLater(() -> {
                    //Let folks know that the chat model service is up
                    CommandTerminalEvent cte = new CommandTerminalEvent(
                    "Chat Model Service is alive.", 
                        new Font("Consolas", 20), Color.LIME);
                    scene.getRoot().fireEvent(cte);
                    //let folks know what chat models are available
                    scene.getRoot().fireEvent(
                        new RestEvent(RestEvent.CHAT_MODELS_ALIVE, chatModelsAliveReesponse));
                });  
            } catch (Exception ex) {
                Logger.getLogger(ChatModelsAliveCallback.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}
