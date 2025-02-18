/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.messages;

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
        VIEW_HYPERSPACE, VIEW_HYPERSURFACE, VIEW_PROJECTIONS, EXECUTE_UMAP, AUTO_PROJECTION
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
