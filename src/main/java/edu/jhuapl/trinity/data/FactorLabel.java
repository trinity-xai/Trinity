package edu.jhuapl.trinity.data;

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

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.paint.Color;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class FactorLabel {

    private String label;
    private Color color;
    public SimpleBooleanProperty visible;
    public SimpleBooleanProperty ellipsoidsVisible;

    public FactorLabel(String label, Color color) {
        this.label = label;
        this.color = color;
        visible = new SimpleBooleanProperty(true);
        ellipsoidsVisible = new SimpleBooleanProperty(true);
    }

    /**
     * Provides lookup mechanism to find any object model that is currently
     * anchored in the system.
     */
    private static HashMap<String, FactorLabel> globalLabelMap = new HashMap<>();

    public static Collection<FactorLabel> getFactorLabels() {
        return globalLabelMap.values();
    }

    public static FactorLabel getFactorLabel(String label) {
        return globalLabelMap.get(label);
    }

    public static void addFactorLabel(FactorLabel factorLabel) {
        globalLabelMap.put(factorLabel.getLabel(), factorLabel);
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.ADDED_FACTOR_LABEL, factorLabel));
        });
    }

    public static void addAllFactorLabels(List<FactorLabel> factorLabels) {
        factorLabels.forEach(factorLabel -> {
            globalLabelMap.put(factorLabel.getLabel(), factorLabel);
        });
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.ADDEDALL_FACTOR_LABELS, factorLabels));
        });
    }

    public static void removeAllFactorLabels() {
        globalLabelMap.clear();
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.CLEARED_FACTOR_LABELS));
        });
    }

    public static FactorLabel removeFactorLabel(String label) {
        FactorLabel removed = globalLabelMap.remove(label);
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.REMOVED_FACTOR_LABEL, removed));
        });
        return removed;
    }

    public static void updateFactorLabel(String label, FactorLabel factorLabel) {
        Platform.runLater(() -> {
            globalLabelMap.put(label, factorLabel);
            App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.UPDATED_FACTOR_LABEL, factorLabel));
        });
    }

    public static Color getColorByLabel(String label) {
        FactorLabel fl = FactorLabel.getFactorLabel(label);
        if (null == fl)
            return Color.ALICEBLUE;
        return fl.getColor();
    }

    public static boolean visibilityByLabel(String label) {
        FactorLabel fl = FactorLabel.getFactorLabel(label);
        if (null == fl)
            return true;
        return fl.getVisible();
    }

    public static void setAllVisible(boolean visible) {
        globalLabelMap.forEach((s, fl) -> {
            fl.setVisible(visible);
            fl.setEllipsoidsVisible(visible);
        });
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

    public SimpleBooleanProperty visibleProperty() {
        return this.visible;
    }

    public java.lang.Boolean getVisible() {
        return this.visibleProperty().get();
    }

    public void setVisible(final java.lang.Boolean visible) {
        this.visibleProperty().set(visible);
    }

    public SimpleBooleanProperty ellipsoidsVisibleProperty() {
        return this.ellipsoidsVisible;
    }

    public java.lang.Boolean getEllipsoidsVisible() {
        return this.ellipsoidsVisibleProperty().get();
    }

    public void setEllipsoidsVisible(final java.lang.Boolean ellipsoidsVisible) {
        this.ellipsoidsVisibleProperty().set(ellipsoidsVisible);
    }

}
