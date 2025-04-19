package edu.jhuapl.trinity.utils;

import edu.jhuapl.trinity.data.messages.bci.ChannelFrame;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Sean Phillips
 */
public class MessageUtilsTest {
    private static final Logger LOG = LoggerFactory.getLogger(MessageUtilsTest.class);

    public MessageUtilsTest() {
    }

    /**
     * Test of randomGaussianChannelFrame method, of class MessageUtils.
     */
    @Test
    public void testBuildSpikeyChannelFrame() {
        LOG.info("buildSpikeyChannelFrame");
        String entityId = "";
        int size = 0;
        ChannelFrame expResult = null;
        ChannelFrame result = MessageUtils.buildSpikeyChannelFrame(100, 1.0, 5, 30);
        for (Double d : result.getChannelData()) {
            System.out.println(d);
        }
    }

    /**
     * Test of pureSortedGaussians method, of class MessageUtils.
     */
    //@Test
    public void testPureSortedGaussians() {
        LOG.info("pureSortedGaussians");
        int size = 10;
        List<Double> expResult = null;
        List<Double> result = MessageUtils.pureSortedGaussians(size);
        System.out.println(result);
    }
}
