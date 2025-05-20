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
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
public enum RestAccessLayer {
    INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(RestAccessLayer.class);
    private static HttpClient httpClient;
    private static ObjectMapper objectMapper;
    //default local relative path obtained as a system property if running from JLink/Jpackage
    public static final String TRINITY_APP_DIR_PROPERTY = "trinity.app.dir";
    //default to local relative path if loading from a Jar/IDE
    public static String SERVICES_DEFAULT_PATH = "services" + File.separator;
    public static String SERVICES_DEFAULT_CONFIG = "defaultRestAccessLayer.json";
    public static RestAccessLayerConfig restAccessLayerconfig = null;
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;
    public static final int SUCCESS_STATUS_CODE = 200;
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String JSON = "application/json";
    public static String currentEmbeddingsModel = null;
    public static String currentChatModel = null;

    static {
        //Load any defaults like services path from system properties
        //These are injected when running from JLink/JPackage type builds
        checkSystemProperties(); //should be called once to ensure we factor these in
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .build();
        try {
            restAccessLayerconfig = loadDefaultRestConfig();
        } catch (IOException ex) {
            LOG.error(null, ex);
            System.out.println("Error attempting to find and load REST Services Config: "
                + SERVICES_DEFAULT_PATH + SERVICES_DEFAULT_CONFIG);
            restAccessLayerconfig = RestAccessLayerConfig.getDefault();
        }
        currentEmbeddingsModel = restAccessLayerconfig.getDefaultImageModel();
        currentChatModel = restAccessLayerconfig.getDefaultCaptionModel();
    }

    private static void checkSystemProperties() {
        //Check if we are running from JLink/JPackage scenario. If so use System property
        try {
            String servicesDir = System.getProperty(TRINITY_APP_DIR_PROPERTY);
            //if it is null then the property isn't there and we assume Jar/IDE
            if (null != servicesDir) {
                //We assume its JLink/JPackage and hope they put in a good directory
                SERVICES_DEFAULT_PATH = servicesDir;
                if (!SERVICES_DEFAULT_PATH.endsWith(File.separator))
                    SERVICES_DEFAULT_PATH = SERVICES_DEFAULT_PATH + File.separator;
            }
        } catch (Exception ex) {
            //We are running from Jar/IDE or something else went wrong, use local relative
            LOG.info(TRINITY_APP_DIR_PROPERTY + " not found as System Property." + System.lineSeparator()
                + "Using current path of " + SERVICES_DEFAULT_PATH);
        }
    }

    public static RestAccessLayerConfig loadDefaultRestConfig() throws IOException {
        if (!SERVICES_DEFAULT_PATH.endsWith(File.separator))
            SERVICES_DEFAULT_PATH = SERVICES_DEFAULT_PATH + File.separator;
        File defaultConfigFile = new File(SERVICES_DEFAULT_PATH + SERVICES_DEFAULT_CONFIG);
        if (!defaultConfigFile.exists() || !defaultConfigFile.canRead()) {
            return restAccessLayerconfig = RestAccessLayerConfig.getDefault();
        }
        String message = Files.readString(defaultConfigFile.toPath());
        RestAccessLayerConfig config = objectMapper.readValue(message, RestAccessLayerConfig.class);
        return restAccessLayerconfig = config;
    }

    //Text and Image REST calls
    public static void requestQueryTextEmbeddings(EmbeddingsImageInput input,
                                                  Scene scene, List<Integer> inputIDs, int requestNumber) throws JsonProcessingException {
        if (restServiceFailed(scene)) return;
        String inputJSON = objectMapper.writeValueAsString(input);
        //@DEBUG SMP
        //System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(input));
        HttpRequest request = makeHttpPostRequest(inputJSON, restAccessLayerconfig.getImageEmbeddingsEndpoint());
        httpClient.sendAsync(request, BodyHandlers.ofString())
            .thenAcceptAsync(resp -> {
                if (resp.statusCode() != SUCCESS_STATUS_CODE) {
                    new EmbeddingsTextQueryCallback(scene, inputIDs, requestNumber).onFailure();
                } else {
                    new EmbeddingsTextQueryCallback(scene, inputIDs, requestNumber).processResponse(resp.body());
                }
            });
    }

    public static void requestTextEmbeddings(EmbeddingsImageInput input,
                                             Scene scene, List<Integer> inputIDs, int requestNumber) throws JsonProcessingException {
        if (restServiceFailed(scene)) return;
        String inputJSON = objectMapper.writeValueAsString(input);
        HttpRequest request = makeHttpPostRequest(inputJSON, restAccessLayerconfig.getImageEmbeddingsEndpoint());
        httpClient.sendAsync(request, BodyHandlers.ofString())
            .thenAcceptAsync(resp -> {
                if (resp.statusCode() != SUCCESS_STATUS_CODE) {
                    new EmbeddingsTextCallback(scene, inputIDs, requestNumber).onFailure();
                } else {
                    new EmbeddingsTextCallback(scene, inputIDs, requestNumber).processResponse(resp.body());
                }
            });
    }

