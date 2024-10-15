/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;


/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManifoldData extends MessageData {

    public static final String TYPESTRING = "manifold_data";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    '{
        "messageType": "manifold_data",
        "points": [
        {
        "x":1.0,
        "y":2.5,
        "z":9000.1
        },
        {
        "x":1.0,
        "y":2.5,
        "z":9000.1
        },
        {
        "x":1.0,
        "y":2.5,
        "z":9000.1
        },
        {
        "x":1.0,
        "y":2.5,
        "z":9000.1
        },
        etc...
        ],
        "diffuseColor":"#FF00FF88",
        "specularColor":"#66FFFFAA",
        "wireframeColor":"#FF000088",
        "label": "horse",
        "clearAll" : "false" //nuclear flag to remove all existing manifolds
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private ArrayList<P3D> points;
    private Boolean clearAll;
    //</editor-fold>

    public ManifoldData() {
        messageType = TYPESTRING;
        points = new ArrayList<>();
    }

    public static boolean isManifoldData(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING);
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the clearAll
     */
    public Boolean isClearAll() {
        return clearAll;
    }

    /**
     * @param clearAll the clearAll to set
     */
    public void setClearAll(Boolean clearAll) {
        this.clearAll = clearAll;
    }
    //</editor-fold>

    /**
     * @return the points
     */
    public ArrayList<P3D> getPoints() {
        return points;
    }

    /**
     * @param points the points to set
     */
    public void setPoints(ArrayList<P3D> points) {
        this.points = points;
    }

}
