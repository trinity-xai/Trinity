/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphDirectedCollection {

    public static final String TYPESTRING = "GraphDirectedCollection";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {   "type": "GraphDirectedCollection",
        "nodes": [
            ...boat load of GraphNode objects
        ],
        "edges": [
            ...boat load of GraphEdge objects
        ]

    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String messageType;
    private String graphId;
    private String defaultNodeColor; // optional, ex #FF0000FF == fully opaque Red in RGBA HEX form
    private String defaultEdgeColor;// optional, ex #FF0000FF == fully opaque Red in RGBA HEX form
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    //</editor-fold>

    public GraphDirectedCollection() {
        messageType = TYPESTRING;
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
    }

    public static boolean isGraphDirectedCollection(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("nodes")
            && messageBody.contains("edges");
    }

    public Optional<GraphNode> findNodeById(String nodeId) {
        return nodes.stream()
            .filter(n -> n.getEntityID().contentEquals(nodeId))
            .findFirst();
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the type
     */
    public String getType() {
        return messageType;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.messageType = type;
    }

    /**
     * @return the graphId
     */
    public String getGraphId() {
        return graphId;
    }

    /**
     * @param graphId the graphId to set
     */
    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    /**
     * @return the defaultNodeColor
     */
    public String getDefaultNodeColor() {
        return defaultNodeColor;
    }

    /**
     * @param defaultNodeColor the defaultNodeColor to set
     */
    public void setDefaultNodeColor(String defaultNodeColor) {
        this.defaultNodeColor = defaultNodeColor;
    }

    /**
     * @return the defaultEdgeColor
     */
    public String getDefaultEdgeColor() {
        return defaultEdgeColor;
    }

    /**
     * @param defaultEdgeColor the defaultEdgeColor to set
     */
    public void setDefaultEdgeColor(String defaultEdgeColor) {
        this.defaultEdgeColor = defaultEdgeColor;
    }

    /**
     * @return the nodes
     */
    public List<GraphNode> getNodes() {
        return nodes;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(List<GraphNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * @return the edges
     */
    public List<GraphEdge> getEdges() {
        return edges;
    }

    /**
     * @param edges the edges to set
     */
    public void setEdges(List<GraphEdge> edges) {
        this.edges = edges;
    }

    //</editor-fold>
}
