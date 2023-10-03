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
