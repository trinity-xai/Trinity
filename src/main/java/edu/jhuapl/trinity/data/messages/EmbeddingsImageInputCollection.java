package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingsImageInputCollection {

    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*{[
        {
          "input": "string",
          "model": "intfloat/multilingual-e5-large",
          "encoding_format": "float",
          "embedding_type": "all",
          "dimensions": 512,
          "user": "string"
        }
      ]}
     */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<EmbeddingsImageInput> inputs;
    //</editor-fold>

    public EmbeddingsImageInputCollection() {
        inputs = new ArrayList<>();
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">
    /**
     * @return the inputs
     */
    public List<EmbeddingsImageInput> getInputs() {
        return inputs;
    }

    /**
     * @param inputs the inputs to set
     */
    public void setInputs(List<EmbeddingsImageInput> inputs) {
        this.inputs = inputs;
    }
    //</editor-fold>    
}
