package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.Dimension;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * @author Sean Phillips
 */
public class DimensionLabelItem extends HBox {
    private Label indexLabel;
    private TextField dimensionLabelTextField;
    public Dimension dimension;

    public DimensionLabelItem(Dimension dimension) {
        this.dimension = dimension;
        indexLabel = new Label(String.valueOf(dimension.index));
        dimensionLabelTextField = new TextField(dimension.labelString);
        getChildren().addAll(indexLabel,
            new Separator(Orientation.VERTICAL), dimensionLabelTextField);
        setSpacing(5);

        dimensionLabelTextField.textProperty().addListener(e -> {
            dimension.labelString = dimensionLabelTextField.getText();
            getScene().getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.DIMENSION_LABEL_UPDATE, dimension));
        });
    }

}
