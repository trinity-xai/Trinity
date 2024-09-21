package edu.jhuapl.trinity.data;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZeroPilotLatents {
    private static final Logger LOG = LoggerFactory.getLogger(ZeroPilotLatents.class);
    private int number;
    private ArrayList<Double> latents;
    private String labels;
    private Double traj_num;
    public static Function<String, ZeroPilotLatents> csvToZero = csv -> {
        ZeroPilotLatents zeroPilotLatents = new ZeroPilotLatents();
        String[] tokens = csv.split(",");
        try {
            zeroPilotLatents.setNumber(Integer.parseInt(tokens[0]));
            //we know that the first column is just a number and
            //the last two columns are label and traj_num
            zeroPilotLatents.latents = new ArrayList<>(tokens.length - 2);
            //-1 because they accidentally duplicated tissue_name in last column
            for (int i = 1; i < tokens.length - 3; i++) {
                zeroPilotLatents.latents.add(Double.valueOf(tokens[i]));
            }
            zeroPilotLatents.setLabels(tokens[tokens.length - 2]);
            zeroPilotLatents.setTraj_num(Double.parseDouble(tokens[tokens.length - 1]));
        } catch (NumberFormatException ex) {
            LOG.error("Exception", ex);
        }
        return zeroPilotLatents;
    };

    public ZeroPilotLatents() {

    }

    /**
     * @return the number
     */
    public int getNumber() {
        return number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * @return the latents
     */
    public ArrayList<Double> getLatents() {
        return latents;
    }

    /**
     * @param latents the latents to set
     */
    public void setLatents(ArrayList<Double> latents) {
        this.latents = latents;
    }

    /**
     * @return the labels
     */
    public String getLabels() {
        return labels;
    }

    /**
     * @param labels the labels to set
     */
    public void setLabels(String labels) {
        this.labels = labels;
    }

    /**
     * @return the traj_num
     */
    public Double getTraj_num() {
        return traj_num;
    }

    /**
     * @param traj_num the traj_num to set
     */
    public void setTraj_num(Double traj_num) {
        this.traj_num = traj_num;
    }
}
