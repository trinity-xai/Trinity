package edu.jhuapl.trinity.utils;

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

import edu.jhuapl.trinity.data.messages.ChannelFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author Sean Phillips
 */
public enum MessageUtils {
    INSTANCE;
    static AtomicLong atomicLong = new AtomicLong();

    public static ChannelFrame buildSpikeyChannelFrame(int totalSize, double spikeHeightCap, int numberOfSpikes, int spikeSize) {
        ChannelFrame frame = new ChannelFrame(totalSize);
        if (spikeSize > totalSize)
            spikeSize = totalSize;
        Random rando = new Random();
        for (int i = 0; i < numberOfSpikes; i++) {
            //generate a new random gaussian spike
            List<Double> gaussianSpike = pureSortedGaussians(spikeSize);
            //generate a new random spike start index
            int spikeIndex = rando.nextInt(totalSize);
            //additively merge spike into frame
            for (Double d : gaussianSpike) {
                //get the currently indexed value
                double currentValue = frame.getChannelData().get(spikeIndex);
                //add the gaussianSpike value
                currentValue += d;
                //Cap at parameter (usually 1.0)... creates saturation effect
                if (currentValue > spikeHeightCap)
                    currentValue = spikeHeightCap;
                frame.getChannelData().set(spikeIndex, currentValue);
                //increment our current index
                spikeIndex++;
                //don't allow ourselves to index past our actual frame size
                if (spikeIndex >= totalSize)
                    break; //move on to the next randome spike grouping
            }
        }
        return frame;
    }

    public static ChannelFrame randomGaussianChannelFrame(String entityId, int size) {
        List<Double> gaussians = pureSortedGaussians(size);
        List<String> defaultDimensionNames = defaultDimensionNames(size);
        return new ChannelFrame(entityId, atomicLong.getAndIncrement(),
            gaussians, defaultDimensionNames);
    }

    public static List<String> defaultDimensionNames(int size) {
        List<String> dimensionNames = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            dimensionNames.add("Dimension " + i);
        return dimensionNames;
    }

    public static List<Double> pureSortedGaussians(int size) {
        //Generate a randonm list of gaussian doubles between -1.0 and 1.0
        Random rando = new Random();
        List<Double> gaussians = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            double nextG = rando.nextGaussian() / 2.0;
            //cap everything to -1 to 1.
            if (nextG < -1)
                nextG = -1;
            else if (nextG > 1)
                nextG = 1;
            gaussians.add(nextG);
        }
        //sort by natural ordering
        gaussians.sort(null);
        return gaussians.stream()
            .map((Double t) -> 1 - Math.abs(t))
            .collect(Collectors.toList());
    }

    public static ChannelFrame buildSpikeyPositiveChannelFrame(int totalSize, double spikeHeightCap, int numberOfSpikes, int spikeSize) {
        ChannelFrame frame = new ChannelFrame(totalSize);
        if (spikeSize > totalSize)
            spikeSize = totalSize;
        Random rando = new Random();
        for (int i = 0; i < numberOfSpikes; i++) {
            //generate a new random gaussian spike
            List<Double> doublesSpike = pureSortedDoubles(spikeSize);
            //generate a new random spike start index
            int spikeIndex = rando.nextInt(totalSize);
            //additively merge spike into frame
            for (Double d : doublesSpike) {
                //get the currently indexed value
                double currentValue = frame.getChannelData().get(spikeIndex);
                //add the spike value
                currentValue += d;
                //Cap at parameter (usually 1.0)... creates saturation effect
                if (currentValue > spikeHeightCap)
                    currentValue = spikeHeightCap;
                frame.getChannelData().set(spikeIndex, currentValue);
                //increment our current index
                spikeIndex++;
                //don't allow ourselves to index past our actual frame size
                if (spikeIndex >= totalSize)
                    break; //move on to the next randome spike grouping
            }
        }
        return frame;
    }

    public static List<Double> pureSortedDoubles(int size) {
        //Generate a randonm list of gaussian doubles between -1.0 and 1.0
        Random rando = new Random();
        List<Double> doubles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            double nextG = rando.nextDouble();
            doubles.add(nextG);
        }
        //sort by natural ordering
        doubles.sort(null);
        return doubles;
    }
}
