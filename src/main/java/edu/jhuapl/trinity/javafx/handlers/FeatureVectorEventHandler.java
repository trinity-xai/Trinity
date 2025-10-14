package edu.jhuapl.trinity.javafx.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.Dimension;
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.FeatureLayer;
import edu.jhuapl.trinity.data.messages.xai.CyberReport;
import edu.jhuapl.trinity.data.messages.xai.CyberVector;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.data.messages.xai.LabelConfig;
import edu.jhuapl.trinity.javafx.components.ColorMap;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.renderers.FeatureVectorRenderer;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class FeatureVectorEventHandler implements EventHandler<FeatureVectorEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureVectorEventHandler.class);

    List<FeatureVectorRenderer> renderers;
    public double[][] weights = null;
    ColorMap labelColorMap;
    ColorMap layerColorMap;
    int labelColorIndex = 0;
    int labelColorCount = 0;
    int layerColorIndex = 0;
    int layerColorCount = 0;

    public FeatureVectorEventHandler() {
        renderers = new ArrayList<>();
        labelColorMap = ColorMap.tableau();
        labelColorCount = labelColorMap.getFixedColorCount();
        layerColorMap = ColorMap.jet();
        layerColorCount = labelColorMap.getFixedColorCount();
    }

    public void addFeatureVectorRenderer(FeatureVectorRenderer renderer) {
        renderers.add(renderer);
    }

    public static FeatureVector cyberToFeatureVector(String label, CyberVector cyberVector) {
        FeatureVector fv = new FeatureVector();
        fv.setData(CyberVector.mapToVectorList.apply(cyberVector));
        if (null != label) {
            fv.setLabel(label);
            try {
                fv.setText(cyberVector.asJSON(label));
            } catch (JsonProcessingException ex) {

            }
        }
        return fv;
    }

    private void addNewFeatureVector(FeatureVector featureVector) {
        //Have we seen this label before?
        FactorLabel matchingLabel = FactorLabel.getFactorLabel(featureVector.getLabel());
        //The label is new... add a new FactorLabel row
        if (null == matchingLabel) {
            if (labelColorIndex > labelColorCount) {
                labelColorIndex = 0;
            }
            FactorLabel fl = new FactorLabel(featureVector.getLabel(),
                labelColorMap.getColorByIndex(labelColorIndex));
            FactorLabel.addFactorLabel(fl);
            labelColorIndex++;
        }
        //Have we seen this layer before?
        int index = featureVector.getLayer();
        FeatureLayer matchingLayer = FeatureLayer.getFeatureLayer(index);
        //The layer is new... add a new FeatureLayer  row
        if (null == matchingLayer) {
            if (layerColorIndex > layerColorCount) {
                layerColorIndex = 0;
            }
            FeatureLayer fl = new FeatureLayer(index,
                layerColorMap.getColorByIndex(layerColorIndex));
            FeatureLayer.addFeatureLayer(fl);
            layerColorIndex++;
        }

        for (FeatureVectorRenderer renderer : renderers) {
            renderer.addFeatureVector(featureVector);
        }
    }

    public void handleCyberReport(FeatureVectorEvent event) {
        List<CyberReport> cyberReports = (List<CyberReport>) event.object;
        FeatureCollection fc = new FeatureCollection();
        List<FeatureVector> features = new ArrayList<>();

        for (CyberReport cyberReport : cyberReports) {
            HashMap<String, String> metaData = new HashMap();
            metaData.put(CyberReport.GROUNDTRUTH, cyberReport.getGroundTruth());
            metaData.put(CyberReport.ADJACENTNETWORK, cyberReport.getAdjacentNetwork());
            metaData.put(CyberReport.INFERENCES, cyberReport.getInferences().toString());
            metaData.put(CyberReport.MOD, cyberReport.getMod().toString());

            FeatureVector sgtaFV = cyberToFeatureVector(CyberReport.SGTA, cyberReport.getsGtA());
            sgtaFV.getMetaData().putAll(metaData);
            //addNewFeatureVector(sgtaFV);
            features.add(sgtaFV);

            FeatureVector sinfgtFV = cyberToFeatureVector(CyberReport.SINFGT, cyberReport.getsInfGt());
            sinfgtFV.getMetaData().putAll(metaData);
            //addNewFeatureVector(sinfgtFV);
            features.add(sinfgtFV);

            FeatureVector sintelaFV = cyberToFeatureVector(CyberReport.SINTELA, cyberReport.getsIntelA());
            sintelaFV.getMetaData().putAll(metaData);
            //addNewFeatureVector(sintelaFV);
            features.add(sintelaFV);

            FeatureVector sintelgtFV = cyberToFeatureVector(CyberReport.SINTELGT, cyberReport.getsIntelGt());
            sintelgtFV.getMetaData().putAll(metaData);
            //addNewFeatureVector(sintelgtFV);
            features.add(sintelgtFV);

            FeatureVector deltaFV = cyberToFeatureVector(CyberReport.DELTA, cyberReport.getDelta());
            deltaFV.getMetaData().putAll(metaData);
            //addNewFeatureVector(deltaFV);
            features.add(deltaFV);
        }
        fc.setFeatures(features);
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(
                new FeatureVectorEvent(
                    FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc, event.object2));
        });
    }

    public void handleFeatureVectorEvent(FeatureVectorEvent event) {
        FeatureVector featureVector = (FeatureVector) event.object;
        if (event.getEventType().equals(FeatureVectorEvent.LOCATE_FEATURE_VECTOR)) {
            for (FeatureVectorRenderer renderer : renderers) {
                renderer.locateFeatureVector(featureVector);
            }
        }
        if (event.getEventType().equals(FeatureVectorEvent.NEW_FEATURE_VECTOR)) {
            addNewFeatureVector(featureVector);
        }
    }

    public void scanLabelsAndLayers(List<FeatureVector> featureVectors) {
        List<FactorLabel> newFactorLabels = new ArrayList<>();
        List<FeatureLayer> newFeatureLayers = new ArrayList<>();

        featureVectors.forEach(featureVector -> {
            if (null != featureVector.getLabel()) {
                //Is the label already added to the local collection?
                if (newFactorLabels.stream().noneMatch(
                    f -> f.getLabel().contentEquals(featureVector.getLabel()))) {
                    //Have we seen this label before?
                    FactorLabel matchingLabel = FactorLabel.getFactorLabel(featureVector.getLabel());

                    //The label doesn't exist in the global map
                    if (null == matchingLabel) {
                        //... add a new FactorLabel to the map
                        if (labelColorIndex > labelColorCount) {
                            labelColorIndex = 0;
                        }
                        //do bulk update using the addAllFactorLabels() method
                        FactorLabel fl = new FactorLabel(featureVector.getLabel(),
                            labelColorMap.getColorByIndex(labelColorIndex));
                        newFactorLabels.add(fl);
                        labelColorIndex++;
                    }
                }
            }
            //Have we seen this layer before?
            int index = featureVector.getLayer();
            FeatureLayer matchingLayer = FeatureLayer.getFeatureLayer(index);
            //The layer is new... add a new FeatureLayer row
            if (null == matchingLayer) {
                if (layerColorIndex > layerColorCount) {
                    layerColorIndex = 0;
                }
                //do bulk update using the addAllFeatureLayers() method
                FeatureLayer fl = new FeatureLayer(index,
                    layerColorMap.getColorByIndex(layerColorIndex));
                newFeatureLayers.add(fl);
                layerColorIndex++;
            }
        });
        if (!newFactorLabels.isEmpty())
            FactorLabel.addAllFactorLabels(newFactorLabels);
        if (!newFeatureLayers.isEmpty())
            FeatureLayer.addAllFeatureLayer(newFeatureLayers);
    }

    public void handleClearAllEvent(FeatureVectorEvent event) {
        LOG.info("Clearing All Feature Vectors By Request.");
        for (FeatureVectorRenderer renderer : renderers) {
            renderer.clearFeatureVectors();
            renderer.refresh(true);
        }
    }

    public void handleFeatureCollectionEvent(FeatureVectorEvent event) {
        FeatureCollection featureCollection = (FeatureCollection) event.object;
        if (null == featureCollection || featureCollection.getFeatures().isEmpty())
            return;
        LOG.info("Imported FeatureCollection size: {}", featureCollection.getFeatures().size());
        LOG.info("Imported FeatureVector width: {}", featureCollection.getFeatures().get(0).getData().size());

        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(
                new CommandTerminalEvent("Scanning Feature Collection for labels...",
                    new Font("Consolas", 20), Color.GREEN));
        });

        //update feature labels and colors
        scanLabelsAndLayers(featureCollection.getFeatures());
        for (FeatureVectorRenderer renderer : renderers) {
            renderer.addFeatureCollection(featureCollection, event.clearExisting);
        }
        //Did the message specify any new dimensional label strings?
        if (null != featureCollection.getDimensionLabels() && !featureCollection.getDimensionLabels().isEmpty()) {
            updateDimensionLabels(featureCollection.getDimensionLabels());
        }
    }

    public void updateDimensionLabels(ArrayList<String> labels) {
        Dimension.removeAllDimensions();
        int counter = 0;
        for (String dimensionLabel : labels) {
            Dimension.addDimension(new Dimension(dimensionLabel,
                counter++, Color.ALICEBLUE));
        }
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.DIMENSION_LABELS_SET, labels));

            App.getAppScene().getRoot().fireEvent(
                new CommandTerminalEvent("New Dimensional Labels set",
                    new Font("Consolas", 20), Color.GREEN));
        });
        //update the renderers with the new arraylist of strings
        for (FeatureVectorRenderer renderer : renderers) {
            renderer.setDimensionLabels(labels);
            renderer.refresh(true);
        }
    }

    public void handleLabelConfigEvent(FeatureVectorEvent event) {
        LabelConfig labelConfig = (LabelConfig) event.object;
        if (null == labelConfig)
            return;
        if (null != labelConfig.isClearAll() && labelConfig.isClearAll()) {
            Platform.runLater(() -> {
                App.getAppScene().getRoot().fireEvent(
                    new CommandTerminalEvent("All Current Labels Cleared.",
                        new Font("Consolas", 20), Color.GREEN));
            });
            FactorLabel.removeAllFactorLabels();
        }
        if (null != labelConfig.getWildcards() && !labelConfig.getWildcards().isEmpty()) {
            Platform.runLater(() -> {
                App.getAppScene().getRoot().fireEvent(
                    new CommandTerminalEvent("Applying Wild Card Pattern Match Color Map...",
                        new Font("Consolas", 20), Color.GREEN));
            });
            //Going through each label in the system, try to apply each wildcard pattern
            //Yes if subsequent patterns match they will overwrite previous matches
            List<FactorLabel> updatedFactorLabels = new ArrayList<>();
            FactorLabel.getFactorLabels().stream().forEach(factorLabel -> {
                labelConfig.getWildcards().forEach((p, c) -> {
                    try {
                        Color parsedColor = Color.valueOf(c);
                        if (LabelConfig.isMatch(factorLabel.getLabel(), p)) {
                            updatedFactorLabels.add(factorLabel);
                            factorLabel.setColor(parsedColor);
                        }
                    } catch (Exception ex) {
                        //Matches....Matches?? We don't need no stinkin Matches!!
                        LOG.info("Could not convert {} : {} to valid pattern map", p, c);
                    }
                });
            });
            if (!updatedFactorLabels.isEmpty())
                Platform.runLater(() -> {
                    App.getAppScene().getRoot().fireEvent(new HyperspaceEvent(
                        HyperspaceEvent.UPDATEDALL_FACTOR_LABELS, updatedFactorLabels));
                });
        }
        //These are explicit label to color maps. This will overwrite any pattern matches
        if (null != labelConfig.getLabels() && !labelConfig.getLabels().isEmpty()) {
            Platform.runLater(() -> {
                App.getAppScene().getRoot().fireEvent(
                    new CommandTerminalEvent("Applying Explicit Label Color Map...",
                        new Font("Consolas", 20), Color.GREEN));
            });
            List<FactorLabel> newFactorLabels = new ArrayList<>();
            labelConfig.getLabels().forEach((l, c) -> {
                try {
                    Color parsedColor = Color.valueOf(c);
                    FactorLabel fl = new FactorLabel(l, parsedColor);
                    newFactorLabels.add(fl);
                } catch (Exception ex) {
                    LOG.info("Could not convert {} : {} to a Factor Label", l, c);
                }
            });
            if (!newFactorLabels.isEmpty())
                FactorLabel.addAllFactorLabels(newFactorLabels);
        }
        //Did the message specify any new dimensional label strings?
        if (null != labelConfig.getDimensionLabels() && !labelConfig.getDimensionLabels().isEmpty()) {
            updateDimensionLabels(labelConfig.getDimensionLabels());
        }
    }

    @Override
    public void handle(FeatureVectorEvent event) {
        if (event.getEventType().equals(FeatureVectorEvent.NEW_FEATURE_VECTOR)
            || event.getEventType().equals(FeatureVectorEvent.LOCATE_FEATURE_VECTOR))
            handleFeatureVectorEvent(event);
        else if (event.getEventType().equals(FeatureVectorEvent.NEW_FEATURE_COLLECTION))
            handleFeatureCollectionEvent(event);
        else if (event.getEventType().equals(FeatureVectorEvent.CLEAR_ALL_FEATUREVECTORS))
            handleClearAllEvent(event);
        else if (event.getEventType().equals(FeatureVectorEvent.NEW_LABEL_CONFIG))
            handleLabelConfigEvent(event);
        else if (event.getEventType().equals(FeatureVectorEvent.RESCAN_FACTOR_LABELS)
            || event.getEventType().equals(FeatureVectorEvent.RESCAN_FEATURE_LAYERS)) {
            labelColorIndex = 0;
            for (FeatureVectorRenderer renderer : renderers) {
                scanLabelsAndLayers(renderer.getAllFeatureVectors());
            }
        } else if (event.getEventType().equals(FeatureVectorEvent.NEW_CYBER_REPORT)) {
            handleCyberReport(event);
        }
    }
}
