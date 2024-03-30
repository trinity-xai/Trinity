package edu.jhuapl.trinity.javafx.controllers;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2020 - 2024 Johns Hopkins University Applied Physics Laboratory
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
import edu.jhuapl.trinity.utils.Configuration;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * FXML Controller class
 *
 * @author phillsm1
 */
public class SparkLinesController implements Initializable {
    @FXML
    private Spinner dataWindowSpinner;
    
    @FXML
    private LineChart<Number, Number> nativeControlLineChart;
    @FXML
    private NumberAxis nativeControlXAxis;
    XYChart.Series<Number, Number> nativeControlSeries;

    @FXML
    private LineChart<Number, Number> nativePerceptionLineChart;
    @FXML
    private NumberAxis nativePerceptionXAxis;
    XYChart.Series<Number, Number> nativePerceptionSeries;

    @FXML
    private LineChart<Number, Number> bmiPerceptionLineChart;
    @FXML
    private NumberAxis bmiPerceptionXAxis;
    XYChart.Series<Number, Number> bmiPerceptionSeries;
    
    //BMI Control Chart needs some extra stuff
    private XYChart.Series<Number, Number> bmiControlSeries;
    @FXML
    private NumberAxis bmiControlXAxis;
    @FXML
    private LineChart<Number, Number> bmiControlLineChart;

    //Bar Chart for BMI Window
    private XYChart.Series barChartSeries;
    ObservableList<XYChart.Data<String, Number>> barChartSeriesDataList;
    @FXML
    private CategoryAxis barChartXAxis;
    @FXML
    private BarChart barChart;

    private double currentMaxX = 0.0;    
    Scene scene;
    Configuration config;
//    BmiControlConfig currentBmiControlConfig = null;    
//    ConcurrentLinkedQueue<BmiControl> controlQueue;
//    ConcurrentLinkedQueue<DataQueueObject> dataQueue;
//    BmiControl mostRecentBmiControl = null;    
    File currentDirectory = new File(".");
    int dataWindowLimit = 2000;
    int bmiControlWindowSize;
    double deadband = 0.2;
    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        scene = App.getAppScene();
        config = App.getConfig();
        //TODO SMP read in config values for constructing the graphs
//        bmiControlWindowSize = Integer.valueOf((String)config.configProps.get("bmiControlWindowSize"));        
//        dataQueue = new ConcurrentLinkedQueue<>();

        dataWindowSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 5000, dataWindowLimit, 100));
        //whenever the spinner value is changed... call updateData
        dataWindowSpinner.valueProperty().addListener(e -> {
            dataWindowLimit = (Integer) dataWindowSpinner.getValue();
        });   
        
        //static series initialization
        nativeControlSeries = new XYChart.Series<>();
        nativeControlLineChart.getData().add(nativeControlSeries);
        
        bmiControlSeries = new XYChart.Series<>();
        bmiControlLineChart.getData().add(bmiControlSeries);
        
        nativePerceptionSeries = new XYChart.Series<>();
        nativePerceptionLineChart.getData().add(nativePerceptionSeries);        
        bmiPerceptionSeries = new XYChart.Series<>();
        bmiPerceptionLineChart.getData().add(bmiPerceptionSeries);

//        controlQueue = new ConcurrentLinkedQueue<>();
//        barChartSeriesDataList = FXCollections.observableArrayList();
//        barChartSeries = new XYChart.Series<>("BMI Control Classes", barChartSeriesDataList);
//        barChart.getData().add(barChartSeries);
//
//        scene.addEventHandler(BmiControlEvent.BMI_CONTROL_CONFIG, e -> {
//            currentBmiControlConfig = (BmiControlConfig) e.data;
//            clearBmiWindow();
//            updateBarChart();
//        });
//        scene.addEventHandler(ScenarioEvent.PROTECT_RESPONSE, e -> {
//            String attackString = (String)e.object;
//            //String comes in the form "Attack 1"... but we just need the number
//            String [] tokens = attackString.split(" ");
//            if(tokens.length > 1) {
//                try {
//                    Integer protectAction = Integer.valueOf(tokens[1]);
//                    //update integration chart with real data
//                    insertIntegrationEvent(protectAction);
//                    //pad perception charts so they keep up
//                    updatePerceptionCharts(0, 0);
//                    currentMaxX++;
//                } catch(NumberFormatException ex) {
//                    //couldn't read token as a number... drop it like its hot
//                }
//            }
//        });
//
//        scene.addEventHandler(NativeControlEvent.NATIVE_CONTROL, e -> {
//            NativeControl nc = (NativeControl)e.data;
//            //update native control with real data
//            updateNativeControlChart(nc.getDeltaX(), nc.getDeltaY());
//            //pad perception charts so they keep up
//            updatePerceptionCharts(0, 0);
//            //update the generic data queue 
//            addToDataQueue(nc);
//            currentMaxX++;
//        });
//        scene.addEventHandler(MultiplexPerceptionEvent.MULTIPLEX_PERCEPTION, e -> {
//            MultiplexPerception mp = (MultiplexPerception)e.data;
//            updatePerceptionCharts(mp.getNativePerception(), mp.getBmiPerception());
//            //pad bmi control chart so it keeps up
//            updateBmiChart(0,currentBmiControlConfig.getBmiControlNamesArray()[0]);            
//            //update the generic data queue 
//            addToDataQueue(mp);
//            currentMaxX++;
//        });
//
//        scene.addEventHandler(BmiControlEvent.BMI_CONTROL_DATA, e -> {
//            BmiControl bc = (BmiControl)e.data;
//            String label = e.label;
//            addBmiControlToQueue(bc);
//            //add real BMI Control data to the charts
//            updateBmiChart(bc.getBmiControlR(), label);
//            setBarData();
////            //pad native control chart so it keeps up
////            updateNativeControlChart(0, 0);            
//            //pad perception charts so they keep up
//            updatePerceptionCharts(0, 0);
//            //update the generic data queue 
//            addToDataQueue(bc);
//            currentMaxX++;
//        });
    }

