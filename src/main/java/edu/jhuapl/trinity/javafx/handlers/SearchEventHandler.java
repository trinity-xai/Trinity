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
package edu.jhuapl.trinity.javafx.handlers;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.FilterSet;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.SearchEvent;
import edu.jhuapl.trinity.javafx.renderers.FeatureVectorRenderer;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class SearchEventHandler implements EventHandler<SearchEvent> {

    List<FeatureVectorRenderer> renderers;

    public SearchEventHandler() {
        renderers = new ArrayList<>();
    }

    public void addFeatureVectorRenderer(FeatureVectorRenderer renderer) {
        renderers.add(renderer);
    }

    public void handleClearAllFilters(SearchEvent event) {
        System.out.print("Clearing Filters... ");
        for (FeatureVectorRenderer renderer : renderers) {
            List<FeatureVector> fvList = renderer.getAllFeatureVectors();
            int size = fvList.size();
            for (int i = 0; i < size; i++) {
                renderer.setVisibleByIndex(i, true);
            }
            //request render update
            renderer.refresh();
        }
        System.out.println("Complete.");
    }

    public void handleFilterByTerm(SearchEvent event) {
        String metadataTerm = (String) event.eventObject;
        System.out.print("Filtering based on " + metadataTerm + "... ");
        for (FeatureVectorRenderer renderer : renderers) {
            List<FeatureVector> fvList = renderer.getAllFeatureVectors();
            int size = fvList.size();
            for (int i = 0; i < size; i++) {
                if (!fvList.get(i).metaContainsTerm(metadataTerm))
                    renderer.setVisibleByIndex(i, false);
//@DEBUG SMP useful print... but it is very slow
//                else
//                    System.out.println("Index: " + i + " | " + fvList.get(i).metadataAsString(" "));
            }
            //request render update
            renderer.refresh();
        }
        System.out.println("Filtering complete.");
    }

    public void handleFilterByScore(SearchEvent event) {
        FilterSet filterSet = (FilterSet) event.eventObject;
        String msg = "Score Filtering based on " + filterSet.toString() + "... ";
        App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent(msg,
            new Font("Consolas", 20), Color.GREEN));
        System.out.print(msg);

        for (FeatureVectorRenderer renderer : renderers) {
            List<FeatureVector> fvList = renderer.getAllFeatureVectors();
            int size = fvList.size();
            int filteredCount = 0;
            double score;
            for (int i = 0; i < size; i++) {
                score = fvList.get(i).getScore();
                if (score < filterSet.minimum.doubleValue()
                    || score > filterSet.maximum.doubleValue()) {
                    renderer.setVisibleByIndex(i, false);
                    filteredCount++;
                }
            }
            //request render update
            renderer.refresh();
            msg = filteredCount + " featureVectors filtered.";
            System.out.println(msg);
            App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent(msg));
        }
    }

    public void handleFilterByProbability(SearchEvent event) {
        FilterSet filterSet = (FilterSet) event.eventObject;
        String msg = "Probability Filtering based on " + filterSet.toString() + "... ";
        App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent(msg,
            new Font("Consolas", 20), Color.GREEN));
        System.out.print(msg);

        for (FeatureVectorRenderer renderer : renderers) {
            List<FeatureVector> fvList = renderer.getAllFeatureVectors();
            int size = fvList.size();
            int filteredCount = 0;
            double probability;
            for (int i = 0; i < size; i++) {
                probability = fvList.get(i).getPfa();
                if (probability < filterSet.minimum.doubleValue()
                    || probability > filterSet.maximum.doubleValue()) {
                    renderer.setVisibleByIndex(i, false);
                    filteredCount++;
                }
            }
            //request render update
            renderer.refresh();
            msg = filteredCount + " featureVectors filtered.";
            System.out.println(msg);
            App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent(msg));
        }
    }

    @Override
    public void handle(SearchEvent event) {
        if (event.getEventType().equals(SearchEvent.FILTER_BY_TERM))
            handleFilterByTerm(event);
        else if (event.getEventType().equals(SearchEvent.CLEAR_ALL_FILTERS))
            handleClearAllFilters(event);
        else if (event.getEventType().equals(SearchEvent.FILTER_BY_SCORE))
            handleFilterByScore(event);
        else if (event.getEventType().equals(SearchEvent.FILTER_BY_PROBABILITY))
            handleFilterByProbability(event);
    }
}
