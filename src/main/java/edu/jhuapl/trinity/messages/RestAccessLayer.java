package edu.jhuapl.trinity.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.EmbeddingsImageBatchInput;
import edu.jhuapl.trinity.data.messages.RestAccessLayerConfig;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 *
 * @author Sean Phillips
 */
public enum RestAccessLayer {
    INSTANCE;
    private static OkHttpClient client;
    private static ObjectMapper objectMapper;
    public static final String SERVICES_DEFAULT_PATH = "services/"; //default to local relative path
    public static final String SERVICES_DEFAULT_CONFIG = "defaultRestAccessLayer.json";
    public static RestAccessLayerConfig restAccessLayerconfig = null;
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;
      
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        client = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionPool(new ConnectionPool(50, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .build();
        try {
            restAccessLayerconfig = loadDefaultRestConfig();
        } catch (IOException ex) {
            Logger.getLogger(RestAccessLayer.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error attempting to find and load REST Services Config: " 
                + SERVICES_DEFAULT_PATH + SERVICES_DEFAULT_CONFIG);
        }
    }

    public static RestAccessLayerConfig loadDefaultRestConfig() throws IOException {
        File defaultConfigFile = new File(SERVICES_DEFAULT_PATH + SERVICES_DEFAULT_CONFIG);
        if (!defaultConfigFile.exists() || !defaultConfigFile.canRead()) {
            return null;
        }
        String message = Files.readString(defaultConfigFile.toPath());
        RestAccessLayerConfig config = objectMapper.readValue(message, RestAccessLayerConfig.class);
        return config;
    }    
    
    //Event and Image REST calls
    public static void requestImageEmbeddings(List<File> inputFiles, 
        EmbeddingsImageBatchInput input, Scene scene, int requestNumber) throws JsonProcessingException {
        if(!checkRestServiceInitialized()) {
            notifyTerminalError(
                "REST Request Error: Current REST URL or End Point not initialized properly.", 
                scene
            );            
        }
        String inputJSON = objectMapper.writeValueAsString(input);
        Request request = makePostRequest(inputJSON, restAccessLayerconfig.getImageEmbeddingsEndpoint());
        client.newCall(request).enqueue(new EmbeddingsImageCallback(inputFiles, scene, requestNumber));
    }
    public static void requestChatCompletion(List<File> inputFiles, 
        EmbeddingsImageBatchInput input, Scene scene, int requestNumber) throws JsonProcessingException {
        if(!checkRestServiceInitialized()) {
            notifyTerminalError(
                "REST Request Error: Current REST URL or End Point not initialized properly.", 
                scene
            );            
        }
        String inputJSON = objectMapper.writeValueAsString(input);
        Request request = makePostRequest(inputJSON, restAccessLayerconfig.getImageEmbeddingsEndpoint());
        client.newCall(request).enqueue(new EmbeddingsImageCallback(inputFiles, scene, requestNumber));
    }
    
    //Utilities
    public static void requestChatModels(Scene scene) {
        HttpUrl url = HttpUrl.parse(restAccessLayerconfig.getBaseRestURL() 
                + restAccessLayerconfig.getChatModelsEndpoint())
            .newBuilder()
            .build();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new ChatModelsAliveCallback(scene));
    }
    
    public static void requestRestIsAlive(Scene scene) {
        HttpUrl url = HttpUrl.parse(restAccessLayerconfig.getBaseRestURL() 
                + restAccessLayerconfig.getIsAliveEndpoint())
            .newBuilder()
            .build();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new IsAliveCallback(scene));
    }
    private static Request makePostRequest(String payload, String endPoint) {
        HttpUrl url = HttpUrl.parse(restAccessLayerconfig.getBaseRestURL() + endPoint)
            .newBuilder()
            .build();
        RequestBody hardBody = RequestBody.create(payload, JSON);
        //  -H 'accept: application/json' \
        //  -H 'Content-Type: application/json'
        return new Request.Builder()
            .addHeader("accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .url(url)
            .post(hardBody)
            .build();        
    }    
    private static void notifyTerminalError(String message, Scene scene) {
        Platform.runLater(() -> {
            CommandTerminalEvent cte = new CommandTerminalEvent(
                message, new Font("Consolas", 20), Color.RED);
            scene.getRoot().fireEvent(cte);
        });            
    }
    private static boolean checkRestServiceInitialized() {
        return null != restAccessLayerconfig || null != restAccessLayerconfig.getBaseRestURL();
    }
}
