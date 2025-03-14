package edu.jhuapl.trinity.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.jhuapl.trinity.data.messages.llm.ChatCaptionResponse;
import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageBatchInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageInput;
import edu.jhuapl.trinity.data.messages.llm.RestAccessLayerConfig;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sean Phillips
 */
public enum RestAccessLayer {
    INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(RestAccessLayer.class);
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
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
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
            LOG.error(ex.getMessage());
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
    public static void requestTextEmbeddings(EmbeddingsImageInput input, 
        Scene scene, List<Integer> inputIDs, int requestNumber) throws JsonProcessingException {
        if(!checkRestServiceInitialized()) {
            notifyTerminalError(
                "REST Request Error: Current REST URL or End Point not initialized properly.", 
                scene
            );            
        }
        String inputJSON = objectMapper.writeValueAsString(input);
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(input));
        Request request = makePostRequest(inputJSON, restAccessLayerconfig.getImageEmbeddingsEndpoint());
        client.newCall(request).enqueue(new EmbeddingsTextCallback(scene, inputIDs, requestNumber));
    }
    public static void requestImageEmbeddings(EmbeddingsImageBatchInput input, 
        Scene scene, List<Integer> inputIDs, int requestNumber) throws JsonProcessingException {
        if(!checkRestServiceInitialized()) {
            notifyTerminalError(
                "REST Request Error: Current REST URL or End Point not initialized properly.", 
                scene
            );            
        }
        String inputJSON = objectMapper.writeValueAsString(input);
        Request request = makePostRequest(inputJSON, restAccessLayerconfig.getImageEmbeddingsEndpoint());
        client.newCall(request).enqueue(new EmbeddingsImageCallback(scene, inputIDs, requestNumber));
    }
    public static void requestChatCompletion(ChatCompletionsInput input, Scene scene, int inputID, int requestNumber) throws JsonProcessingException {
        if(!checkRestServiceInitialized()) {
            notifyTerminalError(
                "REST Request Error: Current REST URL or End Point not initialized properly.", 
                scene
            );            
        }
        String inputJSON = objectMapper.writeValueAsString(input);
//        System.out.println("Pretty Print of ChatCompletionsInput: \n" 
//            + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(input));
        Request request = makePostRequest(inputJSON, restAccessLayerconfig.getChatCompletionEndpoint());
        client.newCall(request).enqueue(new ChatCompletionCallback(scene, inputID, requestNumber));
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
    public static Function <String, ChatCaptionResponse> stringToChatCaptionResponse = rawText -> {
        ChatCaptionResponse response = null;
        //attempts to naively extract out a json object based on enclosing brackets
        int startIndex = rawText.indexOf("{");
        int endIndex = rawText.lastIndexOf("}");
        if(startIndex >= 0 && endIndex > startIndex) {
            try {
                response = objectMapper.readValue(
                    rawText.substring(startIndex, endIndex+1), ChatCaptionResponse.class);
            } catch (JsonProcessingException ex) {
                LOG.error(null, ex);
            }
        }
        return response;
    };
    
}
