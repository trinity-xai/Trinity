/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.handlers;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.Dimension;
import edu.jhuapl.trinity.data.messages.xai.ShapleyCollection;
import edu.jhuapl.trinity.data.messages.xai.ShapleyVector;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.ShapleyEvent;
import edu.jhuapl.trinity.javafx.renderers.ShapleyVectorRenderer;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class ShapleyEventHandler implements EventHandler<ShapleyEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(ShapleyEventHandler.class);

    List<ShapleyVectorRenderer> renderers;

    public ShapleyEventHandler() {
        renderers = new ArrayList<>();
    }

    public void addShapleyVectorRenderer(ShapleyVectorRenderer renderer) {
        renderers.add(renderer);
    }

    public void handleShapleyVectorEvent(ShapleyEvent event) {
        ShapleyVector shapleyVector = (ShapleyVector) event.object;
//        if (event.getEventType().equals(FeatureVectorEvent.LOCATE_FEATURE_VECTOR)) {
//            for (FeatureVectorRenderer renderer : renderers) {
//                renderer.locateFeatureVector(featureVector);
//            }
//        }
//        if (event.getEventType().equals(FeatureVectorEvent.NEW_FEATURE_VECTOR)) {
//            //Have we seen this label before?
//            FactorLabel matchingLabel = FactorLabel.getFactorLabel(featureVector.getLabel());
//            //The label is new... add a new FactorLabel row
//            if (null == matchingLabel) {
//                if (labelColorIndex > labelColorCount) {
//                    labelColorIndex = 0;
//                }
//                FactorLabel fl = new FactorLabel(featureVector.getLabel(),
//                    labelColorMap.getColorByIndex(labelColorIndex));
//                FactorLabel.addFactorLabel(fl);
//                labelColorIndex++;
//            }
//            //Have we seen this layer before?
//            int index = featureVector.getLayer();
//            FeatureLayer matchingLayer = FeatureLayer.getFeatureLayer(index);
//            //The layer is new... add a new FeatureLayer  row
//            if (null == matchingLayer) {
//                if (layerColorIndex > layerColorCount) {
//                    layerColorIndex = 0;
//                }
//                FeatureLayer fl = new FeatureLayer(index,
//                    layerColorMap.getColorByIndex(layerColorIndex));
//                FeatureLayer.addFeatureLayer(fl);
//                layerColorIndex++;
//            }
//
//            for (FeatureVectorRenderer renderer : renderers) {
//                renderer.addFeatureVector(featureVector);
//            }
//        }
    }

    public void handleShapleyCollectionEvent(ShapleyEvent event) {
        ShapleyCollection shapleyCollection = (ShapleyCollection) event.object;
        if (null == shapleyCollection || shapleyCollection.getValues().isEmpty())
            return;
        LOG.info("Imported ShapleyCollection size: {}", shapleyCollection.getValues().size());
        LOG.info("Imported ShapleyVector width: {}", shapleyCollection.getValues().get(0).getData().size());

//        Platform.runLater(() -> {
//            App.getAppScene().getRoot().fireEvent(
//                new CommandTerminalEvent("Scanning Feature Collection for labels...",
//                    new Font("Consolas", 20), Color.GREEN));
//        });

        for (ShapleyVectorRenderer renderer : renderers) {
            renderer.addShapleyCollection(shapleyCollection);
        }
        //Did the message specify any new dimensional label strings?
        if (null != shapleyCollection.getDimensionLabels() && !shapleyCollection.getDimensionLabels().isEmpty()) {
            Dimension.removeAllDimensions();
            int counter = 0;
            for (String dimensionLabel : shapleyCollection.getDimensionLabels()) {
                Dimension.addDimension(new Dimension(dimensionLabel,
                    counter++, Color.ALICEBLUE));
            }
            Platform.runLater(() -> {
                App.getAppScene().getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.DIMENSION_LABELS_SET,
                        shapleyCollection.getDimensionLabels()));

                App.getAppScene().getRoot().fireEvent(
                    new CommandTerminalEvent("New Dimensional Labels set",
                        new Font("Consolas", 20), Color.GREEN));
            });
            //update the renderers with the new arraylist of strings
            for (ShapleyVectorRenderer renderer : renderers) {
                //@TODO SMP
//                renderer.setDimensionLabels(shapleyCollection.getDimensionLabels());
                renderer.refresh();
            }
        }
    }

    @Override
    public void handle(ShapleyEvent event) {
        if (event.getEventType().equals(ShapleyEvent.NEW_SHAPLEY_VECTOR))
            handleShapleyVectorEvent(event);
        else if (event.getEventType().equals(ShapleyEvent.NEW_SHAPLEY_COLLECTION))
            handleShapleyCollectionEvent(event);
    }
}
