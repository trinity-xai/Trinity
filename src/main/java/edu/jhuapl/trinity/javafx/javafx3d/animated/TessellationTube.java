package edu.jhuapl.trinity.javafx.javafx3d.animated;

import edu.jhuapl.trinity.utils.Utils;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Sean Phillips
 */
public class TessellationTube extends Group {
    private static final Logger LOG = LoggerFactory.getLogger(TessellationTube.class);
    public MeshView meshView;
    public TriangleMesh triangleMesh;
    public List<List<Double>> dataGrid = new ArrayList<>();
    public PhongMaterial phongMaterial;
    public Color color;
    public boolean colorByImage = false;

    public TessellationTube(List<List<Double>> dataGrid, Color color,
                            double radius, double rowYSpacing, double elevationScale) {
        this.dataGrid = dataGrid;
        this.color = color;
        triangleMesh = buildWarp(dataGrid, radius, rowYSpacing, elevationScale);
        meshView = new MeshView(triangleMesh);
        phongMaterial = new PhongMaterial(color);
        meshView.setMaterial(phongMaterial);
        meshView.setDrawMode(DrawMode.LINE);
        meshView.setCullFace(CullFace.NONE);
        getChildren().add(meshView);
        setDepthTest(DepthTest.ENABLE);
    }

    public void updateMaterial(Image image) {
        if (colorByImage) {
            phongMaterial.setDiffuseMap(image);
            phongMaterial.setSelfIlluminationMap(image);
            phongMaterial.setDiffuseColor(Color.WHITE);
        } else
            phongMaterial.setDiffuseColor(color);
    }

    public TriangleMesh buildWarp(List<List<Double>> dataGrid, double radius, double rowYSpacing, double elevationScale) {
        LOG.info("warping mesh...");
        long startTime = System.nanoTime();
        int faceGroupSize = 2; //2 for only outer wrap, 4 for sides.
        int columnWidth = dataGrid.get(0).size() - 1;
        int rowHeight = dataGrid.size() - 1;
        float[] pointFloats = new float[3 * rowHeight * columnWidth];
        int pointFloatIndex = 0;
        double degreeSpacing = 360.0 / columnWidth;
        double currentY = 0;
        final int texCoordSize = 2;
        int numDivX = columnWidth;
        int numVerts = (rowHeight + 1) * numDivX;
        final int faceSize = 6; //should always be 6 for a triangle mesh
        int faceCount = rowHeight * columnWidth * faceGroupSize;
        int faces[] = new int[faceCount * faceSize];
        float texCoords[] = new float[numVerts * texCoordSize];
        float tIndex, tSize;
        int index, p00, p01, p10, p11, tc00, tc01, tc10, tc11;

        //make a warped version of the dataGrid
        //for each row
        for (int rowIndex = 0; rowIndex < rowHeight; rowIndex++) {
            double currentDegree = 0;
            for (int colIndex = 0; colIndex < columnWidth; colIndex++) {
                double d = dataGrid.get(rowIndex).get(colIndex);
                //compute vertex point locations using circle formula
                Double x = ((d * elevationScale) + radius) * Math.cos(Math.toRadians(currentDegree));
                Double y = currentY;
                Double z = ((d * elevationScale) + radius) * -Math.sin(Math.toRadians(currentDegree));
                pointFloats[pointFloatIndex++] = x.floatValue();
                pointFloats[pointFloatIndex++] = y.floatValue();
                pointFloats[pointFloatIndex++] = z.floatValue();
                currentDegree += degreeSpacing;

                //Texture Coordinates
                index = rowIndex * numDivX * texCoordSize + (colIndex * texCoordSize);

                tIndex = colIndex;
                tSize = columnWidth;
                texCoords[index] = tIndex / tSize;
                tIndex = rowIndex;
                tSize = rowHeight;
                texCoords[index + 1] = tIndex / tSize;

                if (rowIndex < rowHeight - 2) {
                    // Create faces
                    p00 = rowIndex * numDivX + colIndex;
                    p01 = p00 + 1;
                    p10 = p00 + numDivX;
                    p11 = p10 + 1;
                    tc00 = rowIndex * numDivX + colIndex;
                    tc01 = tc00 + 1;
                    tc10 = tc00 + numDivX;
                    tc11 = tc10 + 1;

                    index = (rowIndex * columnWidth * faceSize + (colIndex * faceSize)) * faceGroupSize;
                    //outer
                    faces[index + 0] = p10;
                    faces[index + 1] = tc00;
                    faces[index + 2] = p00;

                    faces[index + 3] = tc11;
                    faces[index + 4] = p11;
                    faces[index + 5] = tc10;

                    index += faceSize;
                    faces[index + 0] = p01;
                    faces[index + 1] = tc11;
                    faces[index + 2] = p11;

                    faces[index + 3] = tc00;
                    faces[index + 4] = p00;
                    faces[index + 5] = tc01;
                }
            }
            //@TODO SMP close the outer shell by tessellating back to the start of the row
            currentY += rowYSpacing;
        }
        Utils.printTotalTime(startTime);
        LOG.info("dude the warp is done.");
        float maxTex = Double.valueOf(
                IntStream.range(0, texCoords.length)
                    .mapToDouble(i -> texCoords[i])
                    .max().getAsDouble())
            .floatValue();

        int maxFace = Arrays.stream(faces).max().getAsInt();
        LOG.info("pointFloats size: {} maxTex: {} Total Faces: {} : maxFace: {}", pointFloats.length, maxTex, faces.length, maxFace);
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(pointFloats);
        mesh.getTexCoords().addAll(texCoords);
        mesh.getFaces().addAll(faces);
        return mesh;
    }
}
