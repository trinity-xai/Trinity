package edu.jhuapl.trinity.data.messages;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class ZeroMQStressTestApp {
    public static final int BURST_SIZE = 100;  //messages sent per sleep
    public static long MESSAGE_DELAY_MS = 60;
    public static final String TOPIC = "ZeroMQ Test Topic";
    public static String PUSH_CONNECT = "tcp://localhost:5563";

    public static enum LABELS {BeerBottle, Ball, Face, Toy}

    static List<String> imageURLS;
    static Random rando = new Random();

    public static void main(String[] args) throws Exception {
        /** Provides serialization support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();
        imageURLS = new ArrayList<>();
        imageURLS.add("OnyxBeerBottle.jpg");
        imageURLS.add("OnyxFetchBall.jpg");
        imageURLS.add("OnyxHappyFace.jpg");
        imageURLS.add("OnyxToyBasket.jpg");

        Thread thread = new Thread(() -> {
            System.out.println("Starting Publisher Thread...");
            // Prepare our context and publisher
            try (ZContext context = new ZContext()) {
                Socket pusher = context.createSocket(SocketType.PUSH);
                pusher.setSndHWM(0);
                pusher.setSendTimeOut(0);
                pusher.connect(PUSH_CONNECT);
                int update_nbr = 0;

                System.out.println("Factor Analysis Publisher starting send loop...");
                while (!Thread.currentThread().isInterrupted()) {
                    for (int burstIndex = 0; burstIndex < BURST_SIZE; burstIndex++) {
                        try {
                            FeatureVector featureVector = makeFeatureVector(update_nbr);
                            double x = rando.nextGaussian();
                            double y = rando.nextGaussian();
                            double z = rando.nextGaussian();
                            double normalise = 1.0 / (Math.sqrt(x * x + y * y + z * z));
                            x *= normalise * 0.333;
                            y *= normalise * 0.333;
                            z *= normalise * 0.333;
                            featureVector.getData().set(0, x);
                            featureVector.getData().set(1, y);
                            featureVector.getData().set(2, z);
                            String fvAsString = mapper.writeValueAsString(featureVector);
                            pusher.send(fvAsString, ZMQ.DONTWAIT);
                        } catch (JsonProcessingException ex) {
                            Logger.getLogger(ZeroMQStressTestApp.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    System.out.println("Pushed burst number " + update_nbr);
                    update_nbr++;

                    Thread.sleep(MESSAGE_DELAY_MS);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ZeroMQStressTestApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, "ZeroMQFactorAnalysisStateTestApp Publisher Thread");
//        thread.setDaemon(true);
        thread.start();
    }

    private static FeatureVector makeFeatureVector(int update_nbr) {
        FeatureVector featureVector = new FeatureVector();
        featureVector.setComponent("yolov4_hagerstown");
        featureVector.setComponentName("yolov4");
        featureVector.setComponentType("neural_network");

        List<Double> data = new ArrayList<>(81);
        for (int i = 0; i < 81; i++) {
            if (rando.nextBoolean())
                data.add(rando.nextDouble());
            else
                data.add(-rando.nextDouble());
        }
        featureVector.setData(data);
        featureVector.setEntityId("EntityID001");
        featureVector.setFrameId(update_nbr);
        featureVector.setImageId(3101);
        String randomLabel = randomLabel();
        featureVector.setLabel(randomLabel);
        if (randomLabel.contentEquals(LABELS.BeerBottle.name())) {
            featureVector.setImageURL(imageURLS.get(0));
        } else if (randomLabel.contentEquals(LABELS.Ball.name())) {
            featureVector.setImageURL(imageURLS.get(1));
        } else if (randomLabel.contentEquals(LABELS.Face.name())) {
            featureVector.setImageURL(imageURLS.get(2));
        } else //Toy
            featureVector.setImageURL(imageURLS.get(3));

        featureVector.setBbox(randomBBox(1400, 1400, 100, 500));
        featureVector.setMessageId(0);
        featureVector.setTopic("some_topic");
        featureVector.setVersion("0.1");
        return featureVector;
    }

    private static List<Double> randomBBox(int maxX, int maxY, int minWidth, int maxWidth) {
        List<Double> bbox = new ArrayList<>();
        bbox.add(1 + (rando.nextDouble() * maxX / 2.0));
        bbox.add(1 + (rando.nextDouble() * maxY / 2.0));
        double square = 1 + rando.nextDouble() * maxWidth;
        if (square < minWidth)
            square = minWidth;
        bbox.add(bbox.get(0) + square);
        bbox.add(bbox.get(1) + square);
        return bbox;
    }

    private static String randomImage() {
        Random rando = new Random();
        return imageURLS.get(rando.nextInt(imageURLS.size()));
    }

    private static String randomLabel() {
        Random rando = new Random();
        return LABELS.values()[rando.nextInt(LABELS.values().length)].name();
    }
}
