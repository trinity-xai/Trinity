package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.javafx3d.animated.RadialGrid;
import edu.jhuapl.trinity.javafx.javafx3d.animated.RadialGridControlBox;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class RadialGridControlsPane extends LitPathPane {
    public static int PANE_WIDTH = 350;
    public static int PANE_HEIGHT = 600;
    BorderPane bp;
    RadialGrid radialGrid;
    RadialGridControlBox controls;

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public RadialGridControlsPane(Scene scene, Pane parent, RadialGrid radialGrid) {
        super(scene, parent, PANE_WIDTH, PANE_HEIGHT, createContent(),
            "Radial Grid", "", 300.0, 400.0);
        this.scene = scene;
        this.radialGrid = radialGrid;
        bp = (BorderPane) this.contentPane;
        controls = new RadialGridControlBox(radialGrid);
        bp.setCenter(new ScrollPane(controls));
    }
}