    public static void requestLandmarkTextEmbeddings(EmbeddingsImageInput input,
                                                     Scene scene, List<Integer> inputIDs, int requestNumber) throws JsonProcessingException {
        if (restServiceFailed(scene)) return;
        String inputJSON = objectMapper.writeValueAsString(input);
        HttpRequest request = makeHttpPostRequest(inputJSON, restAccessLayerconfig.getImageEmbeddingsEndpoint());
        httpClient.sendAsync(request, BodyHandlers.ofString())
            .thenAcceptAsync(resp -> {
                if (resp.statusCode() != SUCCESS_STATUS_CODE) {
                    new EmbeddingsTextLandmarkCallback(scene, inputIDs, requestNumber).onFailure();
                } else {
                    new EmbeddingsTextLandmarkCallback(scene, inputIDs, requestNumber).processResponse(resp.body());
                }
            });
    }

    public static void requestImageEmbeddings(EmbeddingsImageBatchInput input,
                                              Scene scene, List<Integer> inputIDs, int requestNumber) throws JsonProcessingException {
        if (restServiceFailed(scene)) return;
        String inputJSON = objectMapper.writeValueAsString(input);
        HttpRequest request = makeHttpPostRequest(inputJSON, restAccessLayerconfig.getImageEmbeddingsEndpoint());
        httpClient.sendAsync(request, BodyHandlers.ofString())
            .thenAcceptAsync(resp -> {
                if (resp.statusCode() != SUCCESS_STATUS_CODE) {
                    new EmbeddingsImageCallback(scene, inputIDs, requestNumber).onFailure();
                } else {
                    new EmbeddingsImageCallback(scene, inputIDs, requestNumber).processResponse(resp.body());
                }
            });
    }

    public static void requestLandmarkImageEmbeddings(EmbeddingsImageBatchInput input,
                                                      Scene scene, List<Integer> inputIDs, int requestNumber) throws JsonProcessingException {
        if (restServiceFailed(scene)) return;
        String inputJSON = objectMapper.writeValueAsString(input);
        HttpRequest request = makeHttpPostRequest(inputJSON, restAccessLayerconfig.getImageEmbeddingsEndpoint());
        httpClient.sendAsync(request, BodyHandlers.ofString())
            .thenAcceptAsync(resp -> {
                if (resp.statusCode() != SUCCESS_STATUS_CODE) {
                    new EmbeddingsImageLandmarkCallback(scene, inputIDs, requestNumber).onFailure();
                } else {
                    new EmbeddingsImageLandmarkCallback(scene, inputIDs, requestNumber).processResponse(resp.body());
                }
            });
    }

    public static void requestChatCompletion(ChatCompletionsInput input, Scene scene, int inputIDs, int requestNumber) throws JsonProcessingException {
        if (restServiceFailed(scene)) return;
        String inputJSON = objectMapper.writeValueAsString(input);
        HttpRequest request = makeHttpPostRequest(inputJSON, restAccessLayerconfig.getChatCompletionEndpoint());
        httpClient.sendAsync(request, BodyHandlers.ofString())
            .thenAcceptAsync(resp -> {
                if (resp.statusCode() != SUCCESS_STATUS_CODE) {
                    new ChatCompletionCallback(scene, inputIDs, requestNumber).onFailure();
                } else {
                    new ChatCompletionCallback(scene, inputIDs, requestNumber).processResponse(resp.body());
                }
            });
    }

    //Utilities
    public static void requestChatModels(Scene scene) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(restAccessLayerconfig.getBaseRestURL()
                + restAccessLayerconfig.getChatModelsEndpoint()))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .header(CONTENT_TYPE, JSON)
            .GET()
            .build();
        httpClient.sendAsync(request, BodyHandlers.ofString())
            .thenAcceptAsync(resp -> {
                if (resp.statusCode() != SUCCESS_STATUS_CODE) {
                    new ChatModelsAliveCallback(scene).onFailure();
                } else {
                    new ChatModelsAliveCallback(scene).processResponse(resp.body());
                }
            });
    }

    public static void requestRestIsAlive(Scene scene) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(restAccessLayerconfig.getBaseRestURL()
                + restAccessLayerconfig.getIsAliveEndpoint()))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .header(CONTENT_TYPE, JSON)
            .GET()
            .build();
        httpClient.sendAsync(request, BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenAccept(new IsAliveCallback(scene)::processResponse);
    }

    private static HttpRequest makeHttpPostRequest(String payload, String endPoint) {
        return HttpRequest.newBuilder()
            .uri(URI.create(restAccessLayerconfig.getBaseRestURL() + endPoint))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .header(CONTENT_TYPE, JSON)
            .POST(BodyPublishers.ofString(payload))
            .build();
    }

    private static boolean restServiceFailed(Scene scene) {
        if (!checkRestServiceInitialized()) {
            notifyTerminalError(
                "REST Request Error: Current REST URL or End Point not initialized properly.",
                scene
            );
            return true;
        }
        return false;
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

    public static Function<String, ChatCaptionResponse> stringToChatCaptionResponse = rawText -> {
        ChatCaptionResponse response = null;
        //attempts to naively extract out a json object based on enclosing brackets
        int startIndex = rawText.indexOf("{");
        int endIndex = rawText.lastIndexOf("}");
        if (startIndex >= 0 && endIndex > startIndex) {
            try {
                response = objectMapper.readValue(
                    rawText.substring(startIndex, endIndex + 1), ChatCaptionResponse.class);
            } catch (JsonProcessingException ex) {
                LOG.error(null, ex);
            }
        }
        return response;
    };

}
