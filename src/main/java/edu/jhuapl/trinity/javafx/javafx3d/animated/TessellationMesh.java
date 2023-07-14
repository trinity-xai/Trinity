package edu.jhuapl.trinity.javafx.javafx3d.animated;

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

import edu.jhuapl.trinity.javafx.components.MatrixEffect;
import edu.jhuapl.trinity.javafx.javafx3d.HyperSurfacePlotMesh;
import edu.jhuapl.trinity.javafx.javafx3d.Vert3D;
import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * @author Sean Phillips
 */
public class TessellationMesh extends MeshView {
    public double DEFAULT_RATEOFCHANGE = 0.01;
    public double rateOfChange = DEFAULT_RATEOFCHANGE;
    public TriangleMesh triangleMesh;
    private TriangleMesh surfPlotMesh;
    private HyperSurfacePlotMesh surfPlot;
    public List<List<Double>> dataGrid = new ArrayList<>();
    public PhongMaterial phongMaterial;
    private AnimationTimer tessellationTimer;
    private AnimationTimer matrixTimer;
    public Image vectorizeImage;
    public Function<Vert3D, Number> vert3DLookup = p -> vertToHeight(p);
    public SimpleBooleanProperty animatingProperty = new SimpleBooleanProperty(false);
    public int pskip = 1;
    public float scale = 1.0f;
    public float maxH = 100;
    public boolean matrixEnabled = false;
    public Color color;
    public MatrixEffect neo;
    private Canvas canvas;
    private WritableImage image;
    public boolean colorByImage = false;

    public TessellationMesh(Image image, Color color, float scale, float maxH, int pskip, boolean colorByImage) {
        super();
        this.color = color;
        this.scale = scale;
        this.maxH = maxH;
        this.pskip = pskip;
        this.colorByImage = colorByImage;
        surfPlot = new HyperSurfacePlotMesh(
            Double.valueOf(image.getWidth()).intValue(),
            Double.valueOf(image.getHeight()).intValue(),
            64, 64, 5, 5, vert3DLookup);
        surfPlot.setTextureModeVertices3D(1530, p -> p.y, 0.0, 360.0);

        vectorizeImage = image;
        triangleMesh = new TriangleMesh();
        if (colorByImage)
            phongMaterial = new PhongMaterial(color, vectorizeImage, null, null, null);
        else
            phongMaterial = new PhongMaterial(color);
        setDrawMode(DrawMode.LINE);
        setCullFace(CullFace.NONE);
        tessellateImage(vectorizeImage, scale, maxH, pskip);
        setMesh(triangleMesh);
        setMaterial(phongMaterial);
        updateMaterial(vectorizeImage);
        canvas = new Canvas(vectorizeImage.getWidth() * 4, vectorizeImage.getHeight() * 4);
        neo = new MatrixEffect(canvas);
    }

    public void enableMatrix(boolean enable) {
        matrixEnabled = enable;
        if (matrixEnabled) {
            neo.start();
            animateMatrix(15);
        } else {
            neo.stop();
            matrixTimer.stop();
        }
    }

    public void animateMatrix(long ms) {
        if (null != matrixTimer)
            matrixTimer.stop();
        SnapshotParameters sp = new SnapshotParameters();
        image = this.snapshot(sp, null);

        matrixTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;

            @Override
            public void handle(long now) {
                sleepNs = ms * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) return;
                prevTime = now;
                image = canvas.snapshot(sp, null);
                updateMaterial(image);
            }

