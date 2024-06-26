package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

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

import edu.jhuapl.trinity.javafx.javafx3d.animated.Tracer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class TracerRound extends Projectile {
    public static float DEFAULT_TRACER_WIDTH = 10;
    public static double DEFAULT_TRACER_LENGTH = 200;
    Tracer tracer;
    double distanceToLive = 2000;

    public TracerRound(float width, Point3D start, Point3D end, Point3D velocity) {
        this.start = start;
        location = start;
        this.velocity = velocity;
        tracer = new Tracer(JavaFX3DUtils.toFXYZ3D.apply(start),
            JavaFX3DUtils.toFXYZ3D.apply(end), width, Color.TOMATO);
        //Try to load default texture set
        Image diffuse = null, specular = null, bump = null, self = null;
        try {
//            diffuse = ResourceUtils.load3DTextureImage("blue-horizontal-laser");
            diffuse = ResourceUtils.load3DTextureImage("neoncyanpyramid-transparent");
//            specular = ResourceUtils.load3DTextureImage("explosion");
//            bump = ResourceUtils.load3DTextureImage("asteroidBumpNormalMap");
//            self = ResourceUtils.load3DTextureImage("explosion");
        } catch (IOException ex) {
            Logger.getLogger(FireBall.class.getName()).log(Level.SEVERE, null, ex);
        }
        //diffuse color, diffuseMap, specularMap, bumpMap, selfIlluminationMap
        PhongMaterial material = new PhongMaterial(
            Color.WHITE,
            diffuse,
            null,
            null,
            null
        );
        tracer.material = material;
        tracer.meshView.setMaterial(tracer.material);
    }

    @Override
    public void reset() {
        location = start;
        activeProperty.set(true);
    }

    @Override
    public Shape3D getShape3D() {
        return tracer.meshView;
    }

    @Override
    public boolean update(double _time) {
        location = location.add(velocity);
        getShape3D().setTranslateX(location.getX());
        getShape3D().setTranslateY(location.getY());
        getShape3D().setTranslateZ(location.getZ());
        //mark to be culled if it has travelled this far without hitting anything
        return Math.abs(location.distance(javafx.geometry.Point3D.ZERO)) < distanceToLive;
    }
}
