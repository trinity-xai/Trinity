package edu.jhuapl.trinity.javafx.components.hyperdrive;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/**
 *
 * @author Birdasaur
 */
public class MappingRow {
    final CheckBox embed = new CheckBox();
    final RadioButton label = new RadioButton();
    final CheckBox metadata = new CheckBox();
    final HBox node;
    public MappingRow(String path, ToggleGroup group) {
        label.setToggleGroup(group);
        Label pathLabel = new Label(path);
        pathLabel.setMinWidth(340);
        pathLabel.setMaxWidth(340);
        pathLabel.setWrapText(true);
        node = new HBox(14, pathLabel, embed, label, metadata);
        node.setPadding(new Insets(4));
    }    
}
