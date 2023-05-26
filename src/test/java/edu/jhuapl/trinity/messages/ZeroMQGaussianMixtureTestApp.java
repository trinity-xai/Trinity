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
import edu.jhuapl.trinity.data.messages.GaussianMixture;
import edu.jhuapl.trinity.data.messages.GaussianMixtureData;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
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
public class ZeroMQGaussianMixtureTestApp {

    public static final String TOPIC = "ZeroMQ Test Topic";
    public static String PUSH_CONNECT = "tcp://localhost:5563";

    //    public static enum LABELS {dog, cat, horse, panda}
    public static enum LABELS {BeerBottle, Ball, Face, Toy}

    public static int NUMBER_FEATURES = 81;
    public static int NUMBER_DATA = 4;

    public static void main(String[] args) throws Exception {
        /** Provides serialization support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();

        Thread thread = new Thread(() -> {
            System.out.println("Starting Publisher Thread...");
            // Prepare our context and publisher
            try (ZContext context = new ZContext()) {
                Socket pusher = context.createSocket(SocketType.PUSH);
                pusher.setSendTimeOut(10);
                pusher.connect(PUSH_CONNECT);
                int update_nbr = 0;
                System.out.println("Gaussian Mixutre Publisher starting send loop...");
//                while (!Thread.currentThread().isInterrupted()) {
                GaussianMixture gaussianMixture = new GaussianMixture();
                gaussianMixture.setComponent("yolov4_hagerstown");
                gaussianMixture.setComponentName("yolov4");
                gaussianMixture.setComponentType("neural_network");

                //List<GaussianMixtureData> data = fixedGaussianMixtureData();
//                    List<GaussianMixtureData> data = singleGaussianMixtureData();

                List<GaussianMixtureData> data = randomGaussianMixtureData(
                    NUMBER_DATA, NUMBER_FEATURES, 1.0, 0.55);
                gaussianMixture.setData(data);
                gaussianMixture.setNumComponents(NUMBER_DATA);
                gaussianMixture.setNumFeatures(NUMBER_FEATURES);
                gaussianMixture.setCovarianceMode("diag");
                gaussianMixture.setEntityId("EntityID001");
//                    gaussianMixture.setLabel(randomLabel());
                gaussianMixture.setLabel("BeerBottle");
                gaussianMixture.setMessageId(0);
                gaussianMixture.setTopic("some_topic");
                gaussianMixture.setVersion("0.1");
                try {
                    String fvAsString = mapper.writeValueAsString(gaussianMixture);
                    pusher.send(fvAsString, ZMQ.DONTWAIT);
                    System.out.println(fvAsString);
                } catch (JsonProcessingException ex) {
                    Logger.getLogger(ZeroMQGaussianMixtureTestApp.class.getName()).log(Level.SEVERE, null, ex);
                }

                update_nbr++;
                Thread.sleep(500);
//                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ZeroMQGaussianMixtureTestApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, "ZeroMQFactorAnalysisStateTestApp Publisher Thread");
//        thread.setDaemon(true);
        thread.start();
    }

    private static List<GaussianMixtureData> singleGaussianMixtureData() {
        List<GaussianMixtureData> dataList = new ArrayList<>();
//
//        //upper right far quadrant
        GaussianMixtureData data = new GaussianMixtureData();
//        for(int j=0; j<10; j++) {
//            data.getMean().add(0.5);
//            data.getMean().add(0.75);
//            data.getMean().add(0.5);
//            data.getCovariance().add(0.25);
//            data.getCovariance().add(0.15);
//            data.getCovariance().add(0.2);
//        }
//        dataList.add(data);

//        //upper left far quadrant
//        data = new GaussianMixtureData();
//        for(int j=0; j<10; j++) {
//            data.getMean().add(-0.5);
//            data.getMean().add(0.333);
//            data.getMean().add(0.45);
//            data.getCovariance().add(0.25);
//            data.getCovariance().add(0.35);
//            data.getCovariance().add(0.1);        }
//        dataList.add(data);

//        //lower right close quadrant
//        data = new GaussianMixtureData();
//        for(int j=0; j<10; j++) {
//            data.getMean().add(0.5);
//            data.getMean().add(-0.533);
//            data.getMean().add(-0.65);
//            data.getCovariance().add(0.55);
//            data.getCovariance().add(0.35);
//            data.getCovariance().add(0.7);
//        }
//        dataList.add(data);
//
        //lower left close quadrant
        data = new GaussianMixtureData();
        for (int j = 0; j < 10; j++) {
            data.getMean().add(-0.8);
            data.getMean().add(-0.733);
            data.getMean().add(-0.65);

            ArrayList<Double> list = new ArrayList<>();
            Collections.addAll(list, 0.55, 0.45, 0.37);
            data.getCovariance().add(list);
        }
        dataList.add(data);

        return dataList;
    }

    private static List<GaussianMixtureData> fixedGaussianMixtureData() {
        List<GaussianMixtureData> dataList = new ArrayList<>();

        //upper right far quadrant
        GaussianMixtureData data = new GaussianMixtureData();
        for (int j = 0; j < 10; j++) {
            data.getMean().add(1.0);
            data.getMean().add(1.0);
            data.getMean().add(1.0);
            ArrayList<Double> list = new ArrayList<>();
            Collections.addAll(list, 1.0, 1.0, 1.0);
            data.getCovariance().add(list);
        }
        dataList.add(data);

        //upper left far quadrant
        data = new GaussianMixtureData();
        for (int j = 0; j < 10; j++) {
            data.getMean().add(-1.0);
            data.getMean().add(1.0);
            data.getMean().add(1.0);
            ArrayList<Double> list = new ArrayList<>();
            Collections.addAll(list, 1.0, 1.0, 1.0);
            data.getCovariance().add(list);
        }
        dataList.add(data);

        //lower right close quadrant
        data = new GaussianMixtureData();
        for (int j = 0; j < 10; j++) {
            data.getMean().add(1.0);
            data.getMean().add(-1.0);
            data.getMean().add(-1.0);
            ArrayList<Double> list = new ArrayList<>();
            Collections.addAll(list, 1.0, 1.0, 1.0);
            data.getCovariance().add(list);
        }
        dataList.add(data);

        //lower left close quadrant
        data = new GaussianMixtureData();
        for (int j = 0; j < 10; j++) {
            data.getMean().add(-1.0);
            data.getMean().add(-1.0);
            data.getMean().add(-1.0);
            ArrayList<Double> list = new ArrayList<>();
            Collections.addAll(list, 1.0, 1.0, 1.0);
            data.getCovariance().add(list);
        }
        dataList.add(data);

        return dataList;
    }

    private static List<GaussianMixtureData> randomGaussianMixtureData(
        int dataCount, int featureCount, double maxMean, double maxCovariance) {
        List<GaussianMixtureData> dataList = new ArrayList<>();
        Random rando = new Random();
        for (int i = 0; i < dataCount; i++) {
            GaussianMixtureData data = new GaussianMixtureData();
            ArrayList<Double> covList = new ArrayList<>();

            for (int j = 0; j < featureCount; j++) {
                if (rando.nextBoolean())
                    data.getMean().add(rando.nextDouble() * maxMean);
                else
                    data.getMean().add(-rando.nextDouble() * maxMean);

                if (rando.nextBoolean())
                    covList.add((rando.nextDouble() + 0.1) * maxCovariance);
                else
                    covList.add((-rando.nextDouble() + 0.1) * maxCovariance);
            }
            data.getCovariance().add(covList);
            dataList.add(data);
        }
        return dataList;
    }

    private static String randomLabel() {
        Random rando = new Random();
        return LABELS.values()[rando.nextInt(LABELS.values().length)].name();
    }
}
