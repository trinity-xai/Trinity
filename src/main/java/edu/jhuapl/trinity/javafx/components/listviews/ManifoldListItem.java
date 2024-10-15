/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.listviews;


import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static edu.jhuapl.trinity.data.Manifold.globalManifoldToManifold3DMap;

/**
 * @author Sean Phillips
 */
public class ManifoldListItem extends VBox {

    private String labelString;
    private CheckBox visibleCheckBox;
    private TextField manifoldLabelTextField;
    private Manifold manifold;
    public boolean reactive = true;

    public ManifoldListItem(Manifold manifold) {
        this.manifold = manifold;
        labelString = manifold.getLabel();
        manifoldLabelTextField = new TextField(labelString);
        manifoldLabelTextField.setPrefWidth(200);
        visibleCheckBox = new CheckBox("Show");
        visibleCheckBox.setSelected(true);

        ImageView manifoldIcon = ResourceUtils.loadIcon("manifold", 32);

        HBox topHBox = new HBox(5, manifoldIcon, visibleCheckBox, manifoldLabelTextField);

        getChildren().addAll(topHBox);//, bottomHBox);
        setSpacing(2);
        visibleCheckBox.selectedProperty().addListener(cl -> {
            if (null != visibleCheckBox.getScene()) {
                this.manifold.setVisible(visibleCheckBox.isSelected());
                if (reactive) {
                    Manifold.updateManifold(this.manifold.getLabel(), this.manifold);
                    Manifold3D m3D = globalManifoldToManifold3DMap.get(this.manifold);
                    m3D.setVisible(manifold.getVisible());
                    getScene().getRoot().fireEvent(new ManifoldEvent(
                        ManifoldEvent.MANIFOLD_3D_VISIBLE, manifold.getVisible()));
                }
            }
        });
        manifoldIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() > 1) {
                //Let application know this distance object has been selected
                getScene().getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.MANIFOLD_OBJECT_SELECTED, this.manifold));
                getScene().getRoot().fireEvent(
                    new ManifoldEvent(ManifoldEvent.MANIFOLD_3D_SELECTED, this.manifold));
            }
        });
        setOnMouseClicked(e -> {
            //Let application know this distance object has been selected
            getScene().getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_OBJECT_SELECTED, this.getManifold()));
        });
        manifoldLabelTextField.textProperty().addListener(e -> {
            labelString = manifoldLabelTextField.getText();
            this.manifold.setLabel(labelString);
        });
    }

    public boolean getDataVisible() {
        return visibleCheckBox.isSelected();
    }

    public void setDataVisible(boolean visible) {
        visibleCheckBox.setSelected(visible);
    }

    /**
     * @return the manifold
     */
    public Manifold getManifold() {
        return manifold;
    }

    /**
     * @param manifold the manifold to set
     */
    public void setManifold(Manifold manifold) {
        this.manifold = manifold;
    }

}
