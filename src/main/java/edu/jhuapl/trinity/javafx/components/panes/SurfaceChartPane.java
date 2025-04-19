package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.events.FactorAnalysisEvent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class SurfaceChartPane extends LitPathPane {
    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;
    BorderPane bp;
    FactorControlBox fcb;

    private static BorderPane createContent() {
        FactorControlBox fcb = new FactorControlBox(500, 400);
        BorderPane bpOilSpill = new BorderPane(fcb);
        return bpOilSpill;
    }

    public SurfaceChartPane(Scene scene, Pane parent) {
        super(scene, parent, 450, 350, createContent(), "Hypersurface Gradients ", "", 200.0, 300.0);
        this.scene = scene;
        bp = (BorderPane) this.contentPane;
        fcb = (FactorControlBox) bp.getCenter();

        this.scene.getRoot().addEventHandler(FactorAnalysisEvent.SURFACE_XFACTOR_VECTOR, e -> {
            fcb.setFactorVector(fcb.xFactorVector, (Double[]) e.object);
        });
        this.scene.getRoot().addEventHandler(FactorAnalysisEvent.SURFACE_ZFACTOR_VECTOR, e -> {
            fcb.setFactorVector(fcb.zFactorVector, (Double[]) e.object);
        });
    }
}
