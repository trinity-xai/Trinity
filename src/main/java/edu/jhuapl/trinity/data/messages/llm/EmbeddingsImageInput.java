package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingsImageInput {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddingsImageInput.class);
    public static final String TYPESTRING = "embeddingImageInput";
    public static final String BASE64_PREFIX_PNG = "data:image/png;base64,";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        {
          "input": "string",
          "model": "intfloat/multilingual-e5-large",
          "encoding_format": "float",
          "embedding_type": "all",
          "dimensions": 512,
          "user": "string"
        }

{
  "input": [
    {"type":"image_url","image_url": {"url": "data:image/png;base64,image1bytesencoded64"}},
    {"type":"image_url","image_url": {"url": "data:image/jpeg;base64,image2bytesencoded64"}},
    {"type":"image_url","image_url": {"url": "data:image/webp;base64,image3bytesencoded64"}}
  ],
  "model": "intfloat/multilingual-e5-large",
  "encoding_format": "float"
}

     */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String model;
    private String input; //base64 image to get embeddings for
    private String encoding_format;
    private String embedding_type;
    private int dimensions;
    private String user;
    //</editor-fold>

    public EmbeddingsImageInput() {
    }

    public static EmbeddingsImageInput hellocarlTextEmbeddingsImageInput() throws IOException {
        return defaultTextInput("carl-b");
    }

    public static EmbeddingsImageInput defaultTextInput(String text) {
        EmbeddingsImageInput input = new EmbeddingsImageInput();
        input.setInput(text);
        input.setDimensions(512);
        input.setEmbedding_type("all");
        input.setEncoding_format("float");
        input.setModel("meta-llama/Llama-3.2-90B-Vision-Instruct");
        input.setUser("string");
        return input;
    }

    public static Function<File, EmbeddingsImageInput> inputFromFile = file -> {
        EmbeddingsImageInput input = new EmbeddingsImageInput();
        try {
            Image image = ResourceUtils.loadImageFile(file);
            input.setInput(EmbeddingsImageInput.BASE64_PREFIX_PNG
                + ResourceUtils.imageToBase64(image));
            input.setDimensions(Double.valueOf(image.getWidth()).intValue());
            input.setEmbedding_type("all");
            input.setEncoding_format("float");
            input.setModel("openai/clip-vit-large-patch14");
            input.setUser("string");
        } catch (JsonProcessingException ex) {
            LOG.error(null, ex);
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
        return input;
    };

    public static boolean isEmbeddingsImageInput(String messageBody) {
        return messageBody.contains("input") && messageBody.contains("model")
            && messageBody.contains("encoding_format") && messageBody.contains("embedding_type");
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
     * @return the input
     */
    public String getInput() {
        return input;
    }

    /**
     * @param input the input to set
     */
    public void setInput(String input) {
        this.input = input;
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

    //</editor-fold>
}
