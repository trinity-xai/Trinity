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

import edu.jhuapl.trinity.data.messages.FeatureCollection;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class Trajectory {

    public Integer totalStates = null;
    public ArrayList<double[]> states = new ArrayList<>();
    public ArrayList<Double> times = new ArrayList<>();
    private Boolean visible = true;
    private Color color = Color.LIGHTSKYBLUE;
    private Double stability = null;
    private String label = null;

    public Trajectory(String label) {
        states = new ArrayList<>();
        states.add(new double[]{0.0, 0.0, 0.0});
        times = new ArrayList<>();
        times.add(0.0);
        this.label = label;
    }

    public Trajectory(String label, ArrayList<double[]> newStates, ArrayList<Double> newTimes) {
        this.states = new ArrayList<>(newStates);
        this.times = new ArrayList<>(newTimes);
        this.label = label;
        totalStates = this.states.size();
    }

    /**
     * Provides lookup mechanism to find any object model that is currently
     * anchored in the system.
     */
    private static HashMap<String, Trajectory> globalTrajectoryMap = new HashMap<>();
    public static HashMap<Trajectory, FeatureCollection> globalTrajectoryToFeatureCollectionMap = new HashMap<>();

    public static Collection<Trajectory> getTrajectories() {
        return globalTrajectoryMap.values();
    }

    public static Trajectory getTrajectory(String label) {
        return globalTrajectoryMap.get(label);
    }

    public static void addTrajectory(Trajectory trajectory) {
        globalTrajectoryMap.put(trajectory.getLabel(), trajectory);
    }

    public static void addAllTrajectories(List<Trajectory> trajectories) {
        trajectories.forEach(t -> {
            globalTrajectoryMap.put(t.getLabel(), t);
        });
    }

    public static void removeAllTrajectories() {
        globalTrajectoryMap.clear();
    }

    public static Trajectory removeTrajectory(String label) {
        Trajectory removed = globalTrajectoryMap.remove(label);
        return removed;
    }

    public static void updateTrajectory(String label, Trajectory trajectory) {
        globalTrajectoryMap.put(label, trajectory);
    }

    public static Color getColorByLabel(String label) {
        Trajectory fl = Trajectory.getTrajectory(label);
        if (null == fl)
            return Color.ALICEBLUE;
        return fl.getColor();
    }

    public static boolean visibilityByLabel(String label) {
        Trajectory fl = Trajectory.getTrajectory(label);
        if (null == fl)
            return true;
        return fl.getVisible();
    }

    public static void setAllVisible(boolean visible) {
        globalTrajectoryMap.forEach((s, fl) -> {
            fl.setVisible(visible);
        });
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

    /**
     * @return the stability
     */
    public Double getStability() {
        return stability;
    }

    /**
     * @param stability the stability to set
     */
    public void setStability(Double stability) {
        this.stability = stability;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the visible
     */
    public Boolean getVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }

    public static Comparator<Trajectory> TrajectoryStabilityComparator = (Trajectory t1, Trajectory t2) -> {
        if (t1.getStability() < t2.getStability())
            return -1;
        else if (t1.getStability() > t2.getStability())
            return 1;
        else return 0;
    };
}
