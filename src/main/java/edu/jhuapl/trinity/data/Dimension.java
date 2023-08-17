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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javafx.scene.paint.Color;


/**
 * @author Sean Phillips
 */
public class Dimension {
    public String labelString; 
    public int index; 
    public Color color;

    public Dimension(String label, int index, Color color) {
        this.labelString = label;
        this.index = index;
        this.color = color;
    }
    /**
     * Provides lookup mechanism to find any object model that is currently
     * anchored in the system.
     */
    private static ArrayList<Dimension> globalDimensionList = new ArrayList<>();

    public static ArrayList<Dimension> getDimensions() {
        return globalDimensionList;
    }
    public static ArrayList<String> getDimensionsAsStrings() {
        ArrayList<String> strings = globalDimensionList.stream()
            .map(t -> t.labelString)
            .collect(Collectors.toCollection(ArrayList::new));
        return strings;
    }
    public static Dimension getDimension(int index) {
        if(index >= globalDimensionList.size()) return null;
        return globalDimensionList.get(index);
    }

    public static void addDimension(Dimension dimension) {
        globalDimensionList.add(dimension);
    }

    public static void addAllDimensions(List<Dimension> dimensions) {
        globalDimensionList.addAll(dimensions);
    }

    public static void removeAllDimensions() {
        globalDimensionList.clear();
    }

    public static boolean replaceDimension(int index, Dimension d) {
        if(index >= globalDimensionList.size()) return false;
        globalDimensionList.remove(d.index);
        globalDimensionList.add(d.index,d);
        return true;
    }

    public static boolean removeDimension(int index) {
        if(index >= globalDimensionList.size()) return false;
        Dimension removed = globalDimensionList.remove(index);
        return true;
    }
}
