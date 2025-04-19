package edu.jhuapl.trinity.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
public class FactorAnalysisState {
    public static final String TYPESTRING = "FactorAnalysisState";
    private String entityId;
    private long frameId;
    private List<Double> factors;
    public final String messageType = TYPESTRING;

    public FactorAnalysisState() {
        this.factors = new ArrayList<>();
    }

    public FactorAnalysisState(String entityId, long frameId, List<Double> factors) {
        this.entityId = entityId;
        this.frameId = frameId;
        this.factors = factors;
    }

    public static boolean isFactorAnalysisState(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING);
    }

    public static Function<FactorAnalysisState, String> mapToString = (state) -> {
        return state.getFactors().toString();
    };

    public static Function<FactorAnalysisState, double[]> mapToStateArray = (state) -> {
        double[] states = new double[state.factors.size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = state.factors.get(i);
        }
        return states;
    };

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
     * @return the frameId
     */
    public long getFrameId() {
        return frameId;
    }

    /**
     * @param frameId the frameId to set
     */
    public void setFrameId(long frameId) {
        this.frameId = frameId;
    }

    /**
     * @return the factors
     */
    public List<Double> getFactors() {
        return factors;
    }

    /**
     * @param factors the factors to set
     */
    public void setFactors(List<Double> factors) {
        this.factors = factors;
    }
}
