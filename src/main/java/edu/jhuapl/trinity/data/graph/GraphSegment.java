/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphSegment {
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /**
     * {
     * "start": {
     * "identity": 12525037,
     * "labels": [
     * "Document"
     * ],
     * "properties": {
     * "name": "7630a867296188c0566d0a73",
     * "numberHashes": 276
     * }
     * },
     * "relationship": {
     * "identity": 32684,
     * "start": 12525037,
     * "end": 12487335,
     * "type": "SIMILAR",
     * "properties": {
     * "jaccard": 1.0
     * }
     * },
     * "end": {
     * "identity": 12487335,
     * "labels": [
     * "Document"
     * ],
     * "properties": {
     * "name": "c735cd9a7620c80fc53de855",
     * "numberHashes": 276
     * }
     * }
     * }
     */
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private GraphNode start;
    private GraphRelationship relationship;
    private GraphNode end;
    //</editor-fold>

    public GraphSegment() {
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the start
     */
    public GraphNode getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(GraphNode start) {
        this.start = start;
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
     * @return the end
     */
    public GraphNode getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(GraphNode end) {
        this.end = end;
    }
    //</editor-fold>
}
