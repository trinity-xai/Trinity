package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingsImageBatchInput {

    public static final String TYPESTRING = "embeddingImageInput";
    public static final String BASE64_PREFIX_PNG = "data:image/png;base64,";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        {
          "input": [
                {"type":"image_url","image_url": {"url": "data:image/png;base64,image1bytesencoded64"}},
                {"type":"image_url","image_url": {"url": "data:image/jpeg;base64,image2bytesencoded64"}},
                {"type":"image_url","image_url": {"url": "data:image/webp;base64,image3bytesencoded64"}}
              ]
          "model": "intfloat/multilingual-e5-large",
          "encoding_format": "float",
          "embedding_type": "all",
          "dimensions": 512,
          "user": "string"
        }
     */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String model;
    private List<EmbeddingsImageUrl> input;
    private String encoding_format;
    private String embedding_type;
    private int dimensions;
    private String user;
    //</editor-fold>

    public EmbeddingsImageBatchInput() {
        input = new ArrayList<>();
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel(String model) {
        this.model = model;
    }


    /**
     * @return the encoding_format
     */
    public String getEncoding_format() {
        return encoding_format;
    }

    /**
     * @param encoding_format the encoding_format to set
     */
    public void setEncoding_format(String encoding_format) {
        this.encoding_format = encoding_format;
    }

    /**
     * @return the embedding_type
     */
    public String getEmbedding_type() {
        return embedding_type;
    }

    /**
     * @param embedding_type the embedding_type to set
     */
    public void setEmbedding_type(String embedding_type) {
        this.embedding_type = embedding_type;
    }

    /**
     * @return the dimensions
     */
    public int getDimensions() {
        return dimensions;
    }

    /**
     * @param dimensions the dimensions to set
     */
    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the input
     */
    public List<EmbeddingsImageUrl> getInput() {
        return input;
    }

    /**
     * @param input the input to set
     */
    public void setInput(List<EmbeddingsImageUrl> input) {
        this.input = input;
    }

    //</editor-fold>

}
