package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.text.DecimalFormat;
import java.text.NumberFormat;


/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UmapConfig extends MessageData {

    public static final String TYPESTRING = "umap_config";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    '{
        "messageType": "manifold_data",
        "repulsionStrength":0.5,
        "minDist": 0.1,
        "spread": 0.1,
        "opMixRatio":0.9,
        "numberComponents":3,
        "numberEpochs":200,
        "numberNearestNeighbours":15,
        "negativeSampleRate":10,
        "localConnectivity":10,
        "metric":"Euclidean",
        "verbose":"true"
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">

    private Float repulsionStrength;
    private Float minDist;
    private Float spread;
    private Float opMixRatio;
    private Integer numberComponents;
    private Integer numberEpochs;
    private Integer numberNearestNeighbours;
    private Integer negativeSampleRate;
    private Integer localConnectivity;
    private Double threshold;
    private Float targetWeight;
    private String metric;
    private Boolean verbose;

    //</editor-fold>

    public UmapConfig() {
        messageType = TYPESTRING;

    }

    public static boolean isUmapConfig(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("numberComponents")
            && messageBody.contains("numberNearestNeighbours");
    }

    public static String configToFilename(UmapConfig uc) {
        NumberFormat format = new DecimalFormat("0.00");
        StringBuilder sb = new StringBuilder("UmapConfig-");
//        sb.append(targetWeightSlider.getValue()).append("-");
        sb.append(uc.getMetric()).append("-");
        sb.append("R").append(format.format(uc.getRepulsionStrength())).append("-");
        sb.append("MD").append(format.format(uc.getMinDist())).append("-");
        sb.append("S").append(format.format(uc.getSpread())).append("-");
        sb.append("OPM").append(format.format(uc.getOpMixRatio())).append("-");
//        uc.setNumberComponents((int) numComponentsSpinner.getValue());
//        uc.setNumberEpochs((int) numEpochsSpinner.getValue());
        sb.append("NN").append(uc.getNumberNearestNeighbours()).append("-");
        sb.append("NSR").append(uc.getNegativeSampleRate()).append("-");
        sb.append("LC").append(uc.getLocalConnectivity());
//        uc.setThreshold((double) thresholdSpinner.getValue());
//        uc.setVerbose(verboseCheckBox.isSelected());
        return sb.toString();
    }

    public String prettyPrint() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    public String print() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.writeValueAsString(this);
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the repulsionStrength
     */
    public Float getRepulsionStrength() {
        return repulsionStrength;
    }

    /**
     * @param repulsionStrength the repulsionStrength to set
     */
    public void setRepulsionStrength(Float repulsionStrength) {
        this.repulsionStrength = repulsionStrength;
    }

    /**
     * @return the minDist
     */
    public Float getMinDist() {
        return minDist;
    }

    /**
     * @param minDist the minDist to set
     */
    public void setMinDist(Float minDist) {
        this.minDist = minDist;
    }

    /**
     * @return the spread
     */
    public Float getSpread() {
        return spread;
    }

    /**
     * @param spread the spread to set
     */
    public void setSpread(Float spread) {
        this.spread = spread;
    }

    /**
     * @return the opMixRatio
     */
    public Float getOpMixRatio() {
        return opMixRatio;
    }

    /**
     * @param opMixRatio the opMixRatio to set
     */
    public void setOpMixRatio(Float opMixRatio) {
        this.opMixRatio = opMixRatio;
    }

    /**
     * @return the numberComponents
     */
    public Integer getNumberComponents() {
        return numberComponents;
    }

    /**
     * @param numberComponents the numberComponents to set
     */
    public void setNumberComponents(Integer numberComponents) {
        this.numberComponents = numberComponents;
    }

    /**
     * @return the numberEpochs
     */
    public Integer getNumberEpochs() {
        return numberEpochs;
    }

    /**
     * @param numberEpochs the numberEpochs to set
     */
    public void setNumberEpochs(Integer numberEpochs) {
        this.numberEpochs = numberEpochs;
    }

    /**
     * @return the numberNearestNeighbours
     */
    public Integer getNumberNearestNeighbours() {
        return numberNearestNeighbours;
    }

    /**
     * @param numberNearestNeighbours the numberNearestNeighbours to set
     */
    public void setNumberNearestNeighbours(Integer numberNearestNeighbours) {
        this.numberNearestNeighbours = numberNearestNeighbours;
    }

    /**
     * @return the negativeSampleRate
     */
    public Integer getNegativeSampleRate() {
        return negativeSampleRate;
    }

    /**
     * @param negativeSampleRate the negativeSampleRate to set
     */
    public void setNegativeSampleRate(Integer negativeSampleRate) {
        this.negativeSampleRate = negativeSampleRate;
    }

    /**
     * @return the localConnectivity
     */
    public Integer getLocalConnectivity() {
        return localConnectivity;
    }

    /**
     * @param localConnectivity the localConnectivity to set
     */
    public void setLocalConnectivity(Integer localConnectivity) {
        this.localConnectivity = localConnectivity;
    }

    /**
     * @return the metric
     */
    public String getMetric() {
        return metric;
    }

    /**
     * @param metric the metric to set
     */
    public void setMetric(String metric) {
        this.metric = metric;
    }

    /**
     * @return the verbose
     */
    public Boolean getVerbose() {
        return verbose;
    }

    /**
     * @param verbose the verbose to set
     */
    public void setVerbose(Boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * @return the threshold
     */
    public Double getThreshold() {
        return threshold;
    }

    /**
     * @param threshold the threshold to set
     */
    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    /**
     * @return the targetWeight
     */
    public Float getTargetWeight() {
        return targetWeight;
    }

    /**
     * @param targetWeight the targetWeight to set
     */
    public void setTargetWeight(Float targetWeight) {
        this.targetWeight = targetWeight;
    }

    //</editor-fold>
}
