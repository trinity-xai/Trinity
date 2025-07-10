package edu.jhuapl.trinity.utils.fun;

import edu.jhuapl.trinity.utils.fun.FlarePresets.FlarePresetType;
import edu.jhuapl.trinity.utils.fun.RetrowavePresetFactory.RetrowavePresetType;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 *
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
