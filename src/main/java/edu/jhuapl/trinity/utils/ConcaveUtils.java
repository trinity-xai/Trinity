package edu.jhuapl.trinity.utils;

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

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.utils.mc.MarchingCubesMeshFactory;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.geometry.Vector3D;
import org.fxyz3d.utils.geom.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;

/**
 *
 */
public class ConcaveUtils {
    static int NO_OF_DIM = 3;                              // dimension of data
    static double N = 0;                                  // threshold for digging
    double[][] orgData;
    double[] classCenter;
    ArrayList<int[]> cvxList = new ArrayList<>();             //convex list
    ArrayList<double[]> nvList = new ArrayList<>();        //normal vector list

    /*///////////////////////////////////////////////////////////////////////////////////////////
	Calculate Center point of given class(OH)
	///////////////////////////////////////////////////////////////////////////////////////////*/
    public void calcClassCenter() {
        classCenter = new double[NO_OF_DIM];
        double[] tmpRecord = new double[NO_OF_DIM];

        for (int i = 0; i < orgData.length; i++) {
            for (int j = 0; j < NO_OF_DIM; j++) {
                tmpRecord[j] += orgData[i][j];
            }
        }

        for (int i = 0; i < NO_OF_DIM; i++) {
            classCenter[i] = tmpRecord[i] / orgData.length;
        }
    }