//    private void insertIntegrationEvent(int action) {
//        //Native Perception
//        XYChart.Data dataNode;
//        //if action == non rest
//        if(action > 0) {
//            dataNode = new XYChart.Data(currentMaxX, action); 
//            Font font = new Font("Consolas", 18);
//            Text customLabel = new Text(
//                currentBmiControlConfig.getBmiControlNamesArray()[action]);
//            customLabel.setFont(font);
//            customLabel.setFill(Color.BLUE);            
//            dataNode.setNode(customLabel);
//            bmiControlSeries.getData().add(dataNode);
//            bmiControlSeries.getNode().setViewOrder(1);
//        }
//    }
            
    private void updateNativeControlChart(double deltaX, double deltaY) {
        double total = Math.abs(deltaX) + Math.abs(deltaY);
        XYChart.Data dataNode = new XYChart.Data(currentMaxX, total); 
        nativeControlSeries.getData().add(dataNode);
        //make sure this data series fits within the current window
        trimSeries(nativeControlSeries);
    }
    private void updatePerceptionCharts(int nativePerception, int bmiPerception) {
        //Native Perception
        XYChart.Data npDataNode;
        //if Native Perception == non rest
        if(nativePerception > 0) {
            npDataNode = new XYChart.Data(currentMaxX, 1.0); 
        } else {
            npDataNode = new XYChart.Data(currentMaxX, 0.0);
        }      
        nativePerceptionSeries.getData().add(npDataNode);
        //make sure this data series fits within the current window
        trimSeries(nativePerceptionSeries);
    
        //BMI Perception
        XYChart.Data bpDataNode;
        //if Native Perception == non rest
        if(bmiPerception > 0) {
            bpDataNode = new XYChart.Data(currentMaxX, 1.0); 
        } else {
            bpDataNode = new XYChart.Data(currentMaxX, 0.0);
        }      
        bmiPerceptionSeries.getData().add(bpDataNode);
        //make sure this data series fits within the current window
        trimSeries(bmiPerceptionSeries);
    }
    public void updateBmiChart(int bmiSignalClass, String label) {
        XYChart.Data dataNode = new XYChart.Data(currentMaxX, bmiSignalClass); 
        bmiControlSeries.getData().add(dataNode);
        Tooltip t = new Tooltip(label + " " + dataNode.getXValue().toString());
        Tooltip.install(dataNode.getNode(), t);      
        //make sure this data series fits within the current window
        trimSeries(bmiControlSeries);
    }
