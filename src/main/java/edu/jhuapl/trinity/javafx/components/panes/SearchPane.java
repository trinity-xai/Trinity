package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.FilterBox;
import edu.jhuapl.trinity.javafx.components.SearchBox;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class SearchPane extends LitPathPane {
    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;
    BorderPane bp;
    TabPane tp;

    private static BorderPane createContent() {
        SearchBox searchBox = new SearchBox();
        Tab searchTab = new Tab("Search", searchBox);
        FilterBox filterBox = new FilterBox();
        Tab filterTab = new Tab("Filters", filterBox);

        TabPane tabPane = new TabPane(searchTab, filterTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
        BorderPane bpOilSpill = new BorderPane(tabPane);
        return bpOilSpill;
    }

    public SearchPane(Scene scene, Pane parent) {
        super(scene, parent, 450, 350, createContent(), "Introspection ", "", 200.0, 300.0);
        this.scene = scene;

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
