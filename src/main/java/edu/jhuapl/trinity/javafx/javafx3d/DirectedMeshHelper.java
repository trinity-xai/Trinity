package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.primitives.helper.MeshHelper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sean Phillips
 * Adapted from Jose Pereda's awesome MeshHelper
 */
public class DirectedMeshHelper extends MeshHelper {
    public DirectedMeshHelper(TriangleMesh tm) {
        super(tm);
    }

    /*
    Add to the meshHelper a new meshHelper, and given a list of new positions,
    the data of a list of meshes on these locations will be created, to store the data of
    these meshes that can be joined into one
    */
    public void addMesh(MeshHelper mh, List<Point3D> translate, List<Point3D> endPoints) {
        float[] newPoints = new float[getPoints().length + mh.getPoints().length * translate.size()];
        float[] newF = new float[getF().length + mh.getF().length * translate.size()];
        float[] newTexCoords = new float[getTexCoords().length + mh.getTexCoords().length * translate.size()];
        int[] newFaces = new int[getFaces().length + mh.getFaces().length * translate.size()];
        int[] newFaceSmoothingGroups = new int[getFaceSmoothingGroups().length + mh.getFaceSmoothingGroups().length * translate.size()];
        System.arraycopy(getPoints(), 0, newPoints, 0, getPoints().length);
        System.arraycopy(getF(), 0, newF, 0, getF().length);
        System.arraycopy(getTexCoords(), 0, newTexCoords, 0, getTexCoords().length);
        System.arraycopy(getFaces(), 0, newFaces, 0, getFaces().length);
        System.arraycopy(getFaceSmoothingGroups(), 0, newFaceSmoothingGroups, 0, getFaceSmoothingGroups().length);
        int numPoints = mh.getPoints().length;
        int numF = mh.getF().length;
        int numTexCoords = mh.getTexCoords().length;
        int numFaces = mh.getFaces().length;
        int numFaceSmoothingGroups = mh.getFaceSmoothingGroups().length;
        AtomicInteger count = new AtomicInteger();
        //optimize for array traversal and eliminate object creation

//        List<float[]> collect = translate.parallelStream().map(p3d->transform(mh.getPoints(),p3d)).collect(Collectors.toList());
        //@TODO SMP to make parallel we have to combine translate and endpoint
        //into a single collection that can be streamed
//        translate.parallelStream().forEach(point3D -> {
//
//        });

        Point3D[] translateArray = translate.toArray(Point3D[]::new);
        Point3D[] endArray = endPoints.toArray(Point3D[]::new);
//        for(int i=0; i<translate.size(); i++) {
//            Point3D p3d = translate.get(i);
//            Point3D endPoint = endPoints.get(i);
        float[] transformedArray;
        float[] ff;
        //currently 2 ms for 20k points
        for (int i = 0; i < translateArray.length; i++) {
            transformedArray = transform(mh.getPoints(), translateArray[i]);
            transformedArray[0] += endArray[i].x;
            transformedArray[1] += endArray[i].y;
            transformedArray[2] += endArray[i].z;
            System.arraycopy(transformedArray, 0, newPoints, getPoints().length + numPoints * count.get(), mh.getPoints().length);
            ff = mh.getF();
            Arrays.fill(ff, translateArray[i].f);
            System.arraycopy(ff, 0, newF, getF().length + numF * count.get(), ff.length);
            System.arraycopy(mh.getTexCoords(), 0, newTexCoords, getTexCoords().length + numTexCoords * count.get(), mh.getTexCoords().length);
            System.arraycopy(translateFaces(mh.getFaces(), numPoints / 3 * (count.get() + 1), numTexCoords / 2 * (count.get() + 1)), 0, newFaces, getFaces().length + numFaces * count.get(), mh.getFaces().length);
            System.arraycopy(mh.getFaceSmoothingGroups(), 0, newFaceSmoothingGroups, getFaceSmoothingGroups().length + numFaceSmoothingGroups * count.getAndIncrement(), mh.getFaceSmoothingGroups().length);
        }
        setPoints(newPoints);
        setF(newF);
        setTexCoords(newTexCoords);
        setFaces(newFaces);
        setFaceSmoothingGroups(newFaceSmoothingGroups);
    }


    private int[] translateFaces(int[] faces, int points, int texCoords) {
        int[] newFaces = new int[faces.length];
        for (int i = 0; i < faces.length; i++) {
            newFaces[i] = faces[i] + (i % 2 == 0 ? points : texCoords);
        }
        return newFaces;
    }

    private float[] transform(float[] points, Point3D p3d) {
        float[] newPoints = new float[points.length];
        for (int i = 0; i < points.length / 3; i++) {
            newPoints[3 * i] = points[3 * i] + p3d.x;
            newPoints[3 * i + 1] = points[3 * i + 1] + p3d.y;
            newPoints[3 * i + 2] = points[3 * i + 2] + p3d.z;
        }
        return newPoints;
    }

    public class StartAndEnd {
        public Point3D start;
        public Point3D end;

        public StartAndEnd(Point3D start, Point3D end) {
            this.start = start;
            this.end = end;
        }
    }
}
