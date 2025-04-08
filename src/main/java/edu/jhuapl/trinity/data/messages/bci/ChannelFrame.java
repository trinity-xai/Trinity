/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data.messages.bci;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelFrame extends MessageData {
    public static final String TYPESTRING = "ChannelFrame";
    // Payload data fields--------------------------------------------
    private List<Double> channelData;
    private List<String> dimensionNames;
    private String entityId;
    private long frameId;
    public final String messageType = TYPESTRING;
    //----------------------------------------------------------------

    public ChannelFrame() {
        channelData = new ArrayList<>();
        dimensionNames = new ArrayList<>();
    }

    public ChannelFrame(int channelDataSize) {
        channelData = new ArrayList<>(Collections.nCopies(channelDataSize, 0.0));
        dimensionNames = new ArrayList<>(Collections.nCopies(channelDataSize, ""));
    }

    public ChannelFrame(String entityId, long frameId, List<Double> channelData, List<String> dimensionNames) {
        this.entityId = entityId;
        this.frameId = frameId;
        this.channelData = channelData;
        this.dimensionNames = dimensionNames;
    }

    public static boolean isChannelFrame(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING);
    }

    public void setEntityId(String entity_id) {
        this.entityId = entity_id;
    }

    public List<Double> getChannelData() {
        return channelData;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setChannelData(List<Double> channelData) {
        this.channelData = channelData;
    }

    /**
     * @return the frameId
     */
    public long getFrameId() {
        return frameId;
    }

    /**
     * @param frameId the frameId to set
     */
    public void setFrameId(long frameId) {
        this.frameId = frameId;
    }

    /**
     * @return the dimensionNames
     */
    public List<String> getDimensionNames() {
        return dimensionNames;
    }

    /**
     * @param dimensionNames the dimensionNames to set
     */
    public void setDimensionNames(List<String> dimensionNames) {
        this.dimensionNames = dimensionNames;
    }
}