            ;
        };
        matrixTimer.start();
    }

    public void updateMaterial(Image image) {
        if (colorByImage)
            phongMaterial.setDiffuseMap(image);
        else
            phongMaterial.setDiffuseColor(color);
        phongMaterial.setSelfIlluminationMap(image);
        phongMaterial.setBumpMap(image);
        phongMaterial.setSpecularColor(Color.CYAN);
    }

    public void animateTessellation(long ms, int rows) {
        if (null != tessellationTimer)
            tessellationTimer.stop();

        triangleMesh.getFaces().clear();
        triangleMesh.getFaces().ensureCapacity(surfPlotMesh.getFaces().size());

        tessellationTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;
            int faceIndex = 0;

            @Override
            public void handle(long now) {
                sleepNs = ms * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) return;
                prevTime = now;

                int facesRowWidth = ((int) vectorizeImage.getWidth() * 6) * 2;
                int newFaces = facesRowWidth * rows;

                if (faceIndex + newFaces > surfPlotMesh.getFaces().size()) {
                    newFaces = surfPlotMesh.getFaces().size() - faceIndex;
                }
                triangleMesh.getFaces().addAll(surfPlotMesh.getFaces(), faceIndex, newFaces);

                faceIndex += newFaces;
                if (faceIndex >= surfPlotMesh.getFaces().size()) {
                    this.stop();
                    System.out.println("Tessellation Complete.");
                }
            }

            ;
        };
        tessellationTimer.start();
    }

    public void tessellateImage(Image image, float scale, float maxH, int pskip) {

        int rgb, r, g, b;
        // Create points and texCoords
        dataGrid.clear();
        List<Double> newRow;
        int subDivX = (int) image.getWidth() / pskip;
        int subDivZ = (int) image.getHeight() / pskip;
        int numDivX = subDivX + 1;
        int numVerts = (subDivZ + 1) * numDivX;
        final int texCoordSize = 2;
        float currZ, currX;
        float texCoords[] = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivZ * 2;
        final int faceSize = 6; //should always be 6 for a triangle mesh
        int faces[] = new int[faceCount * faceSize];
        int index, p00, p01, p10, p11, tc00, tc01, tc10, tc11;
        double yValue;
        for (int z = 0; z < subDivZ; z++) {
            currZ = (float) z / subDivZ;
            newRow = new ArrayList<>(subDivX);
            for (int x = 0; x < subDivX; x++) {
                currX = (float) x / subDivX;
                // color value for pixel at point
                rgb = ((int) image.getPixelReader().getArgb(x * pskip, z * pskip));
                r = (rgb >> 16) & 0xFF;
                g = (rgb >> 8) & 0xFF;
                b = rgb & 0xFF;
                yValue = (((r + g + b) / 3.0f) / 255.0f) * maxH;
                newRow.add(yValue);

                index = z * numDivX * texCoordSize + (x * texCoordSize);
                texCoords[index] = currX;
                texCoords[index + 1] = currZ;

                // Create faces
                p00 = z * numDivX + x;
                p01 = p00 + 1;
                p10 = p00 + numDivX;
                p11 = p10 + 1;
                tc00 = z * numDivX + x;
                tc01 = tc00 + 1;
                tc10 = tc00 + numDivX;
                tc11 = tc10 + 1;

                index = (z * subDivX * faceSize + (x * faceSize)) * 2;
                faces[index + 0] = p00;
                faces[index + 1] = tc00;
                faces[index + 2] = p10;
                faces[index + 3] = tc10;
                faces[index + 4] = p11;
                faces[index + 5] = tc11;

                index += faceSize;
                faces[index + 0] = p11;
                faces[index + 1] = tc11;
                faces[index + 2] = p01;
                faces[index + 3] = tc01;
                faces[index + 4] = p00;
                faces[index + 5] = tc00;
            }
            dataGrid.add(newRow);
        }
        surfPlot.updateMeshRaw(subDivX, subDivZ, scale, scale, scale);
        surfPlotMesh = ((TriangleMesh) surfPlot.getMesh());
        surfPlotMesh.getTexCoords().setAll(texCoords);
        surfPlotMesh.getFaces().setAll(faces);
        //update the mesh that will be rendered... faces are done later during animation
        triangleMesh.getPoints().setAll(surfPlotMesh.getPoints());
        triangleMesh.getTexCoords().setAll(surfPlotMesh.getTexCoords());
    }

    private Number vertToHeight(Vert3D p) {
        if (null != dataGrid) {
            return lookupPoint(p);
        } else
            return 0.0;
    }

    private Number lookupPoint(Vert3D p) {
        //hacky bounds check
        if (p.yIndex >= dataGrid.size()
            || p.xIndex >= dataGrid.get(0).size())
            return 0.0;
        return dataGrid.get(p.yIndex).get(p.xIndex);
    }

}