    /*///////////////////////////////////////////////////////////////////////////////////////////
	Calculate angle between two vectors (OH)
	///////////////////////////////////////////////////////////////////////////////////////////*/
    public double calcAngle(double[] vectorA, double[] vectorB) {
//        double angle = 0;
        double normA = 0, normB = 0, innerProduct = 0;
        for (int i = 0; i < NO_OF_DIM; i++) {
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
            innerProduct += vectorA[i] * vectorB[i];
        }
        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);
//        angle = innerProduct / (normA * normB);
        return innerProduct / (normA * normB);
    }

    /*///////////////////////////////////////////////////////////////////////////////////////////
	Calculate Euclidean distance between point A and B(OH)
	///////////////////////////////////////////////////////////////////////////////////////////*/
    public double calcEDistance(double[] pointA, double[] pointB) {
        double tmpDistance = 0;
        for (int i = 0; i < NO_OF_DIM; i++) {
            tmpDistance += Math.pow(pointA[i] - pointB[i], 2);
        }
//        dist = Math.sqrt(tmpDistance);
//        return dist;
        return Math.sqrt(tmpDistance);
    }

    /*///////////////////////////////////////////////////////////////////////////////////////////
	Calculate distance between point A and component B(OH)
    //////////////////////////////////////////////////////////////////////////////////////////*/
    public double calcPCDistance(double[] pointA, int idxComponentB) {
        double dist = -1;

        int getRecord[] = (int[]) cvxList.get(idxComponentB);

        switch (NO_OF_DIM) {
            case 2:  ////////////////////////////////////////////////////////////////////// 2-Dimensional
                double x0 = pointA[0], y0 = pointA[1];                                     // pa
                double x1 = orgData[getRecord[0] - 1][0];
                double y1 = orgData[getRecord[0] - 1][1];  // sa
                double x2 = orgData[getRecord[1] - 1][0];
                double y2 = orgData[getRecord[1] - 1][1];  // sb

                double a, b, c, t;
                a = Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2);          // SQDS(sa,pa);
                b = Math.pow(x2 - x0, 2) + Math.pow(y2 - y0, 2);          // SQDS(sb,pa);
                c = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);          // SQDS(sa,sb);

                if (a < b) {
                    double tmp = a;
                    a = b;
                    b = tmp;
                }
                if (a > b + c || c < 0.000000001) {
                    dist = Math.sqrt(b);
                } else {
                    //t=sa.x*sb.y-sb.x*sa.y+sb.x*pa.y-pa.x*sb.y+pa.x*sa.y-sa.x*pa.y;
                    t = x1 * y2 - x2 * y1 + x2 * y0 - x0 * y2 + x0 * y1 - x1 * y0;
                    dist = (double) Math.abs(t) / (double) Math.sqrt(c);
                }
                return dist;
            //break;

            // case 3:   ////////////////////////////////////////////////////////////////////// 3-Dimensional
            // Implement later
            // break;
            default: ////////////////////////////////////////////////////////////////////// n-Dimensional (n>3)

                // find nearest edge point ------------------------------------------------
                int idxNearEdgePoint = -1;
                double minDistance = 999999;

                for (int m = 0; m < NO_OF_DIM; m++) {
                    double tmpDistance = calcEDistance(pointA, orgData[getRecord[m] - 1]);
                    if (tmpDistance < minDistance) {
                        minDistance = tmpDistance;
                        idxNearEdgePoint = getRecord[m];
                    }
                }
                double[] nearEdgePoint = orgData[idxNearEdgePoint - 1];

                // find opposite mid point of nearEdgePoint ------------------------------------------
                double[] oppositeMidPoint = new double[NO_OF_DIM];

                for (int n = 0; n < NO_OF_DIM; n++) {
                    if (getRecord[n] == idxNearEdgePoint) {
                        continue;
                    }
                    for (int m = 0; m < NO_OF_DIM; m++) {
                        oppositeMidPoint[m] += orgData[getRecord[n] - 1][m];
                    }
                }

                for (int m = 0; m < NO_OF_DIM; m++) {
                    oppositeMidPoint[m] /= (NO_OF_DIM - 1);
                }

                // find inside point of Component ------------------------------------------
                double edgeLength = calcEDistance(pointA, oppositeMidPoint);
                double[] minusFactor = new double[NO_OF_DIM];

                for (int m = 0; m < NO_OF_DIM; m++) {
                    minusFactor[m] = ((nearEdgePoint[m] - oppositeMidPoint[m]) / edgeLength) * 0.1;
                }

                double[] insidePoint = new double[NO_OF_DIM];
                for (int m = 0; m < NO_OF_DIM; m++) {
                    insidePoint[m] = nearEdgePoint[m] - minusFactor[m];
                }
                // claculate distance ------------------------------------------
                dist = calcEDistance(pointA, insidePoint);
        }
        return dist;
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Turn on the flag of vertex via convex list.
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public void flagVertexPoints(Point3d[] points) {

        //map the original point data to the memory structure
        for (int pointIndex = 0; pointIndex < points.length; pointIndex++) {
            orgData[pointIndex][0] = points[pointIndex].x;
            orgData[pointIndex][1] = points[pointIndex].y;
            orgData[pointIndex][2] = points[pointIndex].z;
        }
        //for every point which is a convex hull face mark that with a 1 (true)
        int getRecord[];
        for (int i = 0; i < cvxList.size(); i++) {
            getRecord = cvxList.get(i);
            for (int j = 0; j < getRecord.length; j++) {
                orgData[getRecord[j] - 1][NO_OF_DIM] = 1;    // set flag field to 1
            }
        }
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Concave Hull Algorithm
	Dig the boundary as a result of comparing between nearest inner point and decision distance.
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public void findConcave() {
        System.out.println("Finding Concave Hull for face count: " + cvxList.size());
        int count_array_change = 0;
        for (int i = 0; i < cvxList.size(); i++) {
            if (i % 10 == 0)
                System.out.println("cvxList Index: " + i + " out of " + cvxList.size());
            // Step. 0 Calculate average of edges of each component
            double avgEdgeLength = 0;
            int cCnt = 0;
            int getRecord[] = (int[]) cvxList.get(i);

            int[] indices;
            CombinationGenerator cg = new CombinationGenerator(getRecord.length, 2);
            while (cg.hasMore()) {
                indices = cg.getNext();
                int x = getRecord[indices[0]];
                int y = getRecord[indices[1]];
                double tmpLength = calcEDistance(orgData[x - 1], orgData[y - 1]);
                avgEdgeLength += tmpLength;
                cCnt++;
            }

            avgEdgeLength /= (double) cCnt;
            // End of Step. 0

            // Step. 1 Find nearest inner point of each component
            ArrayList<Double> arrLength = new ArrayList<>();     // Distances of inner points
            ArrayList<Integer> arrIdx = new ArrayList<>();   // Indexes of inner points

            // Calculate average length between every points and edges of each component
            for (int j = 0; j < orgData.length; j++) {
                //if point is already included in vertex list, skip the point.
                if (orgData[j][NO_OF_DIM] == 1.0) {
                    continue;
                }
                double tmpAverage = 0;
                for (int x = 0; x < NO_OF_DIM; x++) {
                    double tmpLength = calcEDistance(orgData[j], orgData[getRecord[x] - 1]);
                    tmpAverage += tmpLength;
                }
                tmpAverage /= (double) NO_OF_DIM;

                arrLength.add(tmpAverage);
                arrIdx.add(j);
            }

            if (arrLength.isEmpty()) {
                break;
            }

            // Find nearest inner point
            double shortestLength = 999999;
            int nearestPoint = 0;
            for (int k = 0; k < arrLength.size(); k++) {
                double currLength = (double) arrLength.get(k);
                if (currLength < shortestLength) {
                    // check if current point is close to neighbor compoinnt OH 20110111
                    {   ////////////////////////////////////////////////////////////////////////////

                        int idxNearEdgePoint = -1;
                        double minDistance = 999999;

                        for (int m = 0; m < NO_OF_DIM; m++) {
                            double tmpDistance = calcEDistance(orgData[(int) arrIdx.get(k)], orgData[getRecord[m] - 1]);
                            if (tmpDistance < minDistance) {
                                minDistance = tmpDistance;
                                idxNearEdgePoint = getRecord[m];
                            }
                        }

                        // point is nearest to given component
                        // collect components that link to given component
                        ArrayList<String> linkComponent = new ArrayList<>();

                        for (int n = 0; n < cvxList.size(); n++) {
                            if (cvxList.get(n) == null || n == i) {
                                continue;
                            }

                            int[] cvxRecord = cvxList.get(n);

                            for (int m = 0; m < NO_OF_DIM; m++) {
                                if (cvxRecord[m] == (idxNearEdgePoint)) {
                                    linkComponent.add(String.valueOf(n));
                                    break;
                                }
                            }
                        }

                        int closerToNeighbor = 0;
                        for (int r = 0; r < linkComponent.size(); r++) {
                            int idxLinkComponent = Integer.parseInt(linkComponent.get(r));
                            if (calcPCDistance(orgData[(int) arrIdx.get(k)], idxLinkComponent) < calcPCDistance(orgData[(int) arrIdx.get(k)], i)) {
                                closerToNeighbor = 1;
                            }
                        }

                        if (closerToNeighbor == 0) {
                            nearestPoint = (int) arrIdx.get(k);
                            shortestLength = currLength;
                        }
                    } ////////////////////////////////////////////////////////////////////////////

                }

            }

            // step. 2 Calculate shortest length between each point of component and nearest inner point.
            // This distance is called decision distance.
            double minLength = 99999;
            int closestPoint = 99999;
            for (int x = 0; x < NO_OF_DIM; x++) {
                double tmpLength = 0;
                for (int k = 0; k < NO_OF_DIM; k++) {
                    tmpLength += Math.pow(orgData[nearestPoint][k] - orgData[getRecord[x] - 1][k], 2);
                }
                tmpLength = Math.sqrt(tmpLength);
                if (tmpLength < minLength) {
                    minLength = tmpLength;
                    closestPoint = getRecord[x];
                }
            }

            // step. 3 Compare the ratio of decision distance with threshold N.
            // if the ratio bigger than N, nearest inner point is inserted to concave list.
            double diggRatio = (double) avgEdgeLength / (double) minLength;
            if (diggRatio < 0)
                System.out.println("Negative diggRatio!");
            if (minLength > 0 && diggRatio > N) {
                // 3.1 vertex flag on.
                orgData[nearestPoint][NO_OF_DIM] = 1;

                // 3.2 make new components through combination of a selected component and nearest inner point.
                cg = new CombinationGenerator(NO_OF_DIM, NO_OF_DIM - 1);
                while (cg.hasMore()) {
                    indices = cg.getNext();
                    int newRecord[] = new int[NO_OF_DIM];

                    for (int q = 0; q < NO_OF_DIM - 1; q++) {
                        newRecord[q] = getRecord[indices[q]];
                    }
                    newRecord[NO_OF_DIM - 1] = nearestPoint + 1;   //
                    //System.out.println("newRecord = " + newRecord[0] + "," + newRecord[1]);
                    cvxList.add(newRecord);    //add new components
                }
                cvxList.set(i, null); // nullify for removal later
                count_array_change++;
            }
        }

        // Delete Nullified component
        for (int k = cvxList.size() - 1; k >= 0; k--) {
            if (cvxList.get(k) == null) {
                cvxList.remove(k);
                //System.out.println("removed :" + k);
            }
        }
        System.out.println("Completed Concave Hull for face count: " + cvxList.size());
    }

    //Sort nodes with positions in 3d space.
    //Assuming the points form a convex shape.
    //Assuming points are on a single plain (or close to it).
    //    public List<Node> sortVerticies( Vector3 normal, List<Node> nodes ) {
    public List<int[]> sortVertices(Vec3d normal, double[] centroid, ArrayList<int[]> faceIndices, double[][] orgData) {
        //https://gamedev.stackexchange.com/questions/159379/getting-the-winding-order-of-a-mesh
        //Vector3 first = nodes[0].pos;
        Vector3D centroidVector = new Vector3D(centroid);

        //Sort by distance from random point to get 2 adjacent points.
        //List<Node> temp = nodes.OrderBy(n => Vector3.Distance(n.pos, first ) ).ToList();
        List<int[]> sorted = faceIndices.stream().sorted((int[] o1, int[] o2) -> {
            //TODO SMP do lookup for position values
            Vector3D first = new Vector3D(0, 0, 0);
            Vector3D second = new Vector3D(0, 0, 0);
            double d1 = first.distance(centroidVector);
            double d2 = second.distance(centroidVector);
            if (d1 < d2) return 1;
            else if (d1 > d2) return -1;
            return 0;
        }).toList();

//        //Create a vector from the 2 adjacent points,
//        //this will be used to sort all points, except the first, by the angle to this vector.
//        //Since the shape is convex, angle will not exceed 180 degrees, resulting in a proper sort.
//        Vector3 refrenceVec = (temp[1].pos - first);
//
//        //Sort by angle to reference, but we are still missing the first one.
//        List<Node> results = temp.Skip(1).OrderBy(n => Vector3.Angle(refrenceVec,n.pos - first)).ToList();
//
//        //insert the first one, at index 0.
//        results.Insert(0,nodes[0]);
//
//        //Now that it is sorted, we check if we got the direction right, if we didn't we reverse the list.
//        //We compare the given normal and the cross product of the first 3 point.
//        //If the magnitude of the sum of the normal and cross product is less than Sqrt(2) then then there is more than 90 between them.
//        if ( (Vector3.Cross( results[1].pos-results[0].pos, results[2].pos - results[0].pos ).normalized + normal.normalized).magnitude < 1.414f ) {
//            results.Reverse();
//        }

        return sorted;
    }

    public static ArrayList<Sphere> makeConcave(Manifold3D manifold3D) {
        double scale = 1.0;
        manifold3D.extrasGroup.getChildren().clear();

        ArrayList<Point3D> subSample = new ArrayList<>();
        Random rando = new Random();
        for (Point3D p3D : manifold3D.getOriginalPoint3DList()) {
            if (rando.nextFloat() <= 0.05)
                subSample.add(p3D);
        }

        ConcaveUtils obj = new ConcaveUtils();
        //Initialize the memory
        int totalPoints = manifold3D.hull.getVertices().length + subSample.size();
        obj.orgData = new double[totalPoints][NO_OF_DIM + 1];    // +1, to add flag field

        obj.cvxList.addAll(Arrays.asList(manifold3D.hull.getFaces(QuickHull3D.INDEXED_FROM_ONE)));
        obj.flagVertexPoints(manifold3D.hull.getVertices()); // flag convex list

        //add subsampled data from total cloud
        int pointIndex = manifold3D.hull.getVertices().length;
        for (Point3D p3D : subSample) {
            obj.orgData[pointIndex][0] = p3D.x;
            obj.orgData[pointIndex][1] = p3D.y;
            obj.orgData[pointIndex][2] = p3D.z;
            obj.orgData[pointIndex][3] = 0;
            pointIndex++;
        }

        obj.calcClassCenter();
        Sphere centerSphere = new Sphere(2.5);
        centerSphere.setMaterial(new PhongMaterial(Color.WHITE));
        centerSphere.setDrawMode(DrawMode.FILL);
        centerSphere.setCullFace(CullFace.NONE);
        centerSphere.setTranslateX(obj.classCenter[0] * scale);
        centerSphere.setTranslateY(obj.classCenter[1] * scale);
        centerSphere.setTranslateZ(obj.classCenter[2] * scale);
        manifold3D.extrasGroup.getChildren().add(centerSphere);

        ConcaveUtils.N = 1.5;
        obj.findConcave();


        ArrayList<Sphere> p = new ArrayList<>();
        if (false && null != manifold3D.getScene()) {

            //Go through orgData and only add points which are marked with a 1

            TriangleMesh tm = new TriangleMesh();
            PhongMaterial pm = new PhongMaterial(Color.RED);
            for (int i = 0; i < obj.cvxList.size(); i++) {
                int[] vertIndices = obj.cvxList.get(i);
                for (int v = 0; v < vertIndices.length; v++) {
                    Sphere sphere = new Sphere(1);
                    sphere.setTranslateX(obj.orgData[vertIndices[v] - 1][0] * scale);
                    sphere.setTranslateY(obj.orgData[vertIndices[v] - 1][1] * scale);
                    sphere.setTranslateZ(obj.orgData[vertIndices[v] - 1][2] * scale);
                    sphere.setMaterial(pm);
                    manifold3D.extrasGroup.getChildren().add(sphere);

                    //TODO SMP Need to sort the faces by order before adding to Triangle Mesh
                    //https://gamedev.stackexchange.com/questions/13229/sorting-array-of-points-in-clockwise-order
                    //https://stackoverflow.com/questions/6880899/sort-a-set-of-3-d-points-in-clockwise-counter-clockwise-order
                    tm.getPoints().addAll(
                        Double.valueOf(obj.orgData[vertIndices[v] - 1][0] * scale).floatValue(),
                        Double.valueOf(obj.orgData[vertIndices[v] - 1][1] * scale).floatValue(),
                        Double.valueOf(obj.orgData[vertIndices[v] - 1][2] * scale).floatValue()
                    );
                    tm.getTexCoords().addAll(
                        Double.valueOf(obj.orgData[vertIndices[v] - 1][0] * scale).floatValue(),
                        Double.valueOf(obj.orgData[vertIndices[v] - 1][2] * scale).floatValue());
                    //TODO SMP These faces might have already been in counter clockwise order!!
                    tm.getFaces().addAll(
//                        vertIndices[0], vertIndices[2], vertIndices[1],
//                        vertIndices[1], vertIndices[2], vertIndices[0]
                        vertIndices[0], vertIndices[1], vertIndices[2],
                        vertIndices[0], vertIndices[2], vertIndices[1]
                    );
                }
            }
            MeshView mv = new MeshView(tm);
            mv.setMaterial(pm);
            mv.setDrawMode(DrawMode.LINE);
            mv.setCullFace(CullFace.NONE);
            manifold3D.extrasGroup.getChildren().add(mv);

        }
        System.out.println("Ok done with Concave stuff...");
/*
        make 3D float array with all zeros.
        then for each point convert x,y,z positions to an index...
        ...rounding up or down as necessary to make them fit
        Then set that position with a 1
  */
        int width = Double.valueOf(Math.ceil(manifold3D.getBoundsWidth())).intValue();
        int height = Double.valueOf(Math.ceil(manifold3D.getBoundsHeight())).intValue();
        int depth = Double.valueOf(Math.ceil(manifold3D.getBoundsDepth())).intValue();

        float[][][] scalarField = new float[width][height][depth];

        double minX = manifold3D.getOriginalPoint3DList().stream()
            .flatMapToDouble(point -> DoubleStream.of(point.x))
            .min().getAsDouble();
        double maxX = minX + width;
        double minY = manifold3D.getOriginalPoint3DList().stream()
            .flatMapToDouble(point -> DoubleStream.of(point.y))
            .min().getAsDouble();
        double maxY = minY + height;
        double minZ = manifold3D.getOriginalPoint3DList().stream()
            .flatMapToDouble(point -> DoubleStream.of(point.z))
            .min().getAsDouble();
        double maxZ = minZ + depth;

//        ;

        for (Point3D p3D : manifold3D.getOriginalPoint3DList()) {
            double normalizedX = DataUtils.normalize(p3D.x, minX, maxX);
            double normalizedY = DataUtils.normalize(p3D.y, minY, maxY);
            double normalizedZ = DataUtils.normalize(p3D.z, minZ, maxZ);
//            int projectedX = Double.valueOf(Math.floor(normalized * width)).intValue();
            int x = Double.valueOf(Math.floor(normalizedX * width)).intValue();
            int y = Double.valueOf(Math.floor(normalizedY * height)).intValue();
            int z = Double.valueOf(Math.floor(normalizedZ * depth)).intValue();
            scalarField[x][y][z] = 1;
        }

        MarchingCubesMeshFactory mcmf = new MarchingCubesMeshFactory(scalarField,
            0.5f, 1);

        TriangleMesh tm = mcmf.createMesh();
        MeshView mv = new MeshView(tm);
        PhongMaterial pm = new PhongMaterial(Color.GREEN);
        mv.setMaterial(pm);
        mv.setDrawMode(DrawMode.FILL);
        mv.setCullFace(CullFace.NONE);
        manifold3D.extrasGroup.getChildren().add(mv);

        ArrayList<Sphere> concavePoints = new ArrayList<>();
//        /////
//        double isoValue = 0.5;
//        float[] voxSize = {1.0f, 1.0f, 1.0f};
//
//        int pointCount = subSample.size();
//        int[] size = {pointCount, pointCount, pointCount};
//        double[] scalarField = new double[size[0] * size[1] * size[2]];
//        for(int i=0;i<pointCount;i++) {
//            Point3D p3D = subSample.get(i);
//            scalarField[i] = (p3D.x * p3D.x + p3D.y * p3D.y - p3D.z * p3D.z - 25);
//        }
//        ArrayList<ArrayList<float []>> results = BenchmarkHandler.makeConcave(
//            scalarField, size, voxSize, isoValue, 1);
//        System.out.println("Total results: " + results.size());
//

//        if(null != manifold3D.getScene()) {
////            manifold3D.extrasGroup.getChildren().clear();
//            double scale = 50.0;
//            for (int i = 0; i < results.size(); i++) {
//                ArrayList<float[]> resSeg = results.get(i);
//                PhongMaterial pm = new PhongMaterial(Color.WHITE);
//                for (int v = 0; v < resSeg.size(); v++) {
//                    Sphere sphere = new Sphere(1);
//                    sphere.setTranslateX(resSeg.get(v)[0] * scale);
//                    sphere.setTranslateY(resSeg.get(v)[1] * scale);
//                    sphere.setTranslateZ(resSeg.get(v)[2] * scale);
//                    sphere.setMaterial(pm);
//                    manifold3D.extrasGroup.getChildren().add(sphere);
//                }
//            }
//        }
        return concavePoints;
    }
}
