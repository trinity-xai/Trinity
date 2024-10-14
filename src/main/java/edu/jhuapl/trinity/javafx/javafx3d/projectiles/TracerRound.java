/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

import edu.jhuapl.trinity.javafx.javafx3d.animated.Tracer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Sean Phillips
 */
public class TracerRound extends Projectile {
    private static final Logger LOG = LoggerFactory.getLogger(TracerRound.class);
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
            LOG.error(null, ex);
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
        return Math.abs(location.distance(Point3D.ZERO)) < distanceToLive;
    }
}
