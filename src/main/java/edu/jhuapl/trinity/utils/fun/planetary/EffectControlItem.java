package edu.jhuapl.trinity.utils.fun.planetary;

/**
 *
 * @author Sean Phillips
 */
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class EffectControlItem extends HBox {
    private final Node node;
    private final PlanetaryEffect effect;

    public EffectControlItem(Node node, PlanetaryEffect effect) {
        this.node = node;
        this.effect = effect;

        setSpacing(6);
        setPadding(new Insets(4));
        setAlignment(Pos.CENTER_LEFT);

        String label = effect.getClass().getSimpleName();

        // Visibility toggle
        CheckBox visibleBox = new CheckBox(label);
        visibleBox.setSelected(node.isVisible());
        visibleBox.selectedProperty().addListener((obs, oldVal, newVal) -> node.setVisible(newVal));
        visibleBox.setPrefWidth(150);

        // Opacity slider
        Slider opacitySlider = new Slider(0.0, 1.0, node.getOpacity());
        opacitySlider.setPrefWidth(125);
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> node.setOpacity(newVal.doubleValue()));

        getChildren().addAll(visibleBox,
            new VBox(5, new Label("Opacity"), opacitySlider));
    }
}
