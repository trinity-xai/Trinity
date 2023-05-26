package edu.jhuapl.trinity.data;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class Trial {
    public String trialId;
    public ArrayList<ArrayList<Double>> factorTimeSeries;

    public Trial(String trialId, int factorCount) {
        this(trialId, new ArrayList<>());
        for (int i = 0; i < factorCount; i++)
            factorTimeSeries.add(new ArrayList<>());
    }

    public Trial(String trialId, ArrayList<ArrayList<Double>> factorTimeSeries) {
        this.trialId = trialId;
        this.factorTimeSeries = new ArrayList<>(factorTimeSeries.size() + 1);
        this.factorTimeSeries.addAll(factorTimeSeries);
    }

    public void addFactors(double[] factors) {
        for (int i = 0; i < factors.length; i++) {
            if (i <= factorTimeSeries.size())
                factorTimeSeries.get(i).add((factors[i]));
        }
    }

    public Trajectory toTrajectory(int xFactor, int yFactor, int zFactor) {
        ArrayList<double[]> states = new ArrayList<>();
        ArrayList<Double> times = new ArrayList<>();
        int size = getTimeSeriesSize(xFactor);

        for (double i = 0; i < size; i++) {
            int timeIndex = Double.valueOf(i).intValue();
            double[] currentState = {
                factorTimeSeries.get(xFactor).get(timeIndex),
                factorTimeSeries.get(yFactor).get(timeIndex),
                factorTimeSeries.get(zFactor).get(timeIndex)
            };
            states.add(currentState);
            times.add(i);
        }
        Trajectory trajectory = new Trajectory(trialId, states, times);
        return trajectory;
    }

    public int getTimeSeriesSize(int factorIndex) {
        if (factorIndex < 0 || factorIndex > factorTimeSeries.size())
            return 0;
        return factorTimeSeries.get(factorIndex).size();
    }

    public static boolean isTrialFile(File file) throws IOException {
        Optional<String> firstLine = Files.lines(file.toPath()).findFirst();
        return firstLine.isPresent() && firstLine.get().startsWith("TrialID");
    }

    public static ArrayList<Trial> readTrialFile(File file) {

        ArrayList<Trial> trialList = new ArrayList<>();
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
                        trialList.add(new Trial(trialID, varTimeSeries));
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
            trialList.add(new Trial(trialID, varTimeSeries));

            System.out.println(trialList.size());
            //closes the stream and release the resources
        } catch (IOException ex) {
            Logger.getLogger(Trial.class.getName()).log(Level.SEVERE, null, ex);
        }
        return trialList;
    }
}
