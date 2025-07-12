package edu.jhuapl.trinity.utils.fun.planetary;

/**
 *
 * @author Sean Phillips
 */
import edu.jhuapl.trinity.javafx.events.EffectEvent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.event.ActionEvent;
import javafx.scene.Node;

public class PlanetaryStyleControls extends VBox {

    private final ComboBox<PlanetaryEffectFactory.PlanetStyle> styleComboBox = new ComboBox<>();
    private final ListView<EffectControlItem> listView = new ListView<>();
    private PlanetaryDisc targetDisc;

    public PlanetaryStyleControls(PlanetaryDisc planetaryDisc) {
        this.targetDisc = planetaryDisc;

        setSpacing(10);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #222; -fx-border-color: #444;");

        Label title = new Label("Planet Style Controls");
        title.setFont(Font.font("Arial", 16));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

        Label comboLabel = new Label("Planet Presets");
        comboLabel.setStyle("-fx-text-fill: lightgray;");

        styleComboBox.getItems().addAll(PlanetaryEffectFactory.PlanetStyle.values());
        styleComboBox.getSelectionModel().select(targetDisc.getPlanetStyle());

        styleComboBox.setOnAction(this::handleStyleChange);

        listView.setPrefHeight(400);
        listView.setPrefWidth(500);
        listView.setFocusTraversable(false);
        listView.setMouseTransparent(false);

        getChildren().addAll(
            title,
            new HBox(10, new VBox(5, comboLabel, styleComboBox)),
            new Label("Effect Controls"),
            listView
        );

        populateEffectControls();
    }

    private void handleStyleChange(ActionEvent e) {
        PlanetaryEffectFactory.PlanetStyle selectedStyle = styleComboBox.getValue();
        if (selectedStyle != null) {
            targetDisc.setPlanetStyle(selectedStyle);
            populateEffectControls();

            EffectEvent event = new EffectEvent(EffectEvent.PLANETARY_STYLE_CHANGE, selectedStyle.name());
            fireEvent(event);
        }
    }

    private void populateEffectControls() {
        listView.getItems().clear();
        for (Node node : targetDisc.getChildren()) {
            Object tag = node.getUserData();
            if (tag instanceof PlanetaryEffect) {
                listView.getItems().add(new EffectControlItem(node, (PlanetaryEffect) tag));
            }
        }
    }

    public void setPlanetaryDisc(PlanetaryDisc newDisc) {
        this.targetDisc = newDisc;
        if (newDisc != null && newDisc.getStyle() != null) {
            styleComboBox.getSelectionModel().select(newDisc.getPlanetStyle());
            populateEffectControls();
        }
    }
}
