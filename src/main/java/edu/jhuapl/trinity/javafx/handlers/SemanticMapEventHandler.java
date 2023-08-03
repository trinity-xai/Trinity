package edu.jhuapl.trinity.javafx.handlers;

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
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.data.messages.GaussianMixture;
import edu.jhuapl.trinity.data.messages.SemanticMap;
import edu.jhuapl.trinity.data.messages.SemanticMapCollection;
import edu.jhuapl.trinity.javafx.components.ColorMap;
import edu.jhuapl.trinity.javafx.components.timeline.Item;
import edu.jhuapl.trinity.javafx.events.SemanticMapEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.javafx.renderers.FeatureVectorRenderer;
import edu.jhuapl.trinity.javafx.renderers.SemanticMapRenderer;
import edu.jhuapl.trinity.utils.DataUtils;
import javafx.event.EventHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class SemanticMapEventHandler implements EventHandler<SemanticMapEvent> {

    public boolean pcaEnabled;
    List<SemanticMapRenderer> renderers;
    List<FeatureVectorRenderer> fvRenderers;
    ColorMap colorMap;
    int colorIndex = 0;
    int colorCount = 0;

    public SemanticMapEventHandler(boolean pcaEnabled) {
        this.pcaEnabled = pcaEnabled;
        renderers = new ArrayList<>();
        fvRenderers = new ArrayList<>();
        colorMap = ColorMap.jet();
        colorCount = colorMap.getFixedColorCount();
    }

    public void addSemanticMapRenderer(SemanticMapRenderer renderer) {
        renderers.add(renderer);
    }

    public void addFeatureVectorRenderer(FeatureVectorRenderer renderer) {
        fvRenderers.add(renderer);
    }

    public void handleSemanticMapEvent(SemanticMapEvent event) {
        SemanticMap semanticMap = (SemanticMap) event.object;
//        if(event.getEventType().equals(SemanticMapEvent.LOCATE_FEATURE_VECTOR)) {
//            for(SemanticMapRenderer renderer : renderers) {
//                renderer.locateSemanticMap(semanticMap);
//            }
//        }
        if (event.getEventType().equals(SemanticMapEvent.NEW_SEMANTIC_MAP)) {
            for (SemanticMapRenderer renderer : renderers) {
                renderer.addSemanticMap(semanticMap);
            }
        }
    }

    public void handleSemanticMapCollectionEvent(SemanticMapEvent event) {
        App.getAppScene().getRoot().fireEvent(
            new TimelineEvent(TimelineEvent.TIMELINE_CLEAR_ITEMS));
        App.getAppScene().getRoot().fireEvent(
            new TimelineEvent(TimelineEvent.TIMELINE_SET_VISIBLE, true));
        SemanticMapCollection semanticMapCollection = (SemanticMapCollection) event.object;
        System.out.println("Semantic Space data is " +
            semanticMapCollection.getSemantic_space().getData().size() + " x " +
            semanticMapCollection.getSemantic_space().getData().get(0).size());
        System.out.println("Reconstruction data is " +
            semanticMapCollection.getReconstruction().getData_vars()
                .getPrediction().getData().size() + " x " +
            semanticMapCollection.getReconstruction().getData_vars()
                .getPrediction().getData().get(0).size());

        GaussianMixture gm = DataUtils.convertSemanticSpace(
            semanticMapCollection.getSemantic_space(), 10.0);
        System.out.println("GaussianMixture.getData().size(): " + gm.getData().size());

        //20220825T1416
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");
        LocalDateTime startTime = LocalDateTime.parse(
            semanticMapCollection.getReconstruction().getAttrs().getRec(), formatter);
        App.getAppScene().getRoot().fireEvent(
            new TimelineEvent(TimelineEvent.TIMELINE_SET_INITIALTIME, startTime));
        List<FeatureVector> featureVectors = DataUtils.convertSemanticReconstruction(
            semanticMapCollection.getReconstruction());
        //get framerate and set the timelineanimation with it
        //@TODO SMP In the future convert to milliseconds
        int sampleFramerate = semanticMapCollection.getReconstruction()
            .getAttrs().getFramerate().intValue(); //for now truncate to seconds
        App.getAppScene().getRoot().fireEvent(
            new TimelineEvent(TimelineEvent.TIMELINE_SET_SAMPLEFRAMERATE, sampleFramerate));

        List<String> labelStrings = semanticMapCollection.getSemantic_space()
            .getCoords().getFeature_desc().getData();
        System.out.println("FeatureVectors.size(): " + featureVectors.size());
        FeatureCollection fc = new FeatureCollection();
        fc.setFeatures(featureVectors);
        fc.setType(FeatureCollection.TYPESTRING);
        //update labels
        List<FactorLabel> newlabels = new ArrayList<>();
        fc.getFeatures().forEach(featureVector -> {
            //Have we seen this label before?
            //FactorLabel matchingLabel = FactorLabel.getFactorLabel(featureVector.getLabel());
            boolean labelExists = newlabels.stream().anyMatch((FactorLabel t) -> featureVector.getLabel().contentEquals(t.getLabel()));
            //The label is new... add a new FactorLabel row
            if (!labelExists) {
                if (colorIndex > colorCount) {
                    colorIndex = 0;
                }
                FactorLabel fl = new FactorLabel(featureVector.getLabel(),
                    colorMap.getColorByIndex(colorIndex));
                newlabels.add(fl);
                colorIndex++;
            }
        });
        System.out.println("Total New Factor Labels: " + newlabels.size());
        FactorLabel.addAllFactorLabels(newlabels);

        for (FeatureVectorRenderer renderer : fvRenderers) {
            renderer.setDimensionLabels(labelStrings);
            renderer.addFeatureCollection(fc);
            renderer.setSpheroidAnchor(true, 0);
        }

        List<Item> timelineItems = DataUtils.extractReconstructionEvents(
            semanticMapCollection.getReconstruction().getAttrs());
        timelineItems.add(0, new Item(0, "Start"));
        App.getAppScene().getRoot().fireEvent(
            new TimelineEvent(TimelineEvent.TIMELINE_ADD_ITEMS, timelineItems));


        for (SemanticMapRenderer renderer : renderers) {
            renderer.addSemanticMapCollection(semanticMapCollection);
            //@TODO SMP Need to convert to a centralized shared FeatureVector source
            //this is a hack but it gets the labeled features into the view
            renderer.setFeatureCollection(fc);
        }
    }

    @Override
    public void handle(SemanticMapEvent event) {
        if (event.getEventType().equals(SemanticMapEvent.NEW_SEMANTIC_MAP))
            handleSemanticMapEvent(event);
        else if (event.getEventType().equals(SemanticMapEvent.NEW_SEMANTICMAP_COLLECTION))
            handleSemanticMapCollectionEvent(event);
    }
}
