package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

import javafx.geometry.Point3D;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class CollisionSweeper {
    private ArrayList<HitBox> hitBoxes;
    private ArrayList<HitShape3D> hitShapes;

    public CollisionSweeper() {
        hitBoxes = new ArrayList<>();
        hitShapes = new ArrayList<>();
    }

    public HitBox checkCollision(Point3D point3D) {
        return hitBoxes.stream()
            .filter(t -> t.insideBox(point3D))
            .findFirst().orElse(null); //null if no intersections
    }

    public HitBox rayCheckFirst(Point3D point3D, Point3D velocity) {
        return hitBoxes.stream()
            .filter(t -> t.rayChecker(point3D, velocity))
            .findFirst().orElse(null); //null if no intersections
    }

    public List<HitBox> rayCheckAll(Point3D point3D, Point3D velocity) {
        return hitBoxes.stream()
            .filter(t -> t.rayChecker(point3D, velocity))
            .toList();
    }

    public HitBox checkRayIntersect(Point3D point3D, Point3D velocity) {
        return hitBoxes.stream()
            .filter(t -> t.intersectsPlanes(point3D, velocity))
            .findFirst().orElse(null); //null if no intersections
    }

    public ArrayList<HitBox> getHitBoxes() {
        return hitBoxes;
    }

    public HitShape3D checkShapeCollision(Point3D point3D) {
        return hitShapes.stream()
            .filter(t -> t.insideBox(point3D))
            .findFirst().orElse(null); //null if no intersections
    }

    public HitShape3D rayShapeCheckFirst(Point3D point3D, Point3D velocity) {
        return hitShapes.stream()
            .filter(t -> t.rayChecker(point3D, velocity))
            .findFirst().orElse(null); //null if no intersections
    }

    public List<HitShape3D> rayShapeCheckAll(Point3D point3D, Point3D velocity) {
        return hitShapes.stream()
            .filter(t -> t.rayChecker(point3D, velocity))
            .toList();
    }

    public HitShape3D checkRayShapeIntersect(Point3D point3D, Point3D velocity) {
        return hitShapes.stream()
            .filter(t -> t.intersectsPlanes(point3D, velocity))
            .findFirst().orElse(null); //null if no intersections
    }

    public ArrayList<HitShape3D> getHitShapes() {
        return hitShapes;
    }

}
