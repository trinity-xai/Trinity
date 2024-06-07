package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2024 Sean Phillips
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

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point3D;

/**
 *
 * @author Sean Phillips
 */
public class CollisionSweeper {
    private ArrayList<HitBox> hitBoxes;
    private ArrayList<HitShape3D> hitShapes;

    public CollisionSweeper(){
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
