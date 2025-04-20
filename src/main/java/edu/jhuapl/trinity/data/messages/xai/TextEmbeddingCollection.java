package edu.jhuapl.trinity.data.messages.xai;

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
public class TextEmbeddingCollection extends MessageData {

    public static final String TYPESTRING = "TextEmbeddingCollection";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {   "type": "TextEmbeddingCollection",
        "label" : "chatGPT",
        "score" : 0.5,
        "text_embeddings": [
            ...boat load of TextEmbeddingSet objects
        ]
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<TextEmbeddingSet> text_embeddings;
    private String type;
    private String label;
    private Double score;
    //</editor-fold>

    public TextEmbeddingCollection() {
        type = TYPESTRING;
        text_embeddings = new ArrayList<>();
    }

    public static boolean isTextEmbeddingCollection(String messageBody) {
        return messageBody.contains("type")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("text_embeddings");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

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

    /**
     * @return the text_embeddings
     */
    public List<TextEmbeddingSet> getText_embeddings() {
        return text_embeddings;
    }

    /**
     * @param text_embeddings the text_embeddings to set
     */
    public void setText_embeddings(List<TextEmbeddingSet> text_embeddings) {
        this.text_embeddings = text_embeddings;
    }
    //</editor-fold>

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the score
     */
    public Double getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(Double score) {
        this.score = score;
    }
}
