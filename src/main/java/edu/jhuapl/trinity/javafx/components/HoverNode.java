package edu.jhuapl.trinity.javafx.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * a node which displays a value on hover, but is otherwise empty
 * @author Sean Phillips
 */
public class HoverNode extends StackPane {

        HoverNode(int index, Double value) {
//            setPrefSize(15, 15);
            Label label = new Label("Vector(" + index + ") = " + value);
            label.getStyleClass().addAll("default-color0", "chart-line-symbol", "chart-series-line");
            label.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
            label.setPrefSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
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