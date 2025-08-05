package edu.jhuapl.trinity.javafx.components.radial;

import javafx.animation.FadeTransition;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * @author Sean Phillips
 */
public class CircleProgressIndicator extends StackPane {

    public Label progressLabel;
    public SimpleStringProperty labelString;
    public Label topLabel;
    public SimpleStringProperty topLabelString;
    public double defaultOpacity = 0.75;
    private FadeTransition ft = new FadeTransition(Duration.seconds(1), this);
    public AnimatedNeonCircle outerNeonCircle, innerNeonCircle;
    public AnimatedFillCircle fillCircle;
    public SimpleDoubleProperty percentComplete = new SimpleDoubleProperty(0.0);

    public CircleProgressIndicator() {
        this(100.0, 70.0);
    }

    public CircleProgressIndicator(double outerRadius, double innerRadius) {
        setMouseTransparent(true);
        setAlignment(Pos.CENTER);
        progressLabel = new Label();
        labelString = new SimpleStringProperty();
        topLabel = new Label();
        topLabelString = new SimpleStringProperty();

        outerNeonCircle = new AnimatedNeonCircle(
            new AnimatedNeonCircle.Animation(
                Duration.millis(2000), Transition.INDEFINITE, false),
            outerRadius, 3, outerRadius * 0.5, outerRadius);
        //outerRadius, 3, outerRadius*0.75, outerRadius*0.5);
        outerNeonCircle.getStyleClass().add("outer-neon-circle");

        innerNeonCircle = new AnimatedNeonCircle(
            new AnimatedNeonCircle.Animation(
                Duration.millis(3000), Transition.INDEFINITE, false),
            innerRadius, 1.5, innerRadius * 0.8, innerRadius * 0.3);
        innerNeonCircle.getStyleClass().add("inner-neon-circle");

        fillCircle = new AnimatedFillCircle(innerRadius * 0.9, 0.0, 2.0, 1.0);
        fillCircle.getStyleClass().add("fill-circle");

        topLabel.setText("...executing...");
        topLabel.setTextAlignment(TextAlignment.CENTER);
        topLabel.setAlignment(Pos.CENTER);
        topLabelString.set("...executing...");
        topLabel.textProperty().bind(topLabelString);
        topLabel.setFont(new Font("Consolas", 24));
        topLabel.setMinSize(outerRadius, 25);


        progressLabel.setText("Loading...");
        progressLabel.setTextAlignment(TextAlignment.CENTER);
        progressLabel.setAlignment(Pos.CENTER);
        labelString.set("Loading...");
        progressLabel.textProperty().bind(labelString);
        progressLabel.setFont(new Font("Consolas", 24));
        progressLabel.setMinSize(outerRadius, 25);
        setAlignment(Pos.CENTER);
        getChildren().addAll(fillCircle, innerNeonCircle,
            outerNeonCircle, progressLabel, topLabel);

        topLabel.setTranslateY(-outerRadius - 5);
        topLabel.getStyleClass().add("progress-label");
        progressLabel.setTranslateY(outerRadius + 5);
        progressLabel.getStyleClass().add("progress-label");

        ft.setAutoReverse(false);
        ft.setCycleCount(0);

        fillCircle.percentComplete.bind(percentComplete);
        getStyleClass().add("circle-progress-indicator");
    }

    public void updateStatus(ProgressStatus ps) {
        if (null == ps)
            return;
        setLabelLater(ps.statusMessage);
        setTopLabelLater(ps.topMessage);
        if (ps.percentComplete < 0) {
            //@TODO SMP something fancier for indeterminate
            setPercentComplete(0);
        } else {
            setPercentComplete(ps.percentComplete);
        }
        innerNeonCircle.setStroke(ps.innerStrokeColor);
        outerNeonCircle.setStroke(ps.outerStrokeColor);
        fillCircle.fillStartColor = ps.fillStartColor;
        fillCircle.fillEndColor = ps.fillEndColor;
    }

    public void setTopLabelLater(final String newText) {
        Platform.runLater(() -> topLabelString.set(newText));
    }

    public void setLabelLater(final String newText) {
        Platform.runLater(() -> labelString.set(newText));
    }

    public void setPercentComplete(double percentComplete) {
        this.percentComplete.set(percentComplete);
    }

    public void spin(boolean spin) {
        outerNeonCircle.play(spin);
        innerNeonCircle.play(spin);
    }

    public void setFadeTimeMS(long millis) {
        ft.setDuration(Duration.millis(millis));
    }

    public void fadeBusy(boolean fadeOut) {
        if (fadeOut) {
            ft.setFromValue(defaultOpacity);
            ft.setToValue(0.0);
        } else {
            ft.setFromValue(0.0);
            ft.setToValue(defaultOpacity);
        }
        setVisible(true);
        ft.setOnFinished(e -> setVisible(!fadeOut));
        ft.playFromStart();
    }
    public boolean inView() {
        return getOpacity() >= defaultOpacity && isVisible();
    }
}
