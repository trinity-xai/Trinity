package edu.jhuapl.trinity.messages;

/**
 * @author Sean Phillips
 */
public class ZeroMQSubscriberConfig {

    public static enum CONNECTION {SUBSCRIBER, PULL}

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
