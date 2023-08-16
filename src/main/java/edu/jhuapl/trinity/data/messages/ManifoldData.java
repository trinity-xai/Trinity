package edu.jhuapl.trinity.data.messages;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
