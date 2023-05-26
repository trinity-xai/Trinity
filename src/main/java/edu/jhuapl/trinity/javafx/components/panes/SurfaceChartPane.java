package edu.jhuapl.trinity.javafx.components.panes;

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

import edu.jhuapl.trinity.javafx.events.FactorAnalysisEvent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import lit.litfx.controls.covalent.PathPane;
import lit.litfx.controls.covalent.events.CovalentPaneEvent;

/**
 * @author Sean Phillips
 */
public class SurfaceChartPane extends PathPane {
    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;
    Scene scene;
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
        // must be set to prevent user from resizing too small.
        setMinWidth(300);
        setMinHeight(300);
        bp = (BorderPane) this.contentPane;
        fcb = (FactorControlBox) bp.getCenter();

        this.scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, e -> {
            if (e.pathPane == this)
                parent.getChildren().remove(this);
        });
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> this.toFront());
        this.scene.getRoot().addEventHandler(FactorAnalysisEvent.SURFACE_XFACTOR_VECTOR, e -> {
            fcb.setFactorVector(fcb.xFactorVector, (Double[]) e.object);
        });
        this.scene.getRoot().addEventHandler(FactorAnalysisEvent.SURFACE_ZFACTOR_VECTOR, e -> {
            fcb.setFactorVector(fcb.zFactorVector, (Double[]) e.object);
        });
    }

}
