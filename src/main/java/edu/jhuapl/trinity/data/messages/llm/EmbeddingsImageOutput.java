package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingsImageOutput extends MessageData {

    public static final String TYPESTRING = "imgoutput";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {
      "object": "list",
      "data": [
        {
          "object": "embedding",
          "embedding": [
            0
          ],
          "index": 0,
          "type": "text"
        }
      ],
      "model": "string"
    }
     */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String object;
    private String model;
    private List<EmbeddingsImageData> data;
    //</editor-fold>
    //extra helper fields
    private int requestNumber;
    
    public EmbeddingsImageOutput() {
        this.messageType = TYPESTRING;
        data = new ArrayList<>();
    }

    public static boolean isEmbeddingsImageOutput(String messageBody) {
        return messageBody.contains("messageType")
                && messageBody.contains(TYPESTRING);
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the object
     */
    public String getObject() {
        return object;
    }

    /**
     * @param object the object to set
     */
    public void setObject(String object) {
        this.object = object;
    }

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
     * @return the data
     */
    public List<EmbeddingsImageData> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(List<EmbeddingsImageData> data) {
        this.data = data;
    }

    /**
     * @return the requestNumber
     */
    public int getRequestNumber() {
        return requestNumber;
    }

    /**
     * @param requestNumber the requestNumber to set
     */
    public void setRequestNumber(int requestNumber) {
        this.requestNumber = requestNumber;
    }
  
}