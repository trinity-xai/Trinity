package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.CommandRequest;
import edu.jhuapl.trinity.messages.CommandTask;
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
import javafx.scene.control.TextField;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import lit.litfx.controls.output.LitLog;
import lit.litfx.controls.output.LogToolbar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * @author Sean Phillips
 */
public class TextPane extends LitPathPane {
    public static int PANE_WIDTH = 700;
    public static double NODE_WIDTH = PANE_WIDTH - 50;
    public static double NODE_HEIGHT = NODE_WIDTH / 2.0;
    public static Font USER_FONT = new Font("consolas", 14);
    private static final Logger LOG = LoggerFactory.getLogger(TextPane.class);

    Background transBackground = new Background(new BackgroundFill(
        Color.DARKCYAN.deriveColor(1, 1, 1, 0.1),
        CornerRadii.EMPTY, Insets.EMPTY));
    BorderPane bp;
    LitLog litLog;
    LogToolbar logToolbar;
    TextField commandTextField;
    String currentText = "";
    int commandIndex = 0;
    ArrayList<String> commandList;

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        LogToolbar logToolbar = new LogToolbar(5, 5);
        logToolbar.textToggleButton.setText("Plain");
        logToolbar.textToggleButton.setPrefWidth(150);
        logToolbar.fontChoiceBox.setPrefHeight(50);
        logToolbar.picker.setPrefSize(150, 50);
        logToolbar.fontSizeSpinner.setPrefSize(75, 50);
        logToolbar.picker.setValue(Color.CYAN);

        LitLog litLog = new LitLog(5, 50);
        litLog.scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        litLog.vbox.setAlignment(Pos.TOP_CENTER);
        litLog.vbox.mouseTransparentProperty().bind(litLog.selectingProperty);
        litLog.textArea.setWrapText(true);
        litLog.setMinWidth(NODE_WIDTH);
        litLog.setMinHeight(NODE_HEIGHT);
        litLog.setMaxWidth(NODE_WIDTH);

        HBox hbox = new HBox(5);
        hbox.setAlignment(Pos.CENTER_LEFT);
        bpOilSpill.setBottom(hbox);
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
            "Terminal", "", 300.0, 400.0);
        this.scene = scene;
        commandList = new ArrayList<>();
        bp = (BorderPane) this.contentPane;
        litLog = (LitLog) bp.getCenter();
        logToolbar = (LogToolbar) bp.getTop();
        commandTextField = new TextField("");
        commandTextField.prefWidthProperty().bind(litLog.widthProperty());
        commandTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN) {
                if (e.getCode() == KeyCode.UP) {
                    commandIndex++;
                    if (commandIndex >= commandList.size()) {
                        commandIndex = commandList.size() - 1;
                    }
                } else {
                    commandIndex--;
                    if (commandIndex < 0) {
                        commandIndex = 0;
                    }
                }
                //update the visible command
                commandTextField.setText(commandList.get(commandIndex));
            }
        });
        commandTextField.setOnAction(e -> {
            String fullCommand = commandTextField.getText();
            commandList.add(0, fullCommand);
            CommandRequest request = new CommandRequest();
            String[] tokens = fullCommand.split(" ");
            request.setRequest(tokens[0]);
            if (tokens.length > 1) {
                request.getProperties().put(CommandRequest.PAYLOAD, fullCommand.replaceFirst(tokens[0], ""));
            }
            CommandTask commandTask = new CommandTask(request, scene);
            Thread t = new Thread(commandTask, "Trinity Command Task " + fullCommand);
            t.setDaemon(true);
            t.start();
            addLine(commandTextField.getText(),
                new Font((String) logToolbar.fontChoiceBox.getValue(),
                    (int) logToolbar.fontSizeSpinner.getValue()),
                logToolbar.picker.getValue());
            commandTextField.clear();
        });
        HBox bottomHBox = (HBox) bp.getBottom();
        bottomHBox.getChildren().add(commandTextField);

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

        getStyleClass().add("text-pane");
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
        addText(currentText);
    }
    public void addText(String text) {
        addLine(text,
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
