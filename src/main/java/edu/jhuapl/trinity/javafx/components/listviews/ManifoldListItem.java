package edu.jhuapl.trinity.javafx.components.listviews;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import edu.jhuapl.trinity.data.Manifold;
import static edu.jhuapl.trinity.data.Manifold.globalManifoldToManifold3DMap;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
        manifoldIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, e-> {
            if(e.getClickCount()>1){
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
        manifoldLabelTextField.textProperty().addListener(e-> {
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
