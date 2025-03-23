/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.handlers;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.FilterSet;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageData;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import static edu.jhuapl.trinity.data.messages.xai.FeatureVector.mapToStateArray;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.SearchEvent;
import edu.jhuapl.trinity.javafx.renderers.FeatureVectorRenderer;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import static edu.jhuapl.trinity.messages.RestAccessLayer.currentEmbeddingsModel;
import edu.jhuapl.trinity.utils.metric.Metric;
import java.io.IOException;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.util.Pair;

/**
 * @author Sean Phillips
 */
public class SearchEventHandler implements EventHandler<SearchEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SearchEventHandler.class);

    List<FeatureVectorRenderer> renderers;
    AtomicInteger requestNumber;

    public SearchEventHandler() {
        renderers = new ArrayList<>();
        requestNumber = new AtomicInteger();
    }

    public void addFeatureVectorRenderer(FeatureVectorRenderer renderer) {
        renderers.add(renderer);
    }

    public void handleClearAllFilters(SearchEvent event) {
        LOG.info("Clearing Filters... ");
        for (FeatureVectorRenderer renderer : renderers) {
            List<FeatureVector> fvList = renderer.getAllFeatureVectors();
            int size = fvList.size();
            for (int i = 0; i < size; i++) {
                renderer.setVisibleByIndex(i, true);
            }
            //request render update
            renderer.refresh(true);
        }
        LOG.info("Complete.");
    }

    public void handleFilterByTerm(SearchEvent event) {
        String metadataTerm = (String) event.eventObject;
        LOG.info("Filtering based on {}... ", metadataTerm);
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
            renderer.refresh(true);
        }
        LOG.info("Filtering complete.");
    }

    public void handleFilterByScore(SearchEvent event) {
        FilterSet filterSet = (FilterSet) event.eventObject;
        String msg = "Score Filtering based on " + filterSet.toString() + "... ";
        App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent(msg,
            new Font("Consolas", 20), Color.GREEN));
        LOG.info(msg);

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
            renderer.refresh(true);
            msg = filteredCount + " featureVectors filtered.";
            LOG.info(msg);
            App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent(msg));
        }
    }

    public void handleFilterByProbability(SearchEvent event) {
        FilterSet filterSet = (FilterSet) event.eventObject;
        String msg = "Probability Filtering based on " + filterSet.toString() + "... ";
        App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent(msg,
            new Font("Consolas", 20), Color.GREEN));
        LOG.info(msg);

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
            renderer.refresh(true);
            msg = filteredCount + " featureVectors filtered.";
            LOG.info(msg);
            App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent(msg));
        }
    }
    public void handleFindByQuery(SearchEvent event) {
        String query = (String) event.eventObject;
        String msg = "Finding closest vector matches to '" + query + "'... ";
        App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent(msg,
            new Font("Consolas", 20), Color.GREEN));
        LOG.info(msg);
        System.out.println("Requesting embedding vector for query...");
        try {
            EmbeddingsImageInput input = EmbeddingsImageInput.defaultTextInput(query);
            if(null != currentEmbeddingsModel)
                input.setModel(currentEmbeddingsModel);
            List<Integer> inputIDs = new ArrayList<>();
            inputIDs.add(-1);
            RestAccessLayer.requestQueryTextEmbeddings(
                input, App.getAppScene(), inputIDs, requestNumber.getAndIncrement());
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
    }
    public void handleQueryEmbeddingsResponse(SearchEvent event) {
        EmbeddingsImageOutput output = (EmbeddingsImageOutput) event.eventObject;
        System.out.println("Query Embedding Response... " + output.getData().get(0).getType());
        EmbeddingsImageData currentOutput = output.getData().get(0);
        //use a stream to convert the Boxed Double list to a primitive array
        double [] queryVector = currentOutput.getEmbedding().stream().mapToDouble(d->d).toArray();
        System.out.println("Computing Landmark Simularity Distances...");
        Metric metric = Metric.getMetric("cosine");
        long startTime = System.nanoTime();
        for (FeatureVectorRenderer renderer : renderers) {
            List<FeatureVector> fvList = renderer.getAllFeatureVectors();
            int size = fvList.size();
            Double shortestDistance = null;
            Integer shortestVectorIndex = null;
            //First calculate all the distances and build a list map against their indices
            ArrayList<Pair<Integer,Double>> indexDistanceRecords = new ArrayList<>();
            for(int i=0;i<size;i++){
                double[] itemVector = mapToStateArray.apply(fvList.get(i));
                double currentDistance = metric.distance(itemVector,queryVector );
                if(null == shortestDistance || currentDistance < shortestDistance) {
                    shortestDistance = currentDistance;
                    shortestVectorIndex = i;
                }
                indexDistanceRecords.add(new Pair<>(i,currentDistance));
            }
            //sort the list map by distance, closest to farthest
            List<Pair<Integer,Double>> sortedList = indexDistanceRecords.stream()
                .sorted(indexDistancePairCompare)
                .toList();
            //@DEBUG SMP 
            System.out.println("The shortest distance FV is index: " + shortestVectorIndex);
            System.out.println("The distance to the query vector is: " + shortestDistance);
            System.out.println("Here is the ranked distance list:");
            sortedList.forEach(p -> {
                System.out.println("FV Index: " + p.getKey() + " with score: " + p.getValue());
            });
            //grey out everything outside the closest ten
            for(int i=11; i<size;i++) {
                renderer.setColorByIndex(sortedList.get(i).getKey()+1, Color.GRAY);
            }

            //request render update
            renderer.refresh(false);
            String msg = "Query on featureVectors executed.";
            LOG.info(msg);
            App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent(msg));
        }
    }
    public static Comparator<Pair<Integer, Double>> indexDistancePairCompare = 
        (Pair<Integer, Double> p1, Pair<Integer, Double> p2) -> {
        if (p1.getValue() < p2.getValue()) return -1;
        else if (p1.getValue() > p2.getValue()) return 1;
        else return 0;
    };
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
        else if (event.getEventType().equals(SearchEvent.FIND_BY_QUERY))
            handleFindByQuery(event);
        else if (event.getEventType().equals(SearchEvent.QUERY_EMBEDDINGS_RESPONSE))
            handleQueryEmbeddingsResponse(event);
    }
}
