package edu.jhuapl.trinity.utils.statistics;

import java.util.List;
import java.util.Objects;

public class AxisParams {
    private StatisticEngine.ScalarType type;
    private String metricName;         // for METRIC_DISTANCE_TO_MEAN
    private List<Double> referenceVec; // for METRIC_DISTANCE_TO_MEAN
    private Integer componentIndex;    // for COMPONENT_AT_DIMENSION

    public AxisParams() {

    }

    public AxisParams(StatisticEngine.ScalarType type,
                      String metricName,
                      List<Double> referenceVec,
                      Integer componentIndex) {
        this.type = Objects.requireNonNull(type, "type");
        this.metricName = metricName;
        this.referenceVec = referenceVec;
        this.componentIndex = componentIndex;
    }

    /**
     * @return the type
     */
    public StatisticEngine.ScalarType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(StatisticEngine.ScalarType type) {
        this.type = type;
    }

    public static AxisParams of(StatisticEngine.ScalarType type) {
        return new AxisParams(type, null, null, null);
    }

    /**
     * @return the metricName
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * @return the referenceVec
     */
    public List<Double> getReferenceVec() {
        return referenceVec;
    }

    /**
     * @return the componentIndex
     */
    public Integer getComponentIndex() {
        return componentIndex;
    }

    /**
     * @param metricName the metricName to set
     */
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    /**
     * @param referenceVec the referenceVec to set
     */
    public void setReferenceVec(List<Double> referenceVec) {
        this.referenceVec = referenceVec;
    }

    /**
     * @param componentIndex the componentIndex to set
     */
    public void setComponentIndex(Integer componentIndex) {
        this.componentIndex = componentIndex;
    }
}
