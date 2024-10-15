/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.events.FactorAnalysisEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Trajectory3D;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;

import java.util.function.UnaryOperator;

/**
 * @author Sean Phillips
 */
public class FactorAnalysisControlPane extends StackPane {
    double controlSize = 15;
    double BORDER = 2.0;
    public double width;
    public double height;
    private double panePosX;
    private double paneBoxPosY;
    private double paneBoxOldX;
    private double paneBoxOldY;
    private double paneBoxDeltaX;
    private double paneBoxDeltaY;

    CheckBox day1CheckBox;

    private FadeTransition ft = new FadeTransition(Duration.seconds(10), this);

    public ObservableList<XYChart.Data<Double, Double>> xFactorVector;
    public XYChart.Series xFactorSeries;
    private LineChart xFactorChart;

    public ObservableList<XYChart.Data<Double, Double>> yFactorVector;
    public XYChart.Series yFactorSeries;
    private LineChart yFactorChart;

    public ObservableList<XYChart.Data<Double, Double>> zFactorVector;
    public XYChart.Series zFactorSeries;
    private LineChart zFactorChart;

    public FactorAnalysisControlPane(double width, double height) {
        xFactorVector = FXCollections.observableArrayList();
        xFactorSeries = new XYChart.Series("Neuronal Trajectory", xFactorVector);
        this.xFactorChart = new LineChart(new NumberAxis(), new NumberAxis(), FXCollections.observableArrayList(xFactorSeries));

        yFactorVector = FXCollections.observableArrayList();
        yFactorSeries = new XYChart.Series("Neuronal Trajectory", yFactorVector);
        this.yFactorChart = new LineChart(new NumberAxis(), new NumberAxis(), FXCollections.observableArrayList(yFactorSeries));

        zFactorVector = FXCollections.observableArrayList();
        zFactorSeries = new XYChart.Series("Neuronal Trajectory", zFactorVector);
        this.zFactorChart = new LineChart(new NumberAxis(), new NumberAxis(), FXCollections.observableArrayList(zFactorSeries));

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

        setPrefSize(width, height + controlSize);
        setMaxSize(width, height + controlSize);

        setBackground(Background.EMPTY);
        Label xFactorLabel = new Label("X Factor");
        Label yFactorLabel = new Label("Y Factor");
        Label zFactorLabel = new Label("Z Factor");
        ChoiceBox<String> xFactorChoiceBox = new ChoiceBox(FXCollections.observableArrayList(
            "Factor-0", "Factor-1", "Factor-2", "Factor-3",
            "Factor-4", "Factor-5", "Factor-6", "Factor-7"));
        xFactorChoiceBox.getSelectionModel().select(0); //X0 is the default
        xFactorChoiceBox.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            xFactorChoiceBox.getScene().getRoot().fireEvent(
                new FactorAnalysisEvent(FactorAnalysisEvent.XFACTOR_SELECTION,
                    xFactorChoiceBox.getSelectionModel().getSelectedIndex()));
        });

        ChoiceBox<String> yFactorChoiceBox = new ChoiceBox(FXCollections.observableArrayList(
            "Factor-0", "Factor-1", "Factor-2", "Factor-3",
            "Factor-4", "Factor-5", "Factor-6", "Factor-7"));
        yFactorChoiceBox.getSelectionModel().select(1); //X1 is the default
        yFactorChoiceBox.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            yFactorChoiceBox.getScene().getRoot().fireEvent(
                new FactorAnalysisEvent(FactorAnalysisEvent.YFACTOR_SELECTION,
                    yFactorChoiceBox.getSelectionModel().getSelectedIndex()));
        });

        ChoiceBox<String> zFactorChoiceBox = new ChoiceBox(FXCollections.observableArrayList(
            "Factor-0", "Factor-1", "Factor-2", "Factor-3",
            "Factor-4", "Factor-5", "Factor-6", "Factor-7"));
        zFactorChoiceBox.getSelectionModel().select(2); //X2 is the default
        zFactorChoiceBox.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            zFactorChoiceBox.getScene().getRoot().fireEvent(
                new FactorAnalysisEvent(FactorAnalysisEvent.ZFACTOR_SELECTION,
                    zFactorChoiceBox.getSelectionModel().getSelectedIndex()));
        });

        TextField scaleTextField = new TextField("500");
        //initialize and setup textfield controls
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?([1-9][0-9]*)?")) {
                return change;
            }
            return null;
        };
        scaleTextField.setTextFormatter(new TextFormatter<>(
            new IntegerStringConverter(), 500, integerFilter));

        scaleTextField.textProperty().addListener(cl -> {
            //detectionExecutor.playbackPeriodNanos = Long.valueOf(playbackPeriodTextField.getText());
            scaleTextField.getScene().getRoot().fireEvent(new FactorAnalysisEvent(
                FactorAnalysisEvent.SCALE_UPDATED, Integer.valueOf(scaleTextField.getText())));
        });

        VBox vbox = new VBox(10,
            new HBox(10, xFactorLabel, xFactorChoiceBox),
            xFactorChart,
            new HBox(10, yFactorLabel, yFactorChoiceBox),
            yFactorChart,
            new HBox(10, zFactorLabel, zFactorChoiceBox),
            zFactorChart
        );
        BorderPane bp = new BorderPane(vbox);
        bp.setMinSize(width, height);
        getChildren().addAll(bp); //add everything to the Pane
    }

    public void setFactorVector(ObservableList<XYChart.Data<Double, Double>> vector, double[] newData) {
        vector.clear();
        for (int i = 0; i < newData.length; i++) {
            XYChart.Data data = new XYChart.Data(i, newData[i], newData[i]);
            Tooltip.install(data.getNode(), new Tooltip("Factor Vector "
                + data.getXValue().toString() + "\n"
                + data.getYValue()));

            data.setNode(new HoverNode(i, newData[i]));
            vector.add(data);
        }
    }

    public void setTrajectory3D(Trajectory3D trajectory3D) {
        //Do the X factor
        double[] dataX = new double[trajectory3D.points.size() + 1];
        double[] dataY = new double[trajectory3D.points.size() + 1];
        double[] dataZ = new double[trajectory3D.points.size() + 1];
        for (int i = 0; i < trajectory3D.points.size(); i++) {
            dataX[i] = trajectory3D.points.get(i).getX();
            dataY[i] = trajectory3D.points.get(i).getY();
            dataZ[i] = trajectory3D.points.get(i).getZ();
        }
        setFactorVector(xFactorVector, dataX);
        setFactorVector(yFactorVector, dataY);
        setFactorVector(zFactorVector, dataZ);
    }

    /**
     * a node which displays a value on hover, but is otherwise empty
     */
    class HoverNode extends StackPane {

        HoverNode(int index, Double value) {
            setPrefSize(15, 15);
            final Label label = new Label("Vector(" + index + ") = " + value);
            label.getStyleClass().addAll("default-color0", "chart-line-symbol", "chart-series-line");
            label.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
            label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
            label.setTranslateY(-30); //Move label 25 pixels up
            Color ALICEBLUETRANS = new Color(0.9411765f, 0.972549f, 1.0f, 0.3);
            label.setBackground(new Background(new BackgroundFill(ALICEBLUETRANS, CornerRadii.EMPTY, Insets.EMPTY)));

            setOnMouseEntered(e -> {
                label.getStyleClass().add("onHover");
                getChildren().setAll(label);
                toFront();
            });
            setOnMouseExited(e -> {
                label.getStyleClass().remove("onHover");
                getChildren().clear();
            });
        }
    }
}
