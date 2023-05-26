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

import edu.jhuapl.trinity.javafx.events.SearchEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class SearchBox extends VBox {

    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;

    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));

    Background transFillBack = new Background(new BackgroundFill(
        Color.ALICEBLUE.deriveColor(1, 1, 1, 0.222), CornerRadii.EMPTY, Insets.EMPTY));


    public SearchBox() {
        Glow glow = new Glow(0.666);
        TextField searchTextField = new TextField();
        ImageView searchIV = new ImageView(ResourceUtils.loadIconFile("search"));
        searchIV.setSmooth(true);
        searchIV.setPreserveRatio(true);
        searchIV.setFitWidth(ICON_FIT_WIDTH);
        searchIV.setFitHeight(ICON_FIT_HEIGHT);
        searchIV.setOnMouseEntered(e -> searchIV.setCursor(Cursor.HAND));
        searchIV.setOnMouseExited(e -> searchIV.setCursor(Cursor.DEFAULT));
        searchIV.disableProperty().bind(searchTextField.textProperty().isEmpty());
        searchIV.setEffect(glow);
        searchIV.setOnMouseClicked(e -> {
            if (!searchTextField.getText().isBlank())
                searchTextField.getScene().getRoot().fireEvent(
                    new SearchEvent(SearchEvent.FILTER_BY_TERM, searchTextField.getText()));
        });
        ImageView resetIV = new ImageView(ResourceUtils.loadIconFile("reset"));
        resetIV.setSmooth(true);
        resetIV.setPreserveRatio(true);
        resetIV.setFitWidth(ICON_FIT_WIDTH);
        resetIV.setFitHeight(ICON_FIT_HEIGHT);
        resetIV.setOnMouseEntered(e -> resetIV.setCursor(Cursor.HAND));
        resetIV.setOnMouseExited(e -> resetIV.setCursor(Cursor.DEFAULT));
        resetIV.setEffect(glow);
        resetIV.setOnMouseClicked(e -> {
            searchTextField.setText("");
            searchTextField.getScene().getRoot().fireEvent(
                new SearchEvent(SearchEvent.CLEAR_ALL_FILTERS));
        });

        searchTextField.setPrefWidth(200);
        HBox searchHBox = new HBox(searchIV);
        searchHBox.setPrefWidth(ICON_FIT_WIDTH);
        searchHBox.setOnMouseEntered(e -> searchHBox.setBackground(transFillBack));
        searchHBox.setOnMouseExited(e -> searchHBox.setBackground(transBack));
        HBox resetHBox = new HBox(resetIV);
        resetHBox.setPrefWidth(ICON_FIT_WIDTH);
        resetHBox.setOnMouseEntered(e -> resetHBox.setBackground(transFillBack));
        resetHBox.setOnMouseExited(e -> resetHBox.setBackground(transBack));
        HBox controlHBox = new HBox(10, searchTextField, searchHBox, resetHBox);
        controlHBox.setAlignment(Pos.CENTER);

        Label textFieldLabel = new Label("Metadata search term");
        textFieldLabel.setPrefWidth(200);
        Label searchLabel = new Label("Search");
        searchLabel.setPrefWidth(ICON_FIT_WIDTH);
        Label resetLabel = new Label("Reset");
        resetLabel.setPrefWidth(ICON_FIT_WIDTH);
        HBox labelHBox = new HBox(10, textFieldLabel, searchLabel, resetLabel);

        setSpacing(10);
        setPadding(new Insets(5, 0, 0, 0));
        getChildren().addAll(labelHBox, controlHBox);
    }
}
