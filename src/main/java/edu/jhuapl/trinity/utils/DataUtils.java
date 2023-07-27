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

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.CdcCsv;
import edu.jhuapl.trinity.data.CdcTissueGenes;
import edu.jhuapl.trinity.data.FeatureVectorComparator;
import edu.jhuapl.trinity.data.cislunar.McclodSplitDataTsv;
import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.data.messages.GaussianMixture;
import edu.jhuapl.trinity.data.messages.GaussianMixtureData;
import edu.jhuapl.trinity.data.messages.ReconstructionAttributes;
import edu.jhuapl.trinity.data.messages.SemanticMap;
import edu.jhuapl.trinity.data.messages.SemanticReconstruction;
import edu.jhuapl.trinity.data.messages.SystemFeatures;
import edu.jhuapl.trinity.javafx.components.ProgressStatus;
import edu.jhuapl.trinity.javafx.components.Projector;
import edu.jhuapl.trinity.javafx.components.timeline.Item;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Sean Phillips
 */
public enum DataUtils {
    INSTANCE;

    public static double normalize(double rawValue, double min, double max) {
        return (rawValue - min) / (max - min);
    }

    public static List<Item> extractReconstructionEvents(ReconstructionAttributes attrs) {
        List<Item> items = new ArrayList<>(attrs.getEvents().size());
        Set<Entry<String, Object>> set = attrs.getEvents().entrySet();
        set.forEach((entry) -> {
            ArrayList<Double> times = (ArrayList<Double>) entry.getValue();

            Item startEventItem = new Item(times.get(0).longValue(), entry.getKey());
            items.add(startEventItem);

            Item endEventItem = new Item(times.get(1).longValue(), entry.getKey());
            endEventItem.setUp(false);
            items.add(endEventItem);
        });
        return items;
    }

    public static List<FeatureVector> convertSemanticReconstruction(SemanticReconstruction reconstruction) {
        List<FeatureVector> featureVectors = new ArrayList<>();

        List<List<Double>> data = reconstruction.getData_vars().getPrediction().getData();
        Map<String, Object> eventsMap = reconstruction.getAttrs().getEvents();
        Set<Entry<String, Object>> eventsSet = reconstruction.getAttrs().getEvents().entrySet();
        ArrayList<Double>[] timesTouples = new ArrayList[eventsSet.size()];
        timesTouples = reconstruction.getAttrs().getEvents().values().toArray(timesTouples);

        String[] eventLabels = new String[eventsMap.keySet().size()];
        eventLabels = eventsMap.keySet().toArray(eventLabels);
        double framerate = reconstruction.getAttrs().getFramerate();

        //data.stream().forEach(d -> {
        for (int i = 0; i < data.size() - 1; i++) {
            List<Double> d = data.get(i);
            FeatureVector fv = new FeatureVector();
            fv.getData().addAll(d);
            double sampleTime = i / framerate;
            //default is no event nor image
            fv.setLabel("No Event");
            fv.setImageURL("noevent.png");
            for (int eventIndex = 0; eventIndex < timesTouples.length; eventIndex++) {
                if (timesTouples[eventIndex].get(0) <= sampleTime
                    && sampleTime <= timesTouples[eventIndex].get(1)) {
                    fv.setLabel(eventLabels[eventIndex]);
                    fv.setImageURL(eventLabels[eventIndex] + ".png");
                    break;
                }
            }

            ArrayList<Double> bboxList = new ArrayList<>();
            bboxList.add(0.0);
            bboxList.add(0.0);
            bboxList.add(0.0);
            bboxList.add(0.0);
            fv.setBbox(bboxList);

            featureVectors.add(fv);
        }//);
        return featureVectors;
    }

