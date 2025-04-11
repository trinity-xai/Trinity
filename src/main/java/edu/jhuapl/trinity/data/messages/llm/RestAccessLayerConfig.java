package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestAccessLayerConfig extends MessageData {

    public static final String TYPESTRING = "RestAccessLayerConfig";
    public static final String DEFAULT_IMAGE_MODEL = "openai/clip-vit-large-patch14";

    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        {
          "messageType" : "RestAccessLayerConfig",
          "messageId" : 0,
          "baseRestURL" : "https://my.multimodal.model.org",
          "isAliveEndpoint" : "/embeddings/alive/",
          "imageEmbeddingsEndpoint" : "/embeddings/",
          "defaultImageModel" : "opensourceorg/embedding-model-name-version",

          "chatModelsEndpoint" : "/chat/models",
          "chatCompletionsEndpoint" : "/chat/completions",
          "defaultCaptionModel" : "opensourceorg/opensourcemodel-versionnumber-Vision-Instruct",

          "notes" : "My happy funtime model that I host"
        }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String baseRestURL = null;
    private String isAliveEndpoint = null;
    private String imageEmbeddingsEndpoint = null;
    private String defaultImageModel = DEFAULT_IMAGE_MODEL;
    private String chatModelsEndpoint;
    private String chatCompletionEndpoint;
    private String defaultCaptionModel;
    private String notes = null;
    //</editor-fold>

    public RestAccessLayerConfig() {
        this.messageType = TYPESTRING;
    }

    public static boolean isRestAccessLayerConfig(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING);
    }

    public static RestAccessLayerConfig getDefault() {
        RestAccessLayerConfig config = new RestAccessLayerConfig();
        config.setBaseRestURL("https://localhost");
        config.setIsAliveEndpoint("/v2/embeddings/alive");
        config.setImageEmbeddingsEndpoint("/v2/embeddings");
        config.setDefaultImageModel("openai/clip-vit-large-patch14");
        config.setChatModelsEndpoint("/v2/models/alive");
        config.setChatCompletionEndpoint("/v2/chat/completions");
        config.setDefaultCaptionModel("meta-llama/Llama-3.2-90B-Vision-Instruct");
        config.setNotes("Default coded settings used at initialization");

        return config;
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the baseRestURL
     */
    public String getBaseRestURL() {
        return baseRestURL;
    }

    /**
     * @param baseRestURL the baseRestURL to set
     */
    public void setBaseRestURL(String baseRestURL) {
        this.baseRestURL = baseRestURL;
    }

    /**
     * @return the isAliveEndpoint
     */
    public String getIsAliveEndpoint() {
        return isAliveEndpoint;
    }

    /**
     * @param isAliveEndpoint the isAliveEndpoint to set
     */
    public void setIsAliveEndpoint(String isAliveEndpoint) {
        this.isAliveEndpoint = isAliveEndpoint;
    }

    /**
     * @return the imageEmbeddingsEndpoint
     */
    public String getImageEmbeddingsEndpoint() {
        return imageEmbeddingsEndpoint;
    }

    /**
     * @param imageEmbeddingsEndpoint the imageEmbeddingsEndpoint to set
     */
    public void setImageEmbeddingsEndpoint(String imageEmbeddingsEndpoint) {
        this.imageEmbeddingsEndpoint = imageEmbeddingsEndpoint;
    }

    /**
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @param notes the notes to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * @return the defaultImageModel
     */
    public String getDefaultImageModel() {
        return defaultImageModel;
    }

    /**
     * @param defaultImageModel the defaultImageModel to set
     */
    public void setDefaultImageModel(String defaultImageModel) {
        this.defaultImageModel = defaultImageModel;
    }

    /**
     * @return the chatModelsEndpoint
     */
    public String getChatModelsEndpoint() {
        return chatModelsEndpoint;
    }

    /**
     * @param chatModelsEndpoint the chatModelsEndpoint to set
     */
    public void setChatModelsEndpoint(String chatModelsEndpoint) {
        this.chatModelsEndpoint = chatModelsEndpoint;
    }

    /**
     * @return the chatCompletionEndpoint
     */
    public String getChatCompletionEndpoint() {
        return chatCompletionEndpoint;
    }

    /**
     * @param chatCompletionEndpoint the chatCompletionEndpoint to set
     */
    public void setChatCompletionEndpoint(String chatCompletionEndpoint) {
        this.chatCompletionEndpoint = chatCompletionEndpoint;
    }

    /**
     * @return the defaultCaptionModel
     */
    public String getDefaultCaptionModel() {
        return defaultCaptionModel;
    }

    /**
     * @param defaultCaptionModel the defaultCaptionModel to set
     */
    public void setDefaultCaptionModel(String defaultCaptionModel) {
        this.defaultCaptionModel = defaultCaptionModel;
    }

    //</editor-fold>
}
