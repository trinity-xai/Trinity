package edu.jhuapl.trinity.javafx.javafx3d;

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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.DepthTest;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import java.util.Objects;

/**
 * Orginal License/Comment block from FXyz3D
 * SpheroidMesh.java
 * <p>
 * Copyright (c) 2013-2021, F(X)yz
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of F(X)yz, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL F(X)yz BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


/**
 * @author Dub (FXyz's SpheroidMesh.  We miss you Jason)
 * @author Sean Phillips
 */
//public class SpheroidMesh extends TexturedMesh {
public class TriaxialSpheroidMesh extends TexturedMesh {

    private static final double DEFAULT_MAJOR_RADIUS = 50.0;
    private static final double DEFAULT_MINOR_RADIUS = 12.0;
    private static final double DEFAULT_GAMMA_RADIUS = 12.0;

    private static final int DEFAULT_DIVISIONS = 64;

    public TriaxialSpheroidMesh() {
        this(DEFAULT_MAJOR_RADIUS, DEFAULT_MINOR_RADIUS, DEFAULT_GAMMA_RADIUS);
    }

    /**
     * @param radius Creates a Sphere with the specified Radius
     */
    public TriaxialSpheroidMesh(double radius) {
        this(radius, radius, radius);
    }

    /**
     * @param majRad The X (horizontal) radius
     * @param minRad The Y(vertical) radius
     * @param gamRad The Z(depth) radius
     */
    public TriaxialSpheroidMesh(double majRad, double minRad, double gamRad) {
        this(DEFAULT_DIVISIONS, majRad, minRad, gamRad);
    }

    /**
     * @param divs   Divisions for the Spheroid. Default is 64
     * @param majRad The major(horizontal) radius
     * @param minRad The minor(vertical) radius
     * @param gamRad the gamma (depth) radius
     */
    public TriaxialSpheroidMesh(int divs, double majRad, double minRad, double gamRad) {
        setDivisions(divs);
        setMajorRadius(majRad);
        setMinorRadius(minRad);
        setGammaRadius(gamRad);
        setCullFace(CullFace.BACK);
        setDepthTest(DepthTest.ENABLE);

        updateMesh();
    }

    /**
     * @return true if both major and minor radii are equal
     */
    public boolean isSphere() {
        return Objects.equals(getMajorRadius(), getMinorRadius());
    }

    /**
     * @return true if major radius is greater than minor radius
     */
    public boolean isOblateSpheroid() {
        return getMajorRadius() > getMinorRadius();
    }

    /**
     * @return true if major radius is less than minor radius
     */
    public boolean isProlateSpheroid() {
        return getMajorRadius() < getMinorRadius();
    }

    private final DoubleProperty majorRadius = new SimpleDoubleProperty(this, "majorRadius", DEFAULT_MAJOR_RADIUS) {
        @Override
        protected void invalidated() {
            updateMesh();
        }
    };

    public final Double getMajorRadius() {
        return majorRadius.get();
    }

    public final void setMajorRadius(Double value) {
        majorRadius.set(value);
    }

    public DoubleProperty majorRadiusProperty() {
        return majorRadius;
    }

    private final DoubleProperty minorRadius = new SimpleDoubleProperty(this, "minorRadius", DEFAULT_MINOR_RADIUS) {
        @Override
        protected void invalidated() {
            updateMesh();
        }
    };

    public final Double getMinorRadius() {
        return minorRadius.get();
    }

    public final void setMinorRadius(double value) {
        minorRadius.set(value);
    }

    public DoubleProperty minorRadiusProperty() {
        return minorRadius;
    }

    private final DoubleProperty gammaRadius = new SimpleDoubleProperty(this, "gammaRadius", DEFAULT_GAMMA_RADIUS) {
        @Override
        protected void invalidated() {
            updateMesh();
        }
    };

    public final Double getGammaRadius() {
        return gammaRadius.get();
    }

    public final void setGammaRadius(Double value) {
        gammaRadius.set(value);
    }

    public DoubleProperty majorGammaProperty() {
        return gammaRadius;
    }

    private final IntegerProperty divisions = new SimpleIntegerProperty(this, "divisions", DEFAULT_DIVISIONS) {
        @Override
        protected void invalidated() {
            updateMesh();
        }
    };

    public final int getDivisions() {
        return divisions.get();
    }

    public final void setDivisions(int value) {
        divisions.set(value);
    }

    public IntegerProperty divisionsProperty() {
        return divisions;
    }

    @Override
    protected void updateMesh() {
        setMesh(createSpheroidMesh(getDivisions(), getMajorRadius(), getMinorRadius(), getGammaRadius()));
    }