    public static GaussianMixture convertSemanticSpace(SemanticMap semanticMap, double covarianceScalar) {
        GaussianMixture gm = new GaussianMixture();
        //for each list of data values make a GaussianMixtureData object
        semanticMap.getData().stream().forEach(d -> {
            GaussianMixtureData gmd = new GaussianMixtureData();
            //map data to mean values (centroids)
            gmd.getMean().addAll(d);
            //for each mean there needs to be a covariance
            List<Double> cov = new ArrayList<>();
            gmd.getMean().forEach(m -> cov.add(0, covarianceScalar));
            //Old way...
//            gmd.getMean().forEach(m -> gmd.getCovariance().add(0, covarianceScalar));
            gmd.getMean().forEach(m -> gmd.getCovariance().add(0, cov));
            gm.getData().add(gmd);
        });
        gm.setNumComponents(semanticMap.getData().size());
        gm.setNumFeatures(semanticMap.getData().get(0).size());
        gm.setLabel(semanticMap.getName());
        return gm;
    }

    private static double[] getAllAtIndex(List<FeatureVector> featureVectors, int index) {
        double[] allAtIndex = new double[featureVectors.size()];
        for (int i = 0; i < featureVectors.size(); i++) {
            allAtIndex[i] = featureVectors.get(i).getData().get(index);
        }
        return allAtIndex;
    }

    public static void generateProjectionPlane(List<FeatureVector> featureVectors) {
        int totalFeatureVectors = featureVectors.size();
        System.out.println("total featurevectors: " + totalFeatureVectors);
        int featureSize = featureVectors.get(0).getData().size();
        System.out.println("featureSize: " + featureSize);
        System.out.println("total combination pairs: " + featureSize * featureSize);

        int width = 100;
        int height = 100;
        ArrayList<Projector> projectors = new ArrayList<>();
        double[] xValues = new double[totalFeatureVectors];
        double[] yValues = new double[totalFeatureVectors];

        long startTime = System.nanoTime();
        //Generate each nCr combination where n is size and r is 2 (2D projection)
        //Outer loop will assign the X factor of the projection
        for (int xIndex = 0; xIndex < totalFeatureVectors; xIndex++) {
            xValues = getAllAtIndex(featureVectors, xIndex);
            //inner loop will assign the Y Factor of the projection
            for (int yIndex = 0; yIndex < totalFeatureVectors; yIndex++) {
                //inner loop will assign the Y Factor of the projection
                Projector projector = new Projector(width, height, xIndex, yIndex);
                yValues = featureVectors.get(yIndex).getData()
                    .stream().mapToDouble(d -> d).toArray();
                projector.project(xValues, yValues);
                projectors.add(projector);
            }
        }
        System.out.println("Time to generate Projectors: " + Utils.totalTimeString(startTime));
    }

