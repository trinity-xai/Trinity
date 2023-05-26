package edu.jhuapl.trinity.messages;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class ZeroMQFeatureVectorTestApp {

    public static final String TOPIC = "ZeroMQ Test Topic";
    public static String PUB_BIND = "tcp://*:5563";
    public static final String TEST_IMAGE_URL = "c:/dev/enceladus-1080p.jpg";

    public static enum LABELS {dog, cat, horse, panda}

    public static void main(String[] args) throws Exception {
        /** Provides serialization support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();

        Thread thread = new Thread(() -> {
            System.out.println("Starting Publisher Thread...");
            // Prepare our context and publisher
            try (ZContext context = new ZContext()) {
                Socket publisher = context.createSocket(SocketType.PUB);
                publisher.bind(PUB_BIND);
                int update_nbr = 0;
                Random rando = new Random();
                System.out.println("Factor Analysis Publisher starting send loop...");
                while (!Thread.currentThread().isInterrupted()) {
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
//                    for(int i=0;i<81;i++)
//                        data.add(Math.cos(i) + rando.nextDouble()*0.25);

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
                        Logger.getLogger(ZeroMQFeatureVectorTestApp.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    update_nbr++;
                    Thread.sleep(50);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ZeroMQFeatureVectorTestApp.class.getName()).log(Level.SEVERE, null, ex);
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
