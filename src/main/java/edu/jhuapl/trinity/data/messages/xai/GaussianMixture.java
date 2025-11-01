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
public class GaussianMixture extends MessageData {
    public static enum COVARIANCE_MODE {
        DIAGONAL, FULL
    }

    public static final String TYPESTRING = "gaussian_mixture";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {
        "version": "0.1",
        "component": "message_tester",
        "componentType": "simulated_data",
        "componentName": "gaussian_mixture_simulation",
        "messageType": "gaussian_mixture",
        "topic": "gaussian_mixture",
        "messageId": 0,
        "numFeatures": 255,
        "numComponents": 4,
        "covarianceMode": "diag",
        "data": [
            {
                "mean": [list of floats of length numFeatures],
                "covariance": [list of floats of length numFeatures]
            },
            {
                "mean": [list of floats of length numFeatures],
                "covariance": [list of floats of length numFeatures]
            },
            {
                "mean": [list of floats of length numFeatures],
                "covariance": [list of floats of length numFeatures]
            },
            {
                "mean": [list of floats of length numFeatures],
                "covariance": [list of floats of length numFeatures]
            }
        ],
        "label": "horse"
    }

    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String entityId;
    private List<GaussianMixtureData> data;
    private String label;  // "horse"
    private int numFeatures;  //255,
    private int numComponents;  //4,
    private String covarianceMode;  //"diag", "full"
    //</editor-fold>

    public GaussianMixture() {
        this.messageType = TYPESTRING;
        this.data = new ArrayList<>();
    }

    public static boolean isGaussianMixture(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("covarianceMode");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the entityId
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * @param entityId the entityId to set
     */
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    /**
     * @return the data
     */
    public List<GaussianMixtureData> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(List<GaussianMixtureData> data) {
        this.data = data;
    }

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
     * @return the numFeatures
     */
    public int getNumFeatures() {
        return numFeatures;
    }

    /**
     * @param numFeatures the numFeatures to set
     */
    public void setNumFeatures(int numFeatures) {
        this.numFeatures = numFeatures;
    }

    /**
     * @return the numComponents
     */
    public int getNumComponents() {
        return numComponents;
    }

    /**
     * @param numComponents the numComponents to set
     */
    public void setNumComponents(int numComponents) {
        this.numComponents = numComponents;
    }

    /**
     * @return the covarianceMode
     */
    public String getCovarianceMode() {
        return covarianceMode;
    }

    /**
     * @param covarianceMode the covarianceMode to set
     */
    public void setCovarianceMode(String covarianceMode) {
        this.covarianceMode = covarianceMode;
    }
    //</editor-fold>
}