//    private void updateBarChart() {
//        barChartSeriesDataList.clear();
//        barChartXAxis.setCategories(FXCollections.<String>observableArrayList(
//            currentBmiControlConfig.getBmiControlNamesArray()));
//        int len = currentBmiControlConfig.getBmiControlNamesArray().length;
//        for(int i=0; i<len; i++) {
//            barChartSeriesDataList.add(i, new BarChart.Data<>(
//                currentBmiControlConfig.getBmiControlNamesArray()[i], 0.0));
//        } 
//    }
//    public void setBarData() {
//        //which entry has the largest percentage ?                
//        HashMap<Integer, Double> percentMap = computePercentages(bmiControlWindowSize);        
//        //Convert Hashmap to array list
//        ArrayList<Map.Entry<Integer, Double>> categoryPercentages = new ArrayList<>(percentMap.entrySet());
//        //for each category percentage
//        for(int i=0;i<categoryPercentages.size();i++) {
//            Entry<Integer, Double> entry = categoryPercentages.get(i);
//            if(null != currentBmiControlConfig)
//                barChartSeriesDataList.add(entry.getKey(), new BarChart.Data<>(
//                    currentBmiControlConfig.getBmiControlNamesArray()[entry.getKey()], entry.getValue()*100.0));
//            else
//                barChartSeriesDataList.add(entry.getKey(), new BarChart.Data<>(
//                    String.valueOf(entry.getKey()), entry.getValue()*100.0));
//        }
//    }
//    private HashMap<Integer,Double> computePercentages(Integer total) {
//        HashMap<Integer, Integer> countsMap = new HashMap<>();
//        controlQueue.stream().forEach(bc -> {
//            if(bc.getBmiControlR() >= 0) {
//                Integer entry = countsMap.get(bc.getBmiControlR());
//                if(null != entry)
//                    countsMap.put(bc.getBmiControlR(), entry+1); //just increment
//                else
//                    countsMap.put(bc.getBmiControlR(), 1); //first time
//            }
//        });
//        HashMap<Integer, Double> percentEntryMap = new HashMap<>();
//        countsMap.forEach((t, u) -> {
//            percentEntryMap.put(t, u.doubleValue() / total.doubleValue());
//        });
//        return percentEntryMap;
//    }    
//    private void addToDataQueue(MessageData md) {
//        DataQueueObject dqo = new DataQueueObject();
//        if(md instanceof NativeControl)
//            dqo.setNativeControl((NativeControl)md);
//        else if(md instanceof BmiControl)
//            dqo.setBmiControl((BmiControl)md);
//        else if(md instanceof MultiplexPerception)
//            dqo.setMultiplexPerception((MultiplexPerception)md);
//        dataQueue.add(dqo);        
//        int excess = dataQueue.size() - dataWindowLimit;
//        if(excess > 0) {
//            dataQueue.poll();
//        }
//    }
    private void trimSeries(Series series) {
        int excess = series.getData().size() - dataWindowLimit;
        if(excess > 0) {
            series.getData().remove(0, excess);
        }
    }
//    private void clearDataWindow() {
//        dataQueue.clear();
//    }
//    private void clearBmiWindow() {
//        controlQueue.clear();
//    }
//    
//    private void addBmiControlToQueue(BmiControl bc) {
//        controlQueue.add(bc);
//        mostRecentBmiControl = bc;
//        bmiControlWindowSize = Integer.valueOf((String)config.configProps.get("bmiControlWindowSize"));
//        if(controlQueue.size() > bmiControlWindowSize) {
//            controlQueue.poll();
//        }
//    }
    
     @FXML
    public void export(ActionEvent event) {
//        if(metricTracker.metricDataList.isEmpty()) {
//            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No metrics data to export.", ButtonType.OK);
//            alert.showAndWait();
//            return;
//        }
//        String fileName = "MetricsExport.csv";
//        FileChooser fc = new FileChooser();
//        fc.setInitialFileName(fileName);
//        fc.setTitle("Choose a file to export to...");
//        fc.setInitialDirectory(currentDirectory);
//        File selectedFile = fc.showSaveDialog(null);
//        if(null != selectedFile && selectedFile.getParentFile().canWrite()) {
//            try (FileWriter writer = new FileWriter(fileName)) {
//                for(MetricData md : metricTracker.metricDataList) {
//                    writer.append(String.valueOf(md.getNativeControlTrial()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getBmiControlTrial()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getNativePerceptionTrial()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getBmiPerceptionTrial()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getRegionAlertTruth()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getSystemAlertTruth()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getAttackAlertTruth()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getNativePerceptionTruth()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getBmiPerceptionTruth()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getNativeControlAccuracy()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getBmiControlAccuracy()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getNativePerceptionAccuracy()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getBmiPerceptionAccuracy()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getNativeControlTime()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getBmiControlTime()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getNativePerceptionTime()));
//                    writer.append(',');
//                    writer.append(String.valueOf(md.getBmiPerceptionTime()));
//                    writer.append('\n');
//                }
//                writer.flush();
//            } catch(IOException e) {
//                Logger.getLogger(ChartRecorderController.class.getName()).log(Level.SEVERE, null, e);
//            }   
//        }
    }
    @FXML
    public void reset(ActionEvent event) {
//        //Create and Show Confirmation Dialog.
//        Alert alert = AlertFactory.getDeleteAllAlert();
//        alert.showAndWait();
//        //Begin Deletion Process.
//        if (alert.getResult() == ButtonType.YES) {    
//            nativeControlCorrect = bmiControlCorrect = nativePerceptionCorrect = 
//                    bmiPerceptionCorrect = 0;
//            metricTracker = new MetricTracker();
//            dateTextField.setText(LocalDateTime.now().toString());
//
//            metricTable.getItems().clear();
//        }
    }
}
