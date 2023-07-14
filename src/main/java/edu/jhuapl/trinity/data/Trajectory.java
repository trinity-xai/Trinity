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

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * @author Sean Phillips
 */
public class Trajectory {

    public double radius;
    public double angle;
    public ArrayList<double[]> states = new ArrayList<>();
    public ArrayList<Double> times = new ArrayList<>();
    public Boolean visible = true;
    public Color lastColor = Color.LIGHTSKYBLUE;
    public Double stability = null;
    public String trialID = null;

    public Trajectory(String id) {
        states = new ArrayList<>();
        states.add(new double[]{0.0, 0.0, 0.0});
        times = new ArrayList<>();
        times.add(0.0);
        this.trialID = id;
    }

    public Trajectory(String id, ArrayList<double[]> newStates, ArrayList<Double> newTimes) {
        this.states = new ArrayList<>(newStates);
        this.times = new ArrayList<>(newTimes);
        this.trialID = id;
    }

    public double[] getMaxXState() {
        return states.stream().max((double[] o1, double[] o2) -> {
            if (o1[0] < o2[0]) return -1;
            else if (o1[0] > o1[0]) return 1;
            else return 0;
        }).orElse(null);
    }

    public double[] getMaxYState() {
        return states.stream().max((double[] o1, double[] o2) -> {
            if (o1[1] < o2[1]) return -1;
            else if (o1[1] > o1[1]) return 1;
            else return 0;
        }).orElse(null);
    }

    public double[] getMaxZState() {
        return states.stream().max((double[] o1, double[] o2) -> {
            if (o1[2] < o2[2]) return -1;
            else if (o1[2] > o1[2]) return 1;
            else return 0;
        }).orElse(null);
    }

    public double[] getMinXState() {
        return states.stream().min((double[] o1, double[] o2) -> {
            if (o1[0] < o2[0]) return -1;
            else if (o1[0] > o1[0]) return 1;
            else return 0;
        }).orElse(null);
    }

    public double[] getMinYState() {
        return states.stream().min((double[] o1, double[] o2) -> {
            if (o1[1] < o2[1]) return -1;
            else if (o1[1] > o1[1]) return 1;
            else return 0;
        }).orElse(null);
    }

    public double[] getMinZState() {
        return states.stream().min((double[] o1, double[] o2) -> {
            if (o1[2] < o2[2]) return -1;
            else if (o1[2] > o1[2]) return 1;
            else return 0;
        }).orElse(null);
    }

    public double getRangeX() {
        Double maxX = null;
        Double minX = null;
        Double range = null;
        for (double[] state : states) {
            //update coordinate transformation information for X
            maxX = null == maxX || state[0] > maxX ? state[0] : maxX;
            minX = null == minX || state[0] < minX ? state[0] : minX;
            range = maxX - minX;
        }
        return range;
    }

    public double getRangeY() {
        Double max = null;
        Double min = null;
        Double range = null;
        for (double[] state : states) {
            max = null == max || state[1] > max ? state[1] : max;
            min = null == min || state[1] < min ? state[1] : min;
            range = max - min;
        }
        return range;
    }

    public double getRangeZ() {
        Double max = null;
        Double min = null;
        Double range = null;
        for (double[] state : states) {
            max = null == max || state[2] > max ? state[2] : max;
            min = null == min || state[2] < min ? state[2] : min;
            range = max - min;
        }
        return range;
    }

    public static Comparator<Trajectory> TrajectoryStabilityComparator = (Trajectory t1, Trajectory t2) -> {
        if (t1.stability < t2.stability)
            return -1;
        else if (t1.stability > t2.stability)
            return 1;
        else return 0;
    };
}
