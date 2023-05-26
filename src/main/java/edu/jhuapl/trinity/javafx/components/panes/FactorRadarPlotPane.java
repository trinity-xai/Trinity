package edu.jhuapl.trinity.javafx.components.panes;

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

import edu.jhuapl.trinity.data.Trial;
import edu.jhuapl.trinity.javafx.components.DataCarousel;
import edu.jhuapl.trinity.javafx.components.TrialRadarChart;
import eu.hansolo.fx.charts.ChartType;
import eu.hansolo.fx.charts.YChart;
import eu.hansolo.fx.charts.data.ValueChartItem;
import eu.hansolo.fx.charts.series.YSeries;
import javafx.animation.FadeTransition;
import javafx.scene.Cursor;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;

/**
 * @author Sean Phillips
 */
public class FactorRadarPlotPane extends StackPane {
    public double width;
    public double height;
    private double panePosX;
    private double paneBoxPosY;
    private double paneBoxOldX;
    private double paneBoxOldY;
    private double paneBoxDeltaX;
    private double paneBoxDeltaY;
    private FadeTransition ft = new FadeTransition(Duration.seconds(10), this);
    private static final int ELEMENTS = 8;
    private static final ChartType CHART_TYPE = ChartType.SMOOTH_RADAR_POLYGON;
    private YSeries<ValueChartItem> series1;
    private YChart<ValueChartItem> chart;
    private DataCarousel dataCarousel;
    double chartSize = 175;

    public FactorRadarPlotPane(double width, double height) {
        this.width = width;
        this.height = height;
        ft.setAutoReverse(false);
        ft.setCycleCount(0);
        setOnMouseEntered(e -> {
            requestFocus();
            ft.stop();
            setOpacity(0.9);
        });
        setOnMouseExited(e -> {
            ft.stop();
            ft.setToValue(0.2);
            ft.setDelay(Duration.seconds(10));
            ft.playFromStart();
        });
        setOnMousePressed(me -> {
            setCursor(Cursor.MOVE);
            panePosX = me.getSceneX();
            paneBoxPosY = me.getSceneY();
            paneBoxOldX = me.getSceneX();
            paneBoxOldY = me.getSceneY();
            me.consume();
        });
        setOnMouseDragged(me -> {
            setCursor(Cursor.MOVE);
            paneBoxOldX = panePosX;
            paneBoxOldY = paneBoxPosY;
            panePosX = me.getSceneX();
            paneBoxPosY = me.getSceneY();
            paneBoxDeltaX = (panePosX - paneBoxOldX);
            paneBoxDeltaY = (paneBoxPosY - paneBoxOldY);
            setTranslateX(getTranslateX() + paneBoxDeltaX);
            setTranslateY(getTranslateY() + paneBoxDeltaY);
            me.consume();
        });
        setOnMouseReleased(me -> setCursor(Cursor.DEFAULT));

        setPrefSize(width, height);
        setMaxSize(width, height);

        setBackground(Background.EMPTY);
        //make some charts
        ArrayList<TrialRadarChart> charts = new ArrayList<>();
        dataCarousel = new DataCarousel(charts, chartSize, chartSize);

        VBox vbox = new VBox(2,
            dataCarousel
        );
        BorderPane bp = new BorderPane(vbox);
        bp.setMinSize(width, height);
        getChildren().addAll(bp); //add everything to the Pane
    }

    public void setTrial(Trial trial) {
        dataCarousel.clearAll();
        for (int i = 0; i < trial.factorTimeSeries.size(); i++) {

            TrialRadarChart trc = new TrialRadarChart(trial, i, chartSize, chartSize);
            dataCarousel.addChart(trc);
        }
    }
}
