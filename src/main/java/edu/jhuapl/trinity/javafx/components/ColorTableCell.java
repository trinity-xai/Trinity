package edu.jhuapl.trinity.javafx.components;

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

import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;

/**
 * @param <T> The table row type
 * @author Sean Phillips
 */
public class ColorTableCell<T> extends TableCell<T, Color> {
    public final ColorPicker colorPicker;

    public ColorTableCell(TableColumn<T, Color> column) {
        this.colorPicker = new ColorPicker();
//        this.colorPicker.setPrefHeight(30);
        this.colorPicker.editableProperty().bind(column.editableProperty());
        this.colorPicker.disableProperty().bind(column.editableProperty().not());
        this.colorPicker.setOnShowing(event -> {
            final TableView<T> tableView = getTableView();
            tableView.getSelectionModel().select(getTableRow().getIndex());
            tableView.edit(tableView.getSelectionModel().getSelectedIndex(), column);
        });
        this.colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (null != getScene()) {
                FactorLabel fl = (FactorLabel) getTableRow().getItem();
                if (null != fl) {
                    fl.setColor(newValue);
                    FactorLabel.updateFactorLabel(fl.getLabel(), fl);
                    getScene().getRoot().fireEvent(new HyperspaceEvent(
                        HyperspaceEvent.UPDATED_FACTOR_LABEL, fl));
                }
            }
            if (isEditing()) {
                commitEdit(newValue);
            }
        });
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    protected void updateItem(Color item, boolean empty) {
        super.updateItem(item, empty);

        setText(null);
        if (empty) {
            setGraphic(null);
        } else {
            this.colorPicker.setValue(item);
            this.setGraphic(this.colorPicker);
        }
    }
}
