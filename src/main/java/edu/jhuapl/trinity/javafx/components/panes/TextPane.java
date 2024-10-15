/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import lit.litfx.controls.output.LitLog;
import lit.litfx.controls.output.LogToolbar;

/**
 * @author Sean Phillips
 */
public class TextPane extends LitPathPane {
    Background transBackground = new Background(new BackgroundFill(
        Color.DARKCYAN.deriveColor(1, 1, 1, 0.1),
        CornerRadii.EMPTY, Insets.EMPTY));
    BorderPane bp;
    LitLog litLog;
    LogToolbar logToolbar;
    public static int PANE_WIDTH = 700;
    public static double NODE_WIDTH = PANE_WIDTH - 50;
    public static double NODE_HEIGHT = NODE_WIDTH / 2.0;
    String currentText = "";

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        LogToolbar logToolbar = new LogToolbar(5, 5);
        logToolbar.textToggleButton.setText("Plain");
        logToolbar.textToggleButton.setPrefWidth(150);
        logToolbar.fontChoiceBox.setPrefHeight(50);
        logToolbar.picker.setPrefSize(150, 50);
        logToolbar.fontSizeSpinner.setPrefSize(75, 50);

        LitLog litLog = new LitLog(5, 50);
        litLog.scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        litLog.vbox.setAlignment(Pos.TOP_CENTER);
        litLog.vbox.setMaxHeight(NODE_HEIGHT);
        litLog.vbox.mouseTransparentProperty().bind(litLog.selectingProperty);
        litLog.textArea.setWrapText(true);
        litLog.setMinWidth(NODE_WIDTH);
        litLog.setMinHeight(NODE_HEIGHT);
        litLog.setMaxWidth(NODE_WIDTH);
        litLog.setMaxHeight(NODE_HEIGHT);
        bpOilSpill.setTop(logToolbar);
        bpOilSpill.setCenter(litLog);

        litLog.selectingProperty.bind(logToolbar.textToggleButton.selectedProperty());
        litLog.vbox.addEventHandler(MouseEvent.MOUSE_CLICKED, eh -> {
            logToolbar.textToggleButton.setSelected(true);
            if (eh.getClickCount() > 1) {
                Platform.runLater(() -> litLog.textArea.selectAll());
            }
        });
        return bpOilSpill;
    }

    public TextPane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, 350, createContent(),
            "Text Console", "", 300.0, 400.0);
        this.scene = scene;
        bp = (BorderPane) this.contentPane;
        litLog = (LitLog) bp.getCenter();
        logToolbar = (LogToolbar) bp.getTop();
        litLog.setBackground(transBackground);
        logToolbar.fontChoiceBox.valueProperty().addListener(c -> refresh());
        logToolbar.picker.valueProperty().addListener(c -> refresh());
        logToolbar.fontSizeSpinner.valueProperty().addListener(c -> refresh());
        Glow glow = new Glow(0.95);
        ImageView refresh = ResourceUtils.loadIcon("refresh", 50);
        VBox refreshVBox = new VBox(refresh);
        refreshVBox.setOnMouseEntered(e -> refresh.setEffect(glow));
        refreshVBox.setOnMouseExited(e -> refresh.setEffect(null));
        refreshVBox.setOnMouseClicked(eh -> refresh());
        logToolbar.getChildren().add(refreshVBox);
    }

    private void refresh() {
        litLog.vbox.getChildren().clear();
        litLog.lines.clear();
        litLog.textArea.clear();
        Text text = new Text(currentText);
        //Override default font
        text.setFont(new Font((String) logToolbar.fontChoiceBox.getValue(),
            (int) logToolbar.fontSizeSpinner.getValue()));
        text.setFill(logToolbar.picker.getValue());
        text.setWrappingWidth(NODE_WIDTH);
        text.setTextAlignment(TextAlignment.LEFT);
        Platform.runLater(() -> {
            litLog.lines.add(text);
            litLog.vbox.getChildren().add(text);
            litLog.textArea.setText(currentText);
        });
    }

    public void setText(String text) {
        litLog.lines.clear();
        litLog.textArea.clear();
        litLog.vbox.getChildren().clear();
        currentText = text;
        addLine(currentText,
            new Font((String) logToolbar.fontChoiceBox.getValue(),
                (int) logToolbar.fontSizeSpinner.getValue()),
            logToolbar.picker.getValue());
    }

    public void addLine(String line, Font font, Color color) {
        Text text = new Text(line);
        //Override default font
        text.setFont(font);
        text.setFill(color);
        text.setWrappingWidth(NODE_WIDTH);
        text.setTextAlignment(TextAlignment.LEFT);
        litLog.lines.add(text);
        Platform.runLater(() -> animateLine(text));
    }

    private void animateLine(Text text) {
        final IntegerProperty i = new SimpleIntegerProperty(0);
        Timeline timeline = new Timeline();
        String animatedString = text.getText();
        KeyFrame keyFrame1 = new KeyFrame(Duration.millis(5), event -> {
            if (i.get() > animatedString.length()) {
                timeline.stop();
                litLog.textArea.appendText(System.lineSeparator() + animatedString);
            } else {
                text.setText(animatedString.substring(0, i.get()));
                i.set(i.get() + 1);
            }
        });
        timeline.getKeyFrames().addAll(keyFrame1);
        timeline.setCycleCount(Animation.INDEFINITE);
        litLog.vbox.getChildren().add(text);
        timeline.play();
    }
}
