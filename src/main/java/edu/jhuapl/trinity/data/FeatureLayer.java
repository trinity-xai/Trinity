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
public class FeatureLayer {

    private int index;
    private Color color;
    public SimpleBooleanProperty visible;

    public FeatureLayer(int index, Color color) {
        this.index = index;
        this.color = color;
        visible = new SimpleBooleanProperty(true);
    }

    /**
     * Provides lookup mechanism.
     */
    private static HashMap<Integer, FeatureLayer> globalLayerMap = new HashMap<>();

    public static Collection<FeatureLayer> getFeatureLayers() {
        return globalLayerMap.values();
    }

    public static FeatureLayer getFeatureLayer(Integer index) {
        return globalLayerMap.get(index);
    }

    public static void addFeatureLayer(FeatureLayer featureLayer) {
        globalLayerMap.put(featureLayer.getIndex(), featureLayer);
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.ADDED_FEATURE_LAYER, featureLayer));
        });
    }

    public static void addAllFeatureLayer(List<FeatureLayer> featureLayers) {
        featureLayers.forEach(featureLayer -> {
            globalLayerMap.put(featureLayer.getIndex(), featureLayer);
        });
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.ADDEDALL_FEATURE_LAYER, featureLayers));
        });
    }

    public static FeatureLayer removeFeatureLayer(Integer index) {
        FeatureLayer removed = globalLayerMap.remove(index);
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.REMOVED_FACTOR_LABEL, removed));
        });
        return removed;
    }

    public static void removeAllFeatureLayers() {
        globalLayerMap.clear();
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.CLEARED_FEATURE_LAYERS));
        });
    }

    public static void updateFeatureLayer(Integer index, FeatureLayer featureLayer) {
        globalLayerMap.put(index, featureLayer);
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.UPDATED_FEATURE_LAYER, featureLayer));
        });
    }

    public static Color getColorByIndex(Integer index) {
        if (null == index) return Color.ALICEBLUE;
        FeatureLayer fl = FeatureLayer.getFeatureLayer(index);
        if (null == fl)
            return Color.ALICEBLUE;
        return fl.getColor();
    }

    public static boolean visibilityByIndex(Integer index) {
        if (null == index) return true;
        FeatureLayer fl = FeatureLayer.getFeatureLayer(index);
        if (null == fl) return true;
        return fl.getVisible();
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(int index) {
        this.index = index;
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

}
