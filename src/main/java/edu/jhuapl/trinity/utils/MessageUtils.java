package edu.jhuapl.trinity.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.bci.ChannelFrame;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageData;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Sean Phillips
 */
public enum MessageUtils {
    INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(MessageUtils.class);
    static AtomicLong atomicLong = new AtomicLong();

    public static boolean probablyJSON(String possibleJson) {
        return (possibleJson.startsWith("{") && possibleJson.endsWith("}"))
            || (possibleJson.startsWith("[") && possibleJson.endsWith("]"));
    }
    public static boolean probablyCSV(String filename) {
        return filename.endsWith("csv") || filename.endsWith("CSV");
    }

    public static boolean isJSONValid(String jsonInString) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonInString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static Function<EmbeddingsImageData, FeatureVector> embeddingsToFeatureVector = d -> {
        FeatureVector fv = new FeatureVector();
        fv.getData().addAll(d.getEmbedding());
        fv.setMessageId(d.getIndex());
        fv.getMetaData().put("object", d.getObject());
        fv.getMetaData().put("type", d.getType());
        return fv;
    };

    public static void injectFeatureCollection(Scene scene, String message) {
        /** Provides deserialization support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            FeatureCollection featureCollection = mapper.readValue(message, FeatureCollection.class);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new FeatureVectorEvent(
                    FeatureVectorEvent.NEW_FEATURE_COLLECTION, featureCollection));
            });
        } catch (JsonProcessingException ex) {
            LOG.error(null, ex);
        }
    }

    public static ChannelFrame buildSpikeyChannelFrame(int totalSize, double spikeHeightCap, int numberOfSpikes, int spikeSize) {
        ChannelFrame frame = new ChannelFrame(totalSize);
        if (spikeSize > totalSize)
            spikeSize = totalSize;
        Random rando = new Random();
        for (int i = 0; i < numberOfSpikes; i++) {
            //generate a new random gaussian spike
            List<Double> gaussianSpike = pureSortedGaussians(spikeSize);
            //generate a new random spike start index
            int spikeIndex = rando.nextInt(totalSize);
            //additively merge spike into frame
            for (Double d : gaussianSpike) {
                //get the currently indexed value
                double currentValue = frame.getChannelData().get(spikeIndex);
                //add the gaussianSpike value
                currentValue += d;
                //Cap at parameter (usually 1.0)... creates saturation effect
                if (currentValue > spikeHeightCap)
                    currentValue = spikeHeightCap;
                frame.getChannelData().set(spikeIndex, currentValue);
                //increment our current index
                spikeIndex++;
                //don't allow ourselves to index past our actual frame size
                if (spikeIndex >= totalSize)
                    break; //move on to the next randome spike grouping
            }
        }
        return frame;
    }

    public static ChannelFrame randomGaussianChannelFrame(String entityId, int size) {
        List<Double> gaussians = pureSortedGaussians(size);
        List<String> defaultDimensionNames = defaultDimensionNames(size);
        return new ChannelFrame(entityId, atomicLong.getAndIncrement(),
            gaussians, defaultDimensionNames);
    }

    public static List<String> defaultDimensionNames(int size) {
        List<String> dimensionNames = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            dimensionNames.add("Dimension " + i);
        return dimensionNames;
    }

    public static List<Double> pureSortedGaussians(int size) {
        //Generate a randonm list of gaussian doubles between -1.0 and 1.0
        Random rando = new Random();
        List<Double> gaussians = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            double nextG = rando.nextGaussian() / 2.0;
            //cap everything to -1 to 1.
            if (nextG < -1)
                nextG = -1;
            else if (nextG > 1)
                nextG = 1;
            gaussians.add(nextG);
        }
        //sort by natural ordering
        gaussians.sort(null);
        return gaussians.stream()
            .map((Double t) -> 1 - Math.abs(t))
            .collect(Collectors.toList());
    }

    public static ChannelFrame buildSpikeyPositiveChannelFrame(int totalSize, double spikeHeightCap, int numberOfSpikes, int spikeSize) {
        ChannelFrame frame = new ChannelFrame(totalSize);
        if (spikeSize > totalSize)
            spikeSize = totalSize;
        Random rando = new Random();
        for (int i = 0; i < numberOfSpikes; i++) {
            //generate a new random gaussian spike
            List<Double> doublesSpike = pureSortedDoubles(spikeSize);
            //generate a new random spike start index
            int spikeIndex = rando.nextInt(totalSize);
            //additively merge spike into frame
            for (Double d : doublesSpike) {
                //get the currently indexed value
                double currentValue = frame.getChannelData().get(spikeIndex);
                //add the spike value
                currentValue += d;
                //Cap at parameter (usually 1.0)... creates saturation effect
                if (currentValue > spikeHeightCap)
                    currentValue = spikeHeightCap;
                frame.getChannelData().set(spikeIndex, currentValue);
                //increment our current index
                spikeIndex++;
                //don't allow ourselves to index past our actual frame size
                if (spikeIndex >= totalSize)
                    break; //move on to the next randome spike grouping
            }
        }
        return frame;
    }

    public static List<Double> pureSortedDoubles(int size) {
        //Generate a randonm list of gaussian doubles between -1.0 and 1.0
        Random rando = new Random();
        List<Double> doubles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            double nextG = rando.nextDouble();
            doubles.add(nextG);
        }
        //sort by natural ordering
        doubles.sort(null);
        return doubles;
    }
}