    public static FeatureCollection convertCdcTissueGenes(List<CdcTissueGenes> cdcTissueGenesList, boolean normalize) {
        System.out.println("Converting Tissue Gene CSVs to Feature Vectors...");
        FeatureCollection fc = new FeatureCollection();
        List<FeatureVector> featureVectors = new ArrayList<>();
        cdcTissueGenesList.stream().forEach(c -> {
            FeatureVector fv = new FeatureVector();
            fv.getData().addAll(c.getGenes());
            String label = c.getTissueName();
            fv.setLabel(label);
            fv.setComponent(c.getSource());
            ArrayList<Double> bboxList = new ArrayList<>();
            bboxList.add(0.0);
            bboxList.add(0.0);
            bboxList.add(0.0);
            bboxList.add(0.0);
            fv.setBbox(bboxList);
//                fv.setImageURL(c.getState()+".PNG");
            HashMap<String, String> metaData = new HashMap<>();
            metaData.put("Number", String.valueOf(c.getNumber()));
            metaData.put("Source", c.getSource());
            metaData.put("Total Gene IDs", String.valueOf(c.getGenes().size()));
            fv.setMetaData(metaData);
            fv.setEntityId(c.getNumber() + ", " + c.getTissueName() + ", " + c.getSource());
            featureVectors.add(fv);
        });
        fc.setFeatures(featureVectors);
        fc.setType(FeatureCollection.TYPESTRING);

        Scene scene = App.getAppScene();
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Normalizing Gene Features...", -1);
            ps.fillStartColor = Color.SPRINGGREEN;
            ps.fillEndColor = Color.CYAN;
            ps.innerStrokeColor = Color.SPRINGGREEN;
            ps.outerStrokeColor = Color.CYAN;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });
        //Normalize the values
        if (normalize) {
            long startTime = System.nanoTime();
            int featureLength = featureVectors.get(0).getData().size();
            int updatePercent = featureLength / 50;

            FeatureVectorComparator fvc = new FeatureVectorComparator(0);
            for (int i = 0; i < featureLength; i++) {
                fvc.setCompareIndex(i);
                double min = featureVectors.stream().min(fvc).get().getData().get(i);
                double max = featureVectors.stream().max(fvc).get().getData().get(i);
                int index = i;
                fc.getFeatures().stream().forEach(fv -> {
                    //normalize data values
                    //scaledValue*2 - 1 to center in -1 to 1 range
                    fv.getData().set(index,
                        DataUtils.normalize(fv.getData().get(index), min, max));
                });
                if (i % updatePercent == 0) {
                    double percentComplete = Double.valueOf(i) / Double.valueOf(featureLength);
                    //System.out.println("percentComplete: " + percentComplete);
                    Platform.runLater(() -> {
                        ProgressStatus ps = new ProgressStatus(
                            "Normalizing Gene Features...", percentComplete);
                        ps.fillStartColor = Color.SPRINGGREEN;
                        ps.fillEndColor = Color.CYAN;
                        ps.innerStrokeColor = Color.SPRINGGREEN;
                        ps.outerStrokeColor = Color.CYAN;
                        scene.getRoot().fireEvent(
                            new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
                    });
                }
            }

            System.out.println("cdc Tissue Gene normalization took: " + Utils.totalTimeString(startTime));
            //fc.getFeatures().stream().sorted(fvc).forEach(fv -> System.out.println(fv.getData().get(2)));
        }
        return fc;
    }

    public static FeatureCollection convertCdcCsv(List<CdcCsv> cdcCsvList, boolean normalize) {
        long startTime = System.nanoTime();
        FeatureCollection fc = new FeatureCollection();
        List<FeatureVector> featureVectors = new ArrayList<>();
        cdcCsvList.stream()
            .filter(t -> !t.getCovid19_community_level().isBlank()
                && !t.getCovid19_community_level().contentEquals("N/A"))
            .filter(t -> t.getCovid_cases_per_100k() > 0)
            .filter(t -> t.getCovid_hospital_admissions_per_100k() > 0)
            .filter(t -> t.getCovid_inpatient_bed_utilization() > 0)
            .forEach(c -> {
                FeatureVector fv = new FeatureVector();
                fv.getData().add(c.getCovid_cases_per_100k());
                fv.getData().add(c.getCovid_hospital_admissions_per_100k());
                fv.getData().add(c.getCovid_inpatient_bed_utilization());
                //System.out.println(c.getCovid_inpatient_bed_utilization());
                String label = c.getCovid19_community_level().toUpperCase();
                fv.setLabel(label);
                fv.setMessageType("CDC County Covid Data");
                fv.setComponent(c.getState());
                ArrayList<Double> bboxList = new ArrayList<>();
                bboxList.add(0.0);
                bboxList.add(0.0);
                bboxList.add(0.0);
                bboxList.add(0.0);
                fv.setBbox(bboxList);
                fv.setImageURL(c.getState() + ".PNG");
                HashMap<String, String> metaData = new HashMap<>();
                metaData.put("county", c.getCounty());
                metaData.put("county_fips", String.valueOf(c.getCounty_fips()));
                metaData.put("state", c.getState());
                metaData.put("county_population", String.valueOf(c.getCounty_population()));
                metaData.put("health_service_area_number", String.valueOf(c.getHealth_service_area_number()));
                metaData.put("health_service_area", c.getHealth_service_area());
                metaData.put("health_service_area_population", String.valueOf(c.getHealth_service_area_population()));
                metaData.put("covid_inpatient_bed_utilization", String.valueOf(c.getCovid_inpatient_bed_utilization()));
                metaData.put("covid_hospital_admissions_per_100k", String.valueOf(c.getCovid_hospital_admissions_per_100k()));
                metaData.put("covid_cases_per_100k", String.valueOf(c.getCovid_cases_per_100k()));
                metaData.put("covid-19_community_level", String.valueOf(c.getCovid19_community_level()));
                metaData.put("date_updated", c.getDate_updated());

                fv.setMetaData(metaData);
                fv.setEntityId(c.getCounty() + ", " + c.getState() + ", " + c.getDate_updated());
                featureVectors.add(fv);
            });
        fc.setFeatures(featureVectors);
        fc.setType(FeatureCollection.TYPESTRING);
        System.out.println("cdccsv conversion took: " + Utils.totalTimeString(startTime));

        //Normalize the values
        if (normalize) {
            startTime = System.nanoTime();
            FeatureVectorComparator fvc = new FeatureVectorComparator(0);
            double min0 = fc.getFeatures().stream().min(fvc).get().getData().get(0);
            double max0 = fc.getFeatures().stream().max(fvc).get().getData().get(0);

            fvc.setCompareIndex(1);
            double min1 = fc.getFeatures().stream().min(fvc).get().getData().get(1);
            double max1 = fc.getFeatures().stream().max(fvc).get().getData().get(1);

            fvc.setCompareIndex(2);
            double min2 = fc.getFeatures().stream().min(fvc).get().getData().get(2);
            double max2 = fc.getFeatures().stream().max(fvc).get().getData().get(2);

            fc.getFeatures().stream().forEach(fv -> {
                //normalize data values
                //scaledValue*2 - 1 to center in -1 to 1 range
                fv.getData().set(0,
                    DataUtils.normalize(fv.getData().get(0), min0, max0));
                fv.getData().set(1,
                    DataUtils.normalize(fv.getData().get(1), min1, max1));
                fv.getData().set(2,
                    DataUtils.normalize(fv.getData().get(2), min2, max2));
            });
            System.out.println("cdccsv normalization took: " + Utils.totalTimeString(startTime));
            //fc.getFeatures().stream().sorted(fvc).forEach(fv -> System.out.println(fv.getData().get(2)));
        }
        return fc;
    }

    public static List<SystemFeatures> convertSplitData(List<McclodSplitDataTsv> splitDataTsvList) {
        System.out.println("Converting Split Data to System Features...");

        List<SystemFeatures> systemFeatures = new ArrayList<>();
        Scene scene = App.getAppScene();
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Converting Split Data to System Features...", -1);
            ps.fillStartColor = Color.SPRINGGREEN;
            ps.fillEndColor = Color.CYAN;
            ps.innerStrokeColor = Color.SPRINGGREEN;
            ps.outerStrokeColor = Color.CYAN;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });

        int dataLength = splitDataTsvList.size();
        int updatePercent = dataLength / 50;
