package edu.jhuapl.trinity.javafx.handlers;

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

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.data.files.ManifoldDataFile;
import edu.jhuapl.trinity.data.messages.ManifoldData;
import edu.jhuapl.trinity.data.messages.P3D;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.javafx.renderers.ManifoldRenderer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Sean Phillips
 */
public class ManifoldEventHandler implements EventHandler<ManifoldEvent> {

    List<ManifoldRenderer> manifoldRenderers;

    public ManifoldEventHandler() {
        manifoldRenderers = new ArrayList<>();
    }

    public void addManifoldRenderer(ManifoldRenderer renderer) {
        manifoldRenderers.add(renderer);
    }

    public void handleClearAllManifolds(ManifoldEvent event) {
        App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent("Clearing Manifolds... ",
            new Font("Consolas", 20), Color.GREEN));

        System.out.print("Clearing Manifolds... ");
        for (ManifoldRenderer renderer : manifoldRenderers) {
            renderer.clearAllManifolds();
        }
        Manifold.removeAllManifolds();
        System.out.println("Complete.");
        App.getAppScene().getRoot().fireEvent(new CommandTerminalEvent("Complete."));
    }

    public void handleScale(ManifoldEvent event) {
        double scale = (double) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        manifold.setScaleX(scale);
                        manifold.setScaleY(scale);
                        manifold.setScaleZ(scale);
                    }
                }
            }
    }

    public void handleRotate(ManifoldEvent event) {
        double rotate = (double) event.object1;
        Point3D axis = Rotate.X_AXIS;
        if (event.getEventType().equals(ManifoldEvent.MANIFOLD_ROTATE_Y))
            axis = Rotate.Y_AXIS;
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_ROTATE_Z))
            axis = Rotate.Z_AXIS;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        manifold.setRotationAxis(axis);
                        manifold.setRotate(rotate);
                    }
                }
            }
    }

    public void handleYPR(ManifoldEvent event) {
        double[] ypr = (double[]) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        manifold.matrixRotate(
                            Math.toRadians(ypr[0]),
                            Math.toRadians(ypr[1]),
                            Math.toRadians(ypr[2]));
                    }
                }
            }
    }

    public void handleDrawModeFill(ManifoldEvent event) {
        boolean fill = (boolean) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        if (fill)
                            manifold.quickhullMeshView.setDrawMode(DrawMode.FILL);
                        else
                            manifold.quickhullMeshView.setDrawMode(DrawMode.LINE);
                    }
                }
            }
    }

    public void handleDrawModeLine(ManifoldEvent event) {
        boolean line = (boolean) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        if (line)
                            manifold.quickhullMeshView.setDrawMode(DrawMode.LINE);
                        else
                            manifold.quickhullMeshView.setDrawMode(DrawMode.FILL);
                    }
                }
            }
    }

    public void handleCullFaceFront(ManifoldEvent event) {
        boolean front = (boolean) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        if (front)
                            manifold.quickhullMeshView.setCullFace(CullFace.FRONT);
                        else
                            manifold.quickhullMeshView.setCullFace(CullFace.NONE);
                    }
                }
            }
    }

    public void handleCullFaceBack(ManifoldEvent event) {
        boolean back = (boolean) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        if (back)
                            manifold.quickhullMeshView.setCullFace(CullFace.BACK);
                        else
                            manifold.quickhullMeshView.setCullFace(CullFace.NONE);
                    }
                }
            }
    }

    public void handleCullFaceNone(ManifoldEvent event) {
        boolean none = (boolean) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        if (none)
                            manifold.quickhullMeshView.setCullFace(CullFace.NONE);
                        else
                            manifold.quickhullMeshView.setCullFace(CullFace.BACK);
                    }
                }
            }
    }

    public void handleShowWireframe(ManifoldEvent event) {
        boolean showWireFrame = (boolean) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        manifold.quickhullLinesMeshView.setVisible(showWireFrame);
                    }
                }
            }
    }

    public void handleShowControl(ManifoldEvent event) {
        boolean showControl = (boolean) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        manifold.extrasGroup.getChildren().forEach(n -> n.setVisible(showControl));
                        manifold.labelGroup.getChildren().forEach(n -> n.setVisible(showControl));
                    }
                }
            }
    }

    public void handleDiffuseColor(ManifoldEvent event) {
        Color color = (Color) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D && null != color)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        ((PhongMaterial) manifold.quickhullMeshView.getMaterial()).setDiffuseColor(color);
                    }
                }
            }
    }

    public void handleSpecularColor(ManifoldEvent event) {
        Color color = (Color) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        ((PhongMaterial) manifold.quickhullMeshView.getMaterial()).setSpecularColor(color);
                    }
                }
            }
    }

    public void handleWireFrameColor(ManifoldEvent event) {
        Color color = (Color) event.object1;
        Manifold m = (Manifold) event.object2;
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(m);
        if (null != manifold3D)
            for (ManifoldRenderer renderer : manifoldRenderers) {
                for (Node node : renderer.getManifoldViews().getChildren()) {
                    if (node instanceof Manifold3D manifold && manifold == manifold3D) {
                        ((PhongMaterial) manifold.quickhullLinesMeshView.getMaterial()).setDiffuseColor(color);
                        manifold.extrasGroup.getChildren().stream()
                            .filter(extra -> extra instanceof Sphere)
                            .forEach(e -> {
                                ((PhongMaterial) ((Sphere) e).getMaterial()).setDiffuseColor(color);
                            });
                    }
                }
            }
    }

    public void handleExport(ManifoldEvent event) {
        File file = (File) event.object1;
        Manifold3D manifold3D = (Manifold3D) event.object2;
        ManifoldData md = new ManifoldData();
        List<org.fxyz3d.geometry.Point3D> points = manifold3D.getOriginalPoint3DList();
        ArrayList<P3D> p3Ds = points.stream().map(P3D.fxyzPoint3DToP3D)
            .collect(Collectors.toCollection(ArrayList::new));
        md.setPoints(p3Ds);
        try {
            ManifoldDataFile mdf = new ManifoldDataFile(file.getAbsolutePath(), false);
            mdf.manifoldData = md;
            mdf.writeContent();
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
        event.consume();
    }

    public void handleNewManifoldData(ManifoldEvent event) {
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(
                new CommandTerminalEvent("Loading Manifold Data...",
                    new Font("Consolas", 20), Color.GREEN));
        });
        System.out.println("Loading Manifold Data...");
        ManifoldData md = (ManifoldData) event.object1;
        //convert deserialized points to Fxyz3D point3ds
        List<org.fxyz3d.geometry.Point3D> points = md.getPoints().stream()
            .map(P3D.p3DToFxyzPoint3D)
            .collect(toList());
        ArrayList<Point3D> fxpoints = points.stream()
            .map(JavaFX3DUtils.fxyzPoint3DTofxPoint3D)
            .collect(Collectors.toCollection(ArrayList::new));
        Manifold manifold = new Manifold(fxpoints, "dudelabel", "dudename", Color.CYAN);
        Manifold3D manifold3D = new Manifold3D(points, true, true, false);
        //Add this Manifold data object to the global tracker
        Manifold.addManifold(manifold);
        //update the manifold to manifold3D mapping
        Manifold.globalManifoldToManifold3DMap.put(manifold, manifold3D);

        for (ManifoldRenderer renderer : manifoldRenderers) {
            renderer.addManifold(manifold, manifold3D);
        }
        Platform.runLater(() -> {
            App.getAppScene().getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD3D_OBJECT_GENERATED, manifold, manifold3D));
        });
    }

    @Override
    public void handle(ManifoldEvent event) {
        if (event.getEventType().equals(ManifoldEvent.GENERATE_PROJECTION_MANIFOLD)) {
            boolean useVisiblePoints = (boolean) event.object1;
            String label = (String) event.object2;
            for (ManifoldRenderer renderer : manifoldRenderers) {
                renderer.makeManifold(useVisiblePoints, label);
            }
        } else if (event.getEventType().equals(ManifoldEvent.CLEAR_ALL_MANIFOLDS))
            handleClearAllManifolds(event);
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_SET_SCALE)) {
            handleScale(event);
        } else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL)) {
            handleYPR(event);
        } else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_ROTATE_X)
            || event.getEventType().equals(ManifoldEvent.MANIFOLD_ROTATE_Y)
            || event.getEventType().equals(ManifoldEvent.MANIFOLD_ROTATE_Z)) {
            handleRotate(event);
        } else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_LINE_DRAWMODE))
            handleDrawModeLine(event);
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_FILL_DRAWMODE))
            handleDrawModeFill(event);
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_FRONT_CULLFACE))
            handleCullFaceFront(event);
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_BACK_CULLFACE))
            handleCullFaceBack(event);
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_NONE_CULLFACE))
            handleCullFaceNone(event);
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_SHOW_WIREFRAME))
            handleShowWireframe(event);
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_SHOW_CONTROL))
            handleShowControl(event);
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_DIFFUSE_COLOR))
            handleDiffuseColor(event);
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_SPECULAR_COLOR))
            handleSpecularColor(event);
        else if (event.getEventType().equals(ManifoldEvent.MANIFOLD_WIREFRAME_COLOR))
            handleWireFrameColor(event);
        else if (event.getEventType().equals(ManifoldEvent.EXPORT_MANIFOLD_DATA)) {
            handleExport(event);
        } else if (event.getEventType().equals(ManifoldEvent.NEW_MANIFOLD_DATA)) {
            handleNewManifoldData(event);
        }
    }
}
