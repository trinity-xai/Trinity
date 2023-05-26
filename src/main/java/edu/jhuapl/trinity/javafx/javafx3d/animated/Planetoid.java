package edu.jhuapl.trinity.javafx.javafx3d.animated;

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

import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.util.Duration;

import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class Planetoid extends Sphere {
    public Color color;
    public PhongMaterial material;
    public double radius;
    public SimpleDoubleProperty scalingBind = null;
    public boolean animateOnHover = false;

    public Planetoid(PhongMaterial material, double radius, int divisions) {
        super(radius, divisions);
        this.radius = radius;
        this.material = material;
        setMaterial(this.material);
        setOnMouseEntered(mouseEnter -> {
            if (animateOnHover) {
                expand();
            }
        });
        setOnMouseExited(mouseExit -> {
            if (animateOnHover) {
                contract();
            }
        });
        addEventHandler(DragEvent.DRAG_OVER, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() &&
                JavaFX3DUtils.isTextureFile(db.getFiles().get(0))) {
                event.acceptTransferModes(TransferMode.COPY);
            }
        });
        addEventHandler(DragEvent.DRAG_ENTERED, event -> expand());
        addEventHandler(DragEvent.DRAG_EXITED, event -> contract());

        // Dropping over surface
        addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() &&
                JavaFX3DUtils.isTextureFile(db.getFiles().get(0))) {
                Image textureImage;
                try {
                    textureImage = new Image(db.getFiles().get(0).toURI().toURL().toExternalForm());
                    material.setDiffuseMap(textureImage);
                    event.setDropCompleted(true);
                } catch (MalformedURLException ex) {
                    Logger.getLogger(Planetoid.class.getName()).log(Level.SEVERE, null, ex);
                    event.setDropCompleted(false);
                }
                event.consume();
            }
        });
    }

    public void bindScale(SimpleDoubleProperty scaleProp) {
        scalingBind = scaleProp;
        scalingBind.addListener((obs, oV, nv) -> {
            setRadius(radius * nv.doubleValue());
        });
//        scaleXProperty().bind(scaleProp);
//        scaleYProperty().bind(scaleProp);
//        scaleZProperty().bind(scaleProp);
    }

    private void expand() {
//        this.scaleXProperty().unbind();
//        this.scaleYProperty().unbind();
//        this.scaleZProperty().unbind();
        ScaleTransition outTransition =
            new ScaleTransition(Duration.millis(50), this);
        outTransition.setToX(null != scalingBind ? scalingBind.doubleValue() * 3.0 : 3f);
        outTransition.setToY(null != scalingBind ? scalingBind.doubleValue() * 3.0 : 3f);
        outTransition.setToZ(null != scalingBind ? scalingBind.doubleValue() * 3.0 : 3f);
        outTransition.setCycleCount(1);
        outTransition.setAutoReverse(false);
        outTransition.setInterpolator(Interpolator.EASE_OUT);
        outTransition.play();
    }

    private void contract() {
//        this.scaleXProperty().unbind();
//        this.scaleYProperty().unbind();
//        this.scaleZProperty().unbind();
        ScaleTransition inTransition =
            new ScaleTransition(Duration.millis(50), this);
        inTransition.setToX(null != scalingBind ? scalingBind.doubleValue() : 1f);
        inTransition.setToY(null != scalingBind ? scalingBind.doubleValue() : 1f);
        inTransition.setToZ(null != scalingBind ? scalingBind.doubleValue() : 1f);
        inTransition.setCycleCount(1);
        inTransition.setAutoReverse(false);
        inTransition.setInterpolator(Interpolator.EASE_OUT);
        inTransition.play();
        if (null != scalingBind)
            inTransition.setOnFinished(e -> bindScale(scalingBind));
    }

    public TriangleMesh getMesh() {
        return createMesh(this.getDivisions(), Double.valueOf(radius).floatValue());
    }

    private static int correctDivisions(int div) {
        return ((div + 3) / 4) * 4;
    }

    public static TriangleMesh createMesh(int div, float r) {
        div = correctDivisions(div);

        // NOTE: still create mesh for degenerated sphere
        final int div2 = div / 2;

        final int nPoints = div * (div2 - 1) + 2;
        final int nTPoints = (div + 1) * (div2 - 1) + div * 2;
        final int nFaces = div * (div2 - 2) * 2 + div * 2;

        final float rDiv = 1.f / div;

        float points[] = new float[nPoints * 3];
        float tPoints[] = new float[nTPoints * 2];
        int faces[] = new int[nFaces * 6];

        int pPos = 0, tPos = 0;

        for (int y = 0; y < div2 - 1; ++y) {
            float va = rDiv * (y + 1 - div2 / 2) * 2 * (float) Math.PI;
            float sin_va = (float) Math.sin(va);
            float cos_va = (float) Math.cos(va);

            float ty = 0.5f + sin_va * 0.5f;
            for (int i = 0; i < div; ++i) {
                double a = rDiv * i * 2 * (float) Math.PI;
                float hSin = (float) Math.sin(a);
                float hCos = (float) Math.cos(a);
                points[pPos + 0] = hSin * cos_va * r;
                points[pPos + 2] = hCos * cos_va * r;
                points[pPos + 1] = sin_va * r;
                tPoints[tPos + 0] = 1 - rDiv * i;
                tPoints[tPos + 1] = ty;
                pPos += 3;
                tPos += 2;
            }
            tPoints[tPos + 0] = 0;
            tPoints[tPos + 1] = ty;
            tPos += 2;
        }

        points[pPos + 0] = 0;
        points[pPos + 1] = -r;
        points[pPos + 2] = 0;
        points[pPos + 3] = 0;
        points[pPos + 4] = r;
        points[pPos + 5] = 0;
        pPos += 6;

        int pS = (div2 - 1) * div;

        float textureDelta = 1.f / 256;
        for (int i = 0; i < div; ++i) {
            tPoints[tPos + 0] = rDiv * (0.5f + i);
            tPoints[tPos + 1] = textureDelta;
            tPos += 2;
        }

        for (int i = 0; i < div; ++i) {
            tPoints[tPos + 0] = rDiv * (0.5f + i);
            tPoints[tPos + 1] = 1 - textureDelta;
            tPos += 2;
        }

        int fIndex = 0;
        for (int y = 0; y < div2 - 2; ++y) {
            for (int x = 0; x < div; ++x) {
                int p0 = y * div + x;
                int p1 = p0 + 1;
                int p2 = p0 + div;
                int p3 = p1 + div;

                int t0 = p0 + y;
                int t1 = t0 + 1;
                int t2 = t0 + (div + 1);
                int t3 = t1 + (div + 1);

                // add p0, p1, p2
                faces[fIndex + 0] = p0;
                faces[fIndex + 1] = t0;
                faces[fIndex + 2] = p1 % div == 0 ? p1 - div : p1;
                faces[fIndex + 3] = t1;
                faces[fIndex + 4] = p2;
                faces[fIndex + 5] = t2;
                fIndex += 6;

                // add p3, p2, p1
                faces[fIndex + 0] = p3 % div == 0 ? p3 - div : p3;
                faces[fIndex + 1] = t3;
                faces[fIndex + 2] = p2;
                faces[fIndex + 3] = t2;
                faces[fIndex + 4] = p1 % div == 0 ? p1 - div : p1;
                faces[fIndex + 5] = t1;
                fIndex += 6;
            }
        }

        int p0 = pS;
        int tB = (div2 - 1) * (div + 1);
        for (int x = 0; x < div; ++x) {
            int p2 = x, p1 = x + 1, t0 = tB + x;
            faces[fIndex + 0] = p0;
            faces[fIndex + 1] = t0;
            faces[fIndex + 2] = p1 == div ? 0 : p1;
            faces[fIndex + 3] = p1;
            faces[fIndex + 4] = p2;
            faces[fIndex + 5] = p2;
            fIndex += 6;
        }

        p0 = p0 + 1;
        tB = tB + div;
        int pB = (div2 - 2) * div;

        for (int x = 0; x < div; ++x) {
            int p1 = pB + x, p2 = pB + x + 1, t0 = tB + x;
            int t1 = (div2 - 2) * (div + 1) + x, t2 = t1 + 1;
            faces[fIndex + 0] = p0;
            faces[fIndex + 1] = t0;
            faces[fIndex + 2] = p1;
            faces[fIndex + 3] = t1;
            faces[fIndex + 4] = p2 % div == 0 ? p2 - div : p2;
            faces[fIndex + 5] = t2;
            fIndex += 6;
        }

        TriangleMesh m = new TriangleMesh();
        m.getPoints().setAll(points);
        m.getTexCoords().setAll(tPoints);
        m.getFaces().setAll(faces);
        return m;
    }

}
