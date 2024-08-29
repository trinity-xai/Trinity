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

/**
 * @author Sean Phillips
 */
public class ZeroMQSubscriberConfig {

    public static enum CONNECTION {SUBSCRIBER, PULL};

    public static final String DEFAULT_NAME = "ZeroMQ";
    public static final String DEFAULT_DESCRIPTION = "Default";
    public static final String DEFAULT_HOST = "tcp://localhost:5563";
    public static final String DEFAULT_PULL = "tcp://*:5563";
    public static final String DEFAULT_TOPIC = "subcriber";
    public static final String DEFAULT_GROUPID = "ZeroMQGroupId";
    public static final Integer DEFAULT_UPDATE_RATE_MS = 250;
    public String name;
    public String description;
    public String host;
    public String topic;
    public String groupID;
    public Integer updateRateMS;
    public CONNECTION connection = CONNECTION.SUBSCRIBER;

    public ZeroMQSubscriberConfig() {
        this(DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_HOST,
            DEFAULT_TOPIC, DEFAULT_GROUPID, DEFAULT_UPDATE_RATE_MS);
    }

    public ZeroMQSubscriberConfig(String name, String description, String host,
                                  String topic, String groupID, Integer updateRateMS) {
        this.name = name;
        this.description = description;
        this.host = host;
        this.topic = topic;
        this.groupID = groupID;
        this.updateRateMS = updateRateMS;
    }
}
