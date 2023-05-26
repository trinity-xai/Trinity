package edu.jhuapl.trinity.data.messages;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReconstructionAttributes extends MessageData {

    public static final String TYPESTRING = "reconstruction_attributes";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        "attrs": {
            "rec": "20220825T1416",
            "events": {
                "pants": [
                    29.672713541666667,
                    35.87271354166667
                ],
                etc etc etc
            },
            "framerate": 2.000575694920736
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String rec;
    private Map<String, Object> events;
    private Double framerate;
    //</editor-fold>

    public ReconstructionAttributes() {
        this.messageType = TYPESTRING;
        events = new HashMap<>();
    }

    public static boolean isReconstructionAttributes(String messageBody) {
        return messageBody.contains("rec") && messageBody.contains("events")
            && messageBody.contains("framerate");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the rec
     */
    public String getRec() {
        return rec;
    }

    /**
     * @param rec the rec to set
     */
    public void setRec(String rec) {
        this.rec = rec;
    }

    /**
     * @return the events
     */
    public Map<String, Object> getEvents() {
        return events;
    }

    /**
     * @param events the events to set
     */
    public void setEvents(Map<String, Object> events) {
        this.events = events;
    }

    /**
     * @return the framerate
     */
    public Double getFramerate() {
        return framerate;
    }

    /**
     * @param framerate the framerate to set
     */
    public void setFramerate(Double framerate) {
        this.framerate = framerate;
    }
    //</editor-fold>
}
