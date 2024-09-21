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
import edu.jhuapl.trinity.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

/**
 * @author Sean Phillips
 */
public class ZeroMQChannelFrameTestApp {
    private static final Logger LOG = LoggerFactory.getLogger(ZeroMQChannelFrameTestApp.class);

    public static final String TOPIC = "ZeroMQ Test Topic";
    public static final int channelSize = 100;
    public static final int numberOfSpikes = 5;
    public static final int spikeSize = 30;

    public static void main(String[] args) throws Exception {
        /** Provides deserializaton support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();

        Thread thread = new Thread(() -> {
            LOG.info("Starting Publisher Thread...");
            // Prepare our context and publisher
            try (ZContext context = new ZContext()) {
                Socket publisher = context.createSocket(SocketType.PUB);
                publisher.bind("tcp://*:5563");
                int update_nbr = 0;
                LOG.info("Publisher starting send loop...");
                while (!Thread.currentThread().isInterrupted()) {
//                    publisher.sendMore(TOPIC);
                    ChannelFrame frame = MessageUtils.buildSpikeyChannelFrame(channelSize, 1.0, numberOfSpikes, spikeSize);
                    frame.setEntityId("EntityID001");
                    frame.setFrameId(update_nbr);
                    frame.setDimensionNames(MessageUtils.defaultDimensionNames(channelSize));
                    try {
                        String frameAsString = mapper.writeValueAsString(frame);
                        publisher.send(frameAsString);
                    } catch (JsonProcessingException ex) {
                        LOG.error(null, ex);
                    }

                    update_nbr++;
                    Thread.sleep(500);
                }
            } catch (InterruptedException ex) {
                LOG.error(null, ex);
            }
        }, "ZeroMQFeedManagerTestApp Publisher Thread");
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
