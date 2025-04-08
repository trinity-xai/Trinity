/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Sean Phillips
 */
public class ZeroMQFeatureVectorTestApp {
    private static final Logger LOG = LoggerFactory.getLogger(ZeroMQFeatureVectorTestApp.class);

    public static final String TOPIC = "ZeroMQ Test Topic";
    public static String PUB_BIND = "tcp://*:5563";
    public static final String TEST_IMAGE_URL = "c:/dev/enceladus-1080p.jpg";

    public static enum LABELS {dog, cat, horse, panda}

    public static void main(String[] args) throws Exception {
        /** Provides serialization support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();

        Thread thread = new Thread(() -> {
            LOG.info("Starting Publisher Thread...");
            // Prepare our context and publisher
            try (ZContext context = new ZContext()) {
                Socket publisher = context.createSocket(SocketType.PUB);
                publisher.bind(PUB_BIND);
                int update_nbr = 0;
                Random rando = new Random();
                LOG.info("Factor Analysis Publisher starting send loop...");
                while (!Thread.currentThread().isInterrupted()) {
                    FeatureVector featureVector = new FeatureVector();
                    featureVector.setComponent("yolov4_hagerstown");
                    featureVector.setComponentName("yolov4");
                    featureVector.setComponentType("neural_network");

                    List<Double> data = new ArrayList<>(81);
                    for (int i = 0; i < 512; i++) {
                        data.add(rando.nextGaussian());
                    }
                    featureVector.setData(data);
                    featureVector.setEntityId("EntityID001");
                    featureVector.setFrameId(update_nbr);
                    featureVector.setImageId(3101);
                    featureVector.setImageURL(TEST_IMAGE_URL);
                    featureVector.setLabel(randomLabel());
                    List<Double> bbox = new ArrayList<>();
                    Collections.addAll(bbox, 252.0, 447.75, 9.0, 9.0);
                    featureVector.setBbox(bbox);
                    featureVector.setMessageId(0);
                    featureVector.setTopic("some_topic");
                    featureVector.setVersion("0.1");
                    try {
                        String fvAsString = mapper.writeValueAsString(featureVector);
                        publisher.send(fvAsString);
                    } catch (JsonProcessingException ex) {
                        LOG.error(null, ex);
                    }

                    update_nbr++;
                    Thread.sleep(16);
                }
            } catch (InterruptedException ex) {
                LOG.error(null, ex);
            }
        }, "ZeroMQFactorAnalysisStateTestApp Publisher Thread");
//        thread.setDaemon(true);
        thread.start();
    }

    private static String randomLabel() {
        Random rando = new Random();
        return LABELS.values()[rando.nextInt(LABELS.values().length)].name();
    }
}
