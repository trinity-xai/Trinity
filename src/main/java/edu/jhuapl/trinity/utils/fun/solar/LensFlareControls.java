package edu.jhuapl.trinity.utils.fun.solar;

import edu.jhuapl.trinity.javafx.events.EffectEvent;
import edu.jhuapl.trinity.utils.fun.solar.FlarePresets.FlarePresetType;
import edu.jhuapl.trinity.utils.fun.solar.RetrowavePresetFactory.RetrowavePresetType;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * @author Sean Phillips
 */
public class LensFlareControls extends VBox {

    private final ListView<FlareControlItem> listView = new ListView<>();
    private LensFlareGroup lensFlareGroup;

    public LensFlareControls(LensFlareGroup lensFlareGroup) {
        this.lensFlareGroup = lensFlareGroup;
        setSpacing(10);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #222; -fx-border-color: #444;");

        Label title = new Label("Flare Controls");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

        ToggleButton enabledBtn = new ToggleButton("Enable Lens Flare");
        enabledBtn.setOnAction(e -> {
            getScene().getRoot().fireEvent(new EffectEvent(
                EffectEvent.LENSFLARE_ARTIFACT_ENABLED, enabledBtn.isSelected()));
        });

        listView.setPrefHeight(400);
        listView.setPrefWidth(500);
        listView.setFocusTraversable(false);
        listView.setMouseTransparent(false);

        updateFromFlares(lensFlareGroup);

        ComboBox<FlarePresetType> flarePresetsComboBox = new ComboBox<>();
        flarePresetsComboBox.getItems().addAll(FlarePresetType.values());
        flarePresetsComboBox.getSelectionModel().selectFirst();
        flarePresetsComboBox.setOnAction(e -> {
            FlarePresetType selected = flarePresetsComboBox.getValue();
            lensFlareGroup.setFlares(selected.create());
            updateFromFlares(lensFlareGroup);
            lensFlareGroup.update();
        });
        ComboBox<RetrowavePresetType> retrowavePresetsComboBox = new ComboBox<>();
        retrowavePresetsComboBox.getItems().addAll(RetrowavePresetType.values());
        retrowavePresetsComboBox.getSelectionModel().selectFirst();
        retrowavePresetsComboBox.setOnAction(e -> {
            RetrowavePresetType selected = retrowavePresetsComboBox.getValue();
            lensFlareGroup.setFlares(selected.create());
            updateFromFlares(lensFlareGroup);
            lensFlareGroup.update();
        });


        getChildren().addAll(title,
            enabledBtn,
            new HBox(10, new VBox(5, new Label("Lens Flare Presets"), flarePresetsComboBox)),
            new HBox(10, new VBox(5, new Label("RetroWave Presets"), retrowavePresetsComboBox)),
            listView);
    }

    public void updateFromFlares(LensFlareGroup flareGroup) {
        listView.getItems().clear();
        for (FlareSprite flare : flareGroup.getFlares()) {
            listView.getItems().add(new FlareControlItem(flare));
        }
    }

}