//        splitDataTsvList.stream().forEach(c -> {
        for (int i = 0; i < dataLength; i++) {
            SystemFeatures sf = new SystemFeatures();
            McclodSplitDataTsv splitData = splitDataTsvList.get(i);
            sf.getOutput().add(splitData.getRightAscensionDeg());
            sf.getOutput().add(splitData.getDeclinationDeg());
            sf.getOutput().add(splitData.getRightAscensionRateDegMin());
            sf.getOutput().add(splitData.getDeclinationRateDegMin());

            sf.getInput().add(splitData.getInclinationDeg());
            sf.getInput().add(splitData.getRaanDeg());
            sf.getInput().add(splitData.getKappa());
            sf.getInput().add(splitData.getZeta());

            sf.setMessage_id(i);
//                HashMap<String,String> metaData = new HashMap<>();
//                metaData.put("Number", String.valueOf(c.getNumber()));
//                metaData.put("Source", c.getSource());
//                metaData.put("Total Gene IDs", String.valueOf(c.getGenes().size()));
//                fv.setMetaData(metaData);
            systemFeatures.add(sf);

            if (i % updatePercent == 0) {
                double percentComplete = Double.valueOf(i) / Double.valueOf(dataLength);
                //System.out.println("percentComplete: " + percentComplete);
                Platform.runLater(() -> {
                    ProgressStatus ps = new ProgressStatus(
                        "Normalizing Gene Features...", percentComplete);
                    ps.fillStartColor = Color.SPRINGGREEN;
                    ps.fillEndColor = Color.CYAN;
                    ps.innerStrokeColor = Color.SPRINGGREEN;
                    ps.outerStrokeColor = Color.CYAN;
                    scene.getRoot().fireEvent(
                        new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
                });
            }
        }
        return systemFeatures;
    }
}
