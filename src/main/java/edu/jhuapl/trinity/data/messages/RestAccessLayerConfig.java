package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

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
          "baseRestURL" : "https://some.url.org",
          "isAliveEndpoint" : "/alive/",
          "imageEmbeddingsEndpoint" : "/embeddings/image/",
          "notes" : "My favorite multimodal services",
        }
     */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String baseRestURL = null;
    private String isAliveEndpoint = null;
    private String imageEmbeddingsEndpoint = null;
    private String notes = null;
    private String defaultImageModel = "openai/clip-vit-large-patch14";
    //</editor-fold>

    public RestAccessLayerConfig() {
        this.messageType = TYPESTRING;
    }

    public static boolean isEmbeddingsImageInput(String messageBody) {
        return messageBody.contains("messageType")
                && messageBody.contains(TYPESTRING);
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
    
    //</editor-fold>    

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
    
}
