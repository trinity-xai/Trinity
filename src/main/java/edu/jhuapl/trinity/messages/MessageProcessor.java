/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.FactorAnalysisState;
import edu.jhuapl.trinity.data.messages.CommandRequest;
import edu.jhuapl.trinity.data.messages.bci.ChannelFrame;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.data.messages.xai.GaussianMixture;
import edu.jhuapl.trinity.data.messages.xai.LabelConfig;
import edu.jhuapl.trinity.data.messages.xai.UmapConfig;
import edu.jhuapl.trinity.javafx.events.ChannelFrameDataEvent;
import edu.jhuapl.trinity.javafx.events.FactorAnalysisDataEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.GaussianMixtureEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sean Phillips
 */
public class MessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessor.class);
    AtomicInteger ai;
    /**
     * Provides deserializaton support for JSON messages
     */
    private ObjectMapper mapper;
    /**
     * Scene reference that enables the processor to route data to events
     */
    private Scene scene;

    public MessageProcessor() {
        this(null);
    }

    public MessageProcessor(Scene scene) {
        this.scene = scene;
        ai = new AtomicInteger();
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // force serialization of timestamps as ISO-8601 standard. Assumption is most formats are ISO-8601 standard.
        // Also used later during conversion from objects by jackson.
//        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
//        mapper.registerModule(new JavaTimeModule());
    }

    public void process(String message) throws JsonProcessingException, IOException {
        if (CommandRequest.isCommandRequest(message)) {
            CommandRequest command = mapper.readValue(message, CommandRequest.class);
            CommandTask commandTask = new CommandTask(command, scene);
            Thread t = new Thread(commandTask, "Trinity Command Task " + ai.incrementAndGet());
            t.setDaemon(true);
            t.start();
        }
        if (FeatureCollection.isFeatureCollection(message)) {
            FeatureCollection featureCollection = mapper.readValue(message, FeatureCollection.class);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new FeatureVectorEvent(
                    FeatureVectorEvent.NEW_FEATURE_COLLECTION, featureCollection));
            });
        } else if (FeatureVector.isFeatureVector(message)) {
            FeatureVector featureVector = getMapper().readValue(message, FeatureVector.class);
            //fire event to load data in JavaFX Scene
            scene.getRoot().fireEvent(new FeatureVectorEvent(
                FeatureVectorEvent.NEW_FEATURE_VECTOR, featureVector));
        } else if (GaussianMixture.isGaussianMixture(message)) {
            GaussianMixture gaussianMixture = getMapper().readValue(message, GaussianMixture.class);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new GaussianMixtureEvent(
                    GaussianMixtureEvent.NEW_GAUSSIAN_MIXTURE, gaussianMixture));
            });
        } else if (LabelConfig.isLabelConfig(message)) {
            LabelConfig labelConfig = getMapper().readValue(message, LabelConfig.class);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new FeatureVectorEvent(
                    FeatureVectorEvent.NEW_LABEL_CONFIG, labelConfig));
            });
        } else if (UmapConfig.isUmapConfig(message)) {
            UmapConfig umapConfig = getMapper().readValue(message, UmapConfig.class);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.NEW_UMAP_CONFIG, umapConfig));
            });
        } else if (ChannelFrame.isChannelFrame(message)) {
            ChannelFrame frame = getMapper().readValue(message, ChannelFrame.class);
            LOG.info("Frame: {}", frame.getFrameId());
            LOG.info("Channel Values: {}", frame.getChannelData());
            //@fire event to load data in JavaFX Scene
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new ChannelFrameDataEvent(frame));
            });
        } else if (FactorAnalysisState.isFactorAnalysisState(message)) {
            FactorAnalysisState fas = getMapper().readValue(message, FactorAnalysisState.class);
            LOG.info("Frame ID: {}", fas.getFrameId());
            LOG.info("Factor Values: {}", fas.getFactors());
            //@fire event to load data in JavaFX Scene
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new FactorAnalysisDataEvent(fas));
            });
        }
        if (message.equalsIgnoreCase("howdy")) {
            LOG.info("Well hello...");
        }
    }

    /**
     * @return the mapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * @param mapper the mapper to set
     */
    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * @return the scene
     */
    public Scene getScene() {
        return scene;
    }

    /**
     * @param scene the scene to set
     */
    public void setScene(Scene scene) {
        this.scene = scene;
    }
}
