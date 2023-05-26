package edu.jhuapl.trinity.javafx.components;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;

/**
 * @author Sean Phillips
 */
//CheckBoxTableCell for creating a CheckBox in a table cell
public class CheckBoxTableCell<S, T> extends TableCell<S, T> {
    private final CheckBox checkBox;
    private ObservableValue<T> ov;

    public CheckBoxTableCell() {
        this.checkBox = new CheckBox();
        this.checkBox.setAlignment(Pos.CENTER);

        setAlignment(Pos.CENTER);
        setGraphic(checkBox);
    }

    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            setGraphic(checkBox);
            if (ov instanceof BooleanProperty) {
                checkBox.selectedProperty().unbindBidirectional((BooleanProperty) ov);
            }
            ov = getTableColumn().getCellObservableValue(getIndex());
            if (ov instanceof BooleanProperty) {
                checkBox.selectedProperty().bindBidirectional((BooleanProperty) ov);
            }
            if (null != getTableRow()) {
                Object rowItem = getTableRow().getItem();
                if (null != rowItem) {
                    FactorLabel fl = (FactorLabel) rowItem;
                    //fl.setVisible(ov.getValue());
                    if (null != getScene()) {
                        getScene().getRoot().fireEvent(new HyperspaceEvent(
                            HyperspaceEvent.UPDATED_FACTOR_LABEL, fl));
                    }
                }
            }
        }
    }
}
