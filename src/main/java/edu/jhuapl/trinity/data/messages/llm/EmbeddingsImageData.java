package edu.jhuapl.trinity.data.messages.llm;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author phillsm1
 */
public class EmbeddingsImageData {
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

    private String object;
    private List<Double> embedding;
    private int index;
    private String type;
    
    public EmbeddingsImageData() {
        embedding = new ArrayList<>();
    }

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
     * @return the embedding
     */
    public List<Double> getEmbedding() {
        return embedding;
    }

    /**
     * @param embedding the embedding to set
     */
    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }
}
