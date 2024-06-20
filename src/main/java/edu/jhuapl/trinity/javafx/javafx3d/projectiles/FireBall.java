package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

import edu.jhuapl.trinity.javafx.javafx3d.TriaxialSpheroidMesh;
import edu.jhuapl.trinity.utils.ResourceUtils;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;

/**
 *
 * @author Sean Phillips
 */
public class FireBall extends Projectile {
    public static double DEFAULT_FIREBALL_RADIUS = 15;
    TriaxialSpheroidMesh ellipsoid;
    double distanceToLive = 2000;
    
    public FireBall(double radius, Point3D start, Point3D velocity) {
        this.start = start;
        location = start;
        this.velocity = velocity;
        ellipsoid = new TriaxialSpheroidMesh(64, radius, radius, radius);
        //Try to load default texture set
        Image diffuse = null, specular = null, bump = null, self = null;
        try {
            diffuse = ResourceUtils.load3DTextureImage("fireballdiffuse");
//            specular = ResourceUtils.load3DTextureImage("explosion");
            bump = ResourceUtils.load3DTextureImage("asteroidBumpNormalMap");
//            self = ResourceUtils.load3DTextureImage("explosion");
        } catch (IOException ex) {
            Logger.getLogger(FireBall.class.getName()).log(Level.SEVERE, null, ex);
        }
        //diffuse color, diffuseMap, specularMap, bumpMap, selfIlluminationMap
        PhongMaterial material = new PhongMaterial(
        Color.FIREBRICK, 
            diffuse, 
            specular, 
            bump, 
            self
        );
        ellipsoid.setMaterial(material);
    }
    
    @Override
    public void reset() {
        location = start;
        activeProperty.set(true);
    }

    @Override
    public Shape3D getShape3D() {
        return ellipsoid;
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
