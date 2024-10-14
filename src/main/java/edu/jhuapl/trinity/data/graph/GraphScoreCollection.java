/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphScoreCollection {

    public static final String TYPESTRING = "GraphScoreCollection";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {   "type": "GraphScoreCollection",
        "graphscores": [
            ...boat load of GraphScore objects
        ]
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<GraphScore> graphscores;
    private String type;
    //</editor-fold>

    public GraphScoreCollection() {
        type = TYPESTRING;
        this.graphscores = new ArrayList<>();
    }

    public static boolean isGraphScoreCollection(String messageBody) {
        return messageBody.contains("type")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("graphscores");
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
     * @return the graphscores
     */
    public List<GraphScore> getGraphscores() {
        return graphscores;
    }

    /**
     * @param graphscores the graphscores to set
     */
    public void setGraphscores(List<GraphScore> graphscores) {
        this.graphscores = graphscores;
    }
    //</editor-fold>
}