    private TriangleMesh createSpheroidMesh(int divs, double major, double minor, double gamma) {
        divs = correctDivisions(divs);
        TriangleMesh m = new TriangleMesh();

        final int divsHalf = divs / 2;

        final int numPoints = divs * (divsHalf - 1) + 2;
        final int numTexCoords = (divs + 1) * (divsHalf - 1) + divs * 2;
        final int numFaces = divs * (divsHalf - 2) * 2 + divs * 2;

        final float divf = 1.f / divs;

        float[] points = new float[numPoints * m.getPointElementSize()];
        float[] tPoints = new float[numTexCoords * m.getTexCoordElementSize()];
        int[] faces = new int[numFaces * m.getFaceElementSize()];

        int pPos = 0;
        int tPos = 0;

        for (int lat = 0; lat < divsHalf - 1; ++lat) {
            float latRad = divf * (lat + 1 - divsHalf / 2) * 2 * (float) Math.PI;
            float sin_v = (float) Math.sin(latRad);
            float cos_v = (float) Math.cos(latRad);
            float ty = 0.5f + sin_v * 0.5f;
            for (int lon = 0; lon < divs; ++lon) {
                double lonRad = divf * lon * 2 * (float) Math.PI;
                float sin_u = (float) Math.sin(lonRad);
                float cos_u = (float) Math.cos(lonRad);
                //update vertices
                points[pPos + 0] = (float) (cos_v * cos_u * major); // x
                points[pPos + 1] = (float) (sin_v * minor);         // y up
                points[pPos + 2] = (float) (cos_v * sin_u * gamma); // z
                //update texture points
                tPoints[tPos + 0] = 1 - divf * lon;
                tPoints[tPos + 1] = ty;
                //move forward counters
                pPos += 3;
                tPos += 2;
            }

            tPoints[tPos + 0] = 0;
            tPoints[tPos + 1] = ty;
            tPos += 2;
        }

        points[pPos + 0] = 0;
        points[pPos + 1] = (float) -minor;
        points[pPos + 2] = 0;
        points[pPos + 3] = 0;
        points[pPos + 4] = (float) minor;
        points[pPos + 5] = 0;
        pPos += 6;

        int pS = (divsHalf - 1) * divs;

        float textureDelta = 1.f / 256;
        for (int i = 0; i < divs; ++i) {
            tPoints[tPos + 0] = divf * (0.5f + i);
            tPoints[tPos + 1] = textureDelta;
            tPos += 2;
        }

        for (int i = 0; i < divs; ++i) {
            tPoints[tPos + 0] = divf * (0.5f + i);
            tPoints[tPos + 1] = 1 - textureDelta;
            tPos += 2;
        }

        int fIndex = 0;
        for (int y = 0; y < divsHalf - 2; ++y) {
            for (int x = 0; x < divs; ++x) {
                int p0 = y * divs + x;
                int p1 = p0 + 1;
                int p2 = p0 + divs;
                int p3 = p1 + divs;

                int t0 = p0 + y;
                int t1 = t0 + 1;
                int t2 = t0 + (divs + 1);
                int t3 = t1 + (divs + 1);

                // add p0, p1, p2
                faces[fIndex + 0] = p0;
                faces[fIndex + 1] = t0;
                faces[fIndex + 2] = p1 % divs == 0 ? p1 - divs : p1;
                faces[fIndex + 3] = t1;
                faces[fIndex + 4] = p2;
                faces[fIndex + 5] = t2;
                fIndex += 6;

                // add p3, p2, p1
                faces[fIndex + 0] = p3 % divs == 0 ? p3 - divs : p3;
                faces[fIndex + 1] = t3;
                faces[fIndex + 2] = p2;
                faces[fIndex + 3] = t2;
                faces[fIndex + 4] = p1 % divs == 0 ? p1 - divs : p1;
                faces[fIndex + 5] = t1;
                fIndex += 6;
            }
        }

        int p0 = pS;
        int tB = (divsHalf - 1) * (divs + 1);
        for (int x = 0; x < divs; ++x) {
            int p2 = x;
            int p1 = x + 1;
            int t0 = tB + x;

            faces[fIndex + 0] = p0;
            faces[fIndex + 1] = t0;
            faces[fIndex + 2] = p1 == divs ? 0 : p1;
            faces[fIndex + 3] = p1;
            faces[fIndex + 4] = p2;
            faces[fIndex + 5] = p2;
            fIndex += 6;
        }

        p0 = p0 + 1;
        tB = tB + divs;
        int pB = (divsHalf - 2) * divs;

        for (int x = 0; x < divs; ++x) {
            int p1 = pB + x;
            int p2 = pB + x + 1;

            int t0 = tB + x;
            int t1 = (divsHalf - 2) * (divs + 1) + x;
            int t2 = t1 + 1;

            faces[fIndex + 0] = p0;
            faces[fIndex + 1] = t0;
            faces[fIndex + 2] = p1;
            faces[fIndex + 3] = t1;
            faces[fIndex + 4] = p2 % divs == 0 ? p2 - divs : p2;
            faces[fIndex + 5] = t2;
            fIndex += 6;
        }

        m.getPoints().addAll(points);
        m.getTexCoords().addAll(tPoints);
        m.getFaces().addAll(faces);

        return m;
    }

    private int correctDivisions(int div) {
        return ((div + 3) / 4) * 4;
    }
}
