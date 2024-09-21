package edu.jhuapl.trinity.data.messages;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2024 Sean Phillips
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
import java.util.List;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VectorMask extends MessageData {

    public static final String TYPESTRING = "vector_mask";

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String entityId;
    private List<Double> data;
    private String color; //ex #FF0000FF == fully opaque Red in RGBA HEX form
    //</editor-fold>

    public VectorMask() {
        this.messageType = TYPESTRING;
        this.data = new ArrayList<>();
    }

    public static boolean isVectorMask(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING);
    }

    public static Function<VectorMask, String> mapDataToString = (state) -> {
        return state.getData().toString();
    };

    public static Function<VectorMask, double[]> mapToStateArray = (state) -> {
        double[] states = new double[state.getData().size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = state.getData().get(i);
        }
        return states;
    };

    public static VectorMask fromData(double[] data, int width, double scaling) {
        VectorMask fv = new VectorMask();
        for (int vectorIndex = 0; vectorIndex < width; vectorIndex++) {
            fv.getData().add(data[vectorIndex] * scaling);
        }
        return fv;
    }

    public double minDataValue() {
        return getData().stream().min(Double::compare).get();
    }

    public double maxDataValue() {
        return getData().stream().max(Double::compare).get();
    }

    public double totalDataWidth() {
        return Math.abs(maxDataValue() - minDataValue());
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the entityId
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * @param entityId the entityId to set
     */
    public void setEntityId(String entityId) {
        this.entityId = entityId;
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
    //</editor-fold>

    /**
     * @return the data
     */
    public List<Double> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(List<Double> data) {
        this.data = data;
    }

}
