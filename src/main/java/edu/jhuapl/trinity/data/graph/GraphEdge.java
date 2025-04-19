package edu.jhuapl.trinity.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphEdge {
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /**
     * {
     * "start": 12525037,
     * "end": 12487335,
     * "relationship": {
     * "labels": [
     * "Document"
     * ],
     * "properties": {
     * "name": "c735cd9a7620c80fc53de855",
     * "numberHashes": 276
     * }
     * }
     */
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String startID; //maps to GraphNode indentity
    private GraphRelationship relationship; //optional
    private String color; //optional overrides default color
    //ex #FF0000FF == fully opaque Red in RGBA HEX form
    private String endID;
    //</editor-fold>

    public GraphEdge() {
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the startId
     */
    public String getStartID() {
        return startID;
    }

    /**
     * @param startId the startId to set
     */
    public void setStartID(String startId) {
        this.startID = startId;
    }

    /**
     * @return the relationship
     */
    public GraphRelationship getRelationship() {
        return relationship;
    }

    /**
     * @param relationship the relationship to set
     */
    public void setRelationship(GraphRelationship relationship) {
        this.relationship = relationship;
    }

    /**
     * @return the color
     */
    public String getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * @return the endId
     */
    public String getEndID() {
        return endID;
    }

    /**
     * @param endId the endId to set
     */
    public void setEndID(String endId) {
        this.endID = endId;
    }

    //</editor-fold>
}
