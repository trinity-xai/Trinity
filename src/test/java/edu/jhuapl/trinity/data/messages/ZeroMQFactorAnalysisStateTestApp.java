/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.FactorAnalysisState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class ZeroMQFactorAnalysisStateTestApp {
    private static final Logger LOG = LoggerFactory.getLogger(ZeroMQFactorAnalysisStateTestApp.class);

    public static final String TOPIC = "ZeroMQ Test Topic";
    public static final int factorListSize = 8;
    public static String PUB_BIND = "tcp://*:5563";

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
                LOG.info("Factor Analysis Publisher starting send loop...");
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
                        LOG.error(null, ex);
                    }

                    update_nbr++;
                    Thread.sleep(500);
                }
            } catch (InterruptedException ex) {
                LOG.error(null, ex);
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
