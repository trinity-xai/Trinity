package edu.jhuapl.trinity.data;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class SpikeTrain {
    public String trialId;
    public ArrayList<ArrayList<Double>> spikeTrains;

    public SpikeTrain(String trialId, ArrayList<ArrayList<Double>> spikeTrains) {
        this.trialId = trialId;
        this.spikeTrains = new ArrayList<>(spikeTrains.size() + 1);
        this.spikeTrains.addAll(spikeTrains);
    }

    public double[] getSpikeArrayAtIndex(int timeIndex) {
        double[] spikeArray = new double[spikeTrains.size()];
        for (int i = 0; i < spikeTrains.size(); i++) {
            spikeArray[i] = spikeTrains.get(i).get(timeIndex);
        }
        return spikeArray;
    }

    public int getTimeSeriesSize(int factorIndex) {
        if (factorIndex < 0 || factorIndex > spikeTrains.size())
            return 0;
        return spikeTrains.get(factorIndex).size();
    }

    public static ArrayList<SpikeTrain> readSpikeTrainFile(File file) {

        ArrayList<SpikeTrain> spikeTrainList = new ArrayList<>();
        //reads the file
        try (FileReader fr = new FileReader(file)) {
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            String trialID = "Unknown";
            ArrayList<ArrayList<Double>> varTimeSeries = new ArrayList<>();
            String line;
            boolean startedFirstTrial = false;
            //Read until the first TrialId line
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.isBlank()) {
                    //skip whitespace
                } else if (line.contains("TrialID")) {
                    //first add the previous trial we were building
                    if (startedFirstTrial) {
                        spikeTrainList.add(new SpikeTrain(trialID, varTimeSeries));
                        varTimeSeries.clear();
                    }
                    trialID = line.split("=")[1].trim();
                    startedFirstTrial = true;
                } else {
                    //its a set of doubles
                    String[] tokens = line.split(",");
                    ArrayList<Double> timeSeries = new ArrayList<>();
                    for (int i = 0; i < tokens.length; i++) {
                        timeSeries.add(Double.valueOf(tokens[i]));
                    }
                    varTimeSeries.add(timeSeries);
                }
            }
            //Add last record when the end of the file is reached
            spikeTrainList.add(new SpikeTrain(trialID, varTimeSeries));

            System.out.println(spikeTrainList.size());
            //closes the stream and release the resources
        } catch (IOException ex) {
            Logger.getLogger(SpikeTrain.class.getName()).log(Level.SEVERE, null, ex);
        }
        return spikeTrainList;
    }
}
