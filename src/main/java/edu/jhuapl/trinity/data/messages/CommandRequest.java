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

import java.util.HashMap;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandRequest extends MessageData {

    public enum COMMANDS {
        VIEW_HYPERSPACE, VIEW_PROJECTIONS, EXECUTE_UMAP, AUTO_PROJECTION
    }
    public static final String TYPESTRING = "command_request";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    '{
        "messageType": "command_request",
        "request": "SOME_ENUMERATED_COMMAND",
        "delaySeconds" : 5.0,
        "properties": {
            "enabled" : "true",
            yada yada
        }
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String request;
    private double delaySeconds = 0.0; //seconds
    private HashMap<String, String> properties;
    //</editor-fold>

    public CommandRequest() {
        messageType = TYPESTRING;
        properties = new HashMap<>();
    }

    public static boolean isCommandRequest(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING);
    }

   //<editor-fold defaultstate="collapsed" desc="Properties">
    /**
     * @return the request
     */
    public String getRequest() {
        return request;
    }

    /**
     * @param request the request to set
     */
    public void setRequest(String request) {
        this.request = request;
    }

    /**
     * @return the properties
     */
    public HashMap<String, String> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(HashMap<String, String> properties) {
        this.properties = properties;
    }
    /**
     * @return the delaySeconds
     */
    public double getDelaySeconds() {
        return delaySeconds;
    }

    /**
     * @param delaySeconds the delaySeconds to set
     */
    public void setDelaySeconds(double delaySeconds) {
        this.delaySeconds = delaySeconds;
    }    
   //</editor-fold>
}
