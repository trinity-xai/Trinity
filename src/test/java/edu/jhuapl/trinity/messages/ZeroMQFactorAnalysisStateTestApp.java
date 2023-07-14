package edu.jhuapl.trinity.messages;

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
import edu.jhuapl.trinity.data.FactorAnalysisState;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class ZeroMQFactorAnalysisStateTestApp {

    public static final String TOPIC = "ZeroMQ Test Topic";
    public static final int factorListSize = 8;
    public static String PUB_BIND = "tcp://*:5563";

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
                System.out.println("Factor Analysis Publisher starting send loop...");
                while (!Thread.currentThread().isInterrupted()) {
//                    publisher.sendMore(TOPIC);
                    List<Double> factors = new ArrayList<>();
                    double x = Math.sin(update_nbr);
                    double y = Math.cos(update_nbr);
                    double z = Math.atan2(y, x);
                    factors.add(x);
                    factors.add(y);
                    factors.add(z);

                    FactorAnalysisState fas = new FactorAnalysisState("EntityID001", update_nbr, factors);
                    try {
                        String fasAsString = mapper.writeValueAsString(fas);
                        publisher.send(fasAsString);
                    } catch (JsonProcessingException ex) {
                        Logger.getLogger(ZeroMQFactorAnalysisStateTestApp.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    update_nbr++;
                    Thread.sleep(500);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ZeroMQFactorAnalysisStateTestApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, "ZeroMQFactorAnalysisStateTestApp Publisher Thread");
//        thread.setDaemon(true);
        thread.start();

//        MessageProcessor processor = new MessageProcessor();
//        ZeroMQSubscriberConfig config = new ZeroMQSubscriberConfig(
//            "ZeroMQ Subscriber", "Testing ZeroMQFeedManager.",
//            "tcp://localhost:5563", TOPIC, "SomeIDValue", 250);
//        ZeroMQFeedManager feed = new ZeroMQFeedManager(2, config, processor);
//
//        feed.startProcessing();
    }
}
