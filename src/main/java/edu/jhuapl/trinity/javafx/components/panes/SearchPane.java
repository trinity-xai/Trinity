package edu.jhuapl.trinity.javafx.components.panes;

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

import edu.jhuapl.trinity.javafx.components.FilterBox;
import edu.jhuapl.trinity.javafx.components.SearchBox;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import lit.litfx.controls.covalent.PathPane;
import lit.litfx.controls.covalent.events.CovalentPaneEvent;

/**
 * @author Sean Phillips
 */
public class SearchPane extends PathPane {
    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;
    Scene scene;
    BorderPane bp;
    TabPane tp;

    private static BorderPane createContent() {
        SearchBox searchBox = new SearchBox();
        Tab searchTab = new Tab("Search", searchBox);
        FilterBox filterBox = new FilterBox();
        Tab filterTab = new Tab("Filters", filterBox);

        TabPane tabPane = new TabPane(searchTab, filterTab);
        BorderPane bpOilSpill = new BorderPane(tabPane);
        return bpOilSpill;
    }

    public SearchPane(Scene scene, Pane parent) {
        super(scene, parent, 450, 350, createContent(), "Introspection ", "", 200.0, 300.0);
        this.scene = scene;
        // must be set to prevent user from resizing too small.
        setMinWidth(300);
        setMinHeight(300);

        this.scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, e -> {
            if (e.pathPane == this)
                parent.getChildren().remove(this);
        });
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> this.toFront());

        bp = (BorderPane) this.contentPane;
        tp = (TabPane) bp.getCenter();
        tp.selectionModelProperty().addListener(cl -> {
            int i = tp.getSelectionModel().getSelectedIndex();
            switch (i) {
                case 0:
                    this.mainTitleText2Property.set("Search");
                case 1:
                    this.mainTitleText2Property.set("Filter");
            }
        });

    }

    public void showSearch() {
        tp.getSelectionModel().select(0);
    }

    public void showFilters() {
        tp.getSelectionModel().select(1);
    }
}
