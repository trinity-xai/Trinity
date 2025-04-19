package edu.jhuapl.trinity.data.messages.bci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticData extends MessageData {

    public static final String TYPESTRING = "semantic_data";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        "noun": {
            "dims": ["noun"],
            "attrs": {},
            "data": ["bear", "cat", "cow", "dog", "horse", "arm", "eye", "foot", "hand", "leg", "apartment", "barn", "church", "house", "igloo", "arch", "chimney", "closet", "door", "window", "coat", "dress", "pants", "shirt", "skirt", "bed", "chair", "desk", "dresser", "table", "ant", "bee", "beetle", "butterfly", "fly", "bottle", "cup", "glass", "knife", "spoon", "bell", "key", "refrigerator", "telephone", "watch", "chisel", "hammer", "pliers", "saw", "screwdriver", "carrot", "celery", "corn", "lettuce", "tomato", "airplane", "bicycle", "car", "train", "truck"]
        }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<String> dims;
    private Map<String, Double> attrs;
    private List<String> data;

    //</editor-fold>

    public SemanticData() {
        this.messageType = TYPESTRING;
        this.dims = new ArrayList<>();
        attrs = new HashMap<>();
        this.data = new ArrayList<>();
    }

    public static boolean isFeatureVector(String messageBody) {
        return messageBody.contains("dims")
            && messageBody.contains("attrs") && messageBody.contains("data");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the dims
     */
    public List<String> getDims() {
        return dims;
    }

    /**
     * @param dims the dims to set
     */
    public void setDims(List<String> dims) {
        this.dims = dims;
    }

    /**
     * @return the attrs
     */
    public Map<String, Double> getAttrs() {
        return attrs;
    }

    /**
     * @param attrs the attrs to set
     */
    public void setAttrs(Map<String, Double> attrs) {
        this.attrs = attrs;
    }

    /**
     * @return the data
     */
    public List<String> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(List<String> data) {
        this.data = data;
    }
    //</editor-fold>
}
