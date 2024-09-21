package edu.jhuapl.trinity.utils;

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

import edu.jhuapl.trinity.data.messages.ChannelFrame;
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
