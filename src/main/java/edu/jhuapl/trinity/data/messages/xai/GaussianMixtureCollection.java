package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GaussianMixtureCollection {

    public static final String TYPESTRING = "GaussianMixtureCollection";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {   "type": "GaussianMixtureCollection",
        "mixtures": [
            ...boat load of FeatureVector objects
        ]
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<GaussianMixture> mixtures;
    private String type;
    private List<String> axesLabels; //optional field
    //</editor-fold>

    public GaussianMixtureCollection() {
        type = TYPESTRING;
        mixtures = new ArrayList<>();
        axesLabels = new ArrayList<>();
    }

    public static boolean isGaussianMixture(String messageBody) {
        return messageBody.contains("type")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("mixtures");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the mixtures
     */
    public List<GaussianMixture> getMixtures() {
        return mixtures;
    }

    /**
     * @param mixtures the mixtures to set
     */
    public void setMixtures(List<GaussianMixture> mixtures) {
        this.mixtures = mixtures;
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

    /**
     * @return the axesLabels
     */
    public List<String> getAxesLabels() {
        return axesLabels;
    }

    /**
     * @param axesLabels the axesLabels to set
     */
    public void setAxesLabels(List<String> axesLabels) {
        this.axesLabels = axesLabels;
    }
    //</editor-fold>
}
