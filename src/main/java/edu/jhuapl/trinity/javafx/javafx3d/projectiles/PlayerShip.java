package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

import edu.jhuapl.trinity.javafx.events.HitEvent;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedTetrahedron;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import org.fxyz3d.geometry.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author Sean Phillips
 * Hokey tetrahedra that is artificially stretched to point in the Z+ axis
 */
public class PlayerShip extends AnimatedTetrahedron implements Hittable {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerShip.class);
    public static final double smallDiff = 1.0E-11;
    public static double defaultThrustMagnitude = 1.0;
    public static double maxThrustMagnitude = 20.0;
    public PhongMaterial playerMaterial, cockpitMaterial = null;
    /**
     * Transform Management
     */
    public Affine affineTransform;
    private boolean useLocalTransform = true;
    public boolean useParentRotate = false;
    public Rotate parentRotateX = null;
    public Rotate parentRotateY = null;
    public boolean enableRotationAnimation = true;

    /**
     * physics of the ship.
     */
    private Point3D start = new Point3D(0, 0, 0);
    private Point3D location = new Point3D(0, 0, 0);
    private Point3D velocity = new Point3D(0, 0, 0);
    public double rotateIncrementDegrees = 0.0;
    public Point3D rotateAxis = Rotate.X_AXIS;
    public boolean enableMouseRotation = false;
    /**
     * Flag indicating whether the particle is active.
     */
    public SimpleBooleanProperty activeProperty = new SimpleBooleanProperty(true);

    public PlayerShip(Point3D center) {
        super(100);
        setStart(center);
        try {
            Image specImage = ResourceUtils.load3DTextureImage("neoncyanpyramid-black");
            Image diffuseImage = ResourceUtils.load3DTextureImage("darkcyanpyramid");
            //PhongMaterial(Color diffuseColor, Image diffuseMap, Image specularMap, Image bumpMap, Image selfIlluminationMap)
            playerMaterial = new PhongMaterial(Color.HOTPINK, diffuseImage, null, specImage, null);
            Image cockpitImage = ResourceUtils.load3DTextureImage("cockpit-transparent");
            cockpitMaterial = new PhongMaterial(Color.WHITE, cockpitImage, null, null, null);

        } catch (IOException ex) {
            LOG.error(null, ex);
            playerMaterial = new PhongMaterial(Color.NAVAJOWHITE);
            cockpitMaterial = new PhongMaterial(Color.TRANSPARENT);
        }
        setMaterial(playerMaterial);
        setDrawMode(DrawMode.FILL);
        setCullFace(CullFace.NONE);

        addEventHandler(DragEvent.DRAG_OVER, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() &&
                JavaFX3DUtils.isTextureFile(db.getFiles().get(0))) {
                event.acceptTransferModes(TransferMode.COPY);
            }
        });
//        addEventHandler(DragEvent.DRAG_ENTERED, event -> expand());
//        addEventHandler(DragEvent.DRAG_EXITED, event -> contract());

        // Dropping over surface
        addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() &&
                JavaFX3DUtils.isTextureFile(db.getFiles().get(0))) {
                Image textureImage;
                try {
                    textureImage = new Image(db.getFiles().get(0).toURI().toURL().toExternalForm());
                    playerMaterial.setDiffuseMap(textureImage);
                    event.setDropCompleted(true);
                } catch (MalformedURLException ex) {
                    LOG.error(null, ex);
                    event.setDropCompleted(false);
                }
                event.consume();
            }
        });
        //point in the Z positive direction by default
        triangleMesh.getPoints().set(2, 300);
        affineTransform = new Affine();
        getTransforms().add(affineTransform);
        setMouseTransparent(true);
    }

    public void setCockPitView(boolean enabled) {
        if (enabled) {
            setMaterial(cockpitMaterial);
        } else {
            setMaterial(playerMaterial);
        }
    }

    /**
     * Accumulate rotation about specified axis
     *
     * @param angle
     * @param axis
     */
    public void addRotation(double angle, Point3D axis) {
        Rotate r = new Rotate(angle, axis);
        /**
         * This is the important bit and thanks to bronkowitz in this post
         * https://stackoverflow.com/questions/31382634/javafx-3d-rotations for
         * getting me to the solution that the rotations need accumulated in
         * this way
         */
        getTransforms().set(0, r.createConcatenation(getTransforms().get(0)));
        affineTransform = (Affine) getTransforms().get(0);
    }

    /**
     * Reset transform to identity transform
     */
    public void reset() {
        velocity = Point3D.ZERO;
        location = Point3D.ZERO;
        affineTransform = new Affine();
        getTransforms().set(0, affineTransform);
    }

    public void mouseDragged(MouseEvent me, double mouseDeltaX, double mouseDeltaY) {
        double modifier = 1.0;
        double modifierFactor = 0.1;  //@TODO SMP connect to sensitivity property

        if (null != me) {
            if (me.isControlDown()) {
                modifier = 0.1;
            }
            if (me.isShiftDown()) {
                modifier = 25.0;
            }
        }
        double yChange = (((mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
        double xChange = (((-mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);

        addRotation(yChange, Rotate.Y_AXIS);
        addRotation(xChange, Rotate.X_AXIS);
    }

    //Reflect(Vector3 vector, Vector3 normal)
    public Point3D reflect(Point3D normal, Point3D direction) {
        //If n is a normalized vector, and v is the incoming direction,
        //then what you want is −(2(n · v) n − v).
        //The minus sign accounts for the fact that the reflection formula
        //doesn't actually reverse the direction, as an object's velocity would reverse.
        double value = 2 * direction.dotProduct(normal);
        Point3D subbed = normal.multiply(value);
        Point3D reflection = direction.subtract(subbed);
        return reflection;
    }

    //Vnew = -2*(V dot N)*N + V
    public Vector3D vecReflec(Vector3D normal, Vector3D velocity) {
        return normal.multiply(-2 * velocity.dotProduct(normal)).add(velocity);
    }

    public Point3D ricochet(Point3D testPoint, Point3D velocity) {
        //figure out target and direction (global coordinates), then normalize
        Point3D gloTarget = testPoint;
        Point3D gloOrigin = testPoint.subtract(velocity);
        Point3D gloDirection = gloTarget.subtract(gloOrigin).normalize();

        //In local coordinates of the shape we have 6 faces given by their normals
        Bounds locBounds = getBoundsInLocal();
        List<Point3D> normals = Arrays.asList(
            new Point3D(-1, 0, 0), new Point3D(1, 0, 0), new Point3D(0, -1, 0),
            new Point3D(0, 1, 0), new Point3D(0, 0, -1), new Point3D(0, 0, 1));

        List<Point3D> positions = Arrays.asList(
            new Point3D(locBounds.getMinX(), 0, 0), new Point3D(locBounds.getMaxX(), 0, 0),
            new Point3D(0, locBounds.getMinY(), 0), new Point3D(0, locBounds.getMaxY(), 0),
            new Point3D(0, 0, locBounds.getMinZ()), new Point3D(0, 0, locBounds.getMaxZ()));
        //Since we'll work on the local system, we need our origin point in this coordinates:
        Point3D gloOriginInLoc = sceneToLocal(gloOrigin);
        //If the shape is no longer axis aligned we need to rotate the normals to match
        if (useParentRotate) {
            for (int i = 0; i < 6; i++) {
                normals.set(i, parentRotateX.transform(
                    parentRotateY.transform(normals.get(i))));
                positions.set(i, parentRotateX.transform(
                    parentRotateY.transform(positions.get(i))));
            }
        }

        Point3D velocityReflection = null;
        Double shortestDistance = null;
        //Go through each normal
        LOG.info("Planar distances: ");
        for (int i = 0; i < 6; i++) {
            //find the distance to the plane
            double d = -normals.get(i).dotProduct(positions.get(i));
            //the distance t to the plane
            double t = Math.abs(
                -(gloOriginInLoc.dotProduct(normals.get(i)) + d)
                    / (gloDirection.dotProduct(normals.get(i)))
            );
            LOG.info("({}, {}) ", d, t);
            //only do the reflection if its a shorter distance
            if (null == shortestDistance || t < shortestDistance) {
                shortestDistance = t;
                //convert normal point to vector
                Vector3D n = new Vector3D(normals.get(i).getX(),
                    normals.get(i).getY(), normals.get(i).getZ());
                //convert velocity to vector
                Vector3D v = new Vector3D(velocity.getX(), velocity.getY(), velocity.getZ());
                //reflect the velocity
                Vector3D nV = vecReflec(n, v);
                velocityReflection = new Point3D(nV.x, nV.y, nV.z);
            }
        }
        LOG.info(".");
        return velocityReflection;
    }

    public boolean rayChecker(Point3D testPoint, Point3D velocity) {
        //figure out target and direction (global coordinates), then normalize
        Point3D gloTarget = testPoint;
        Point3D gloOrigin = testPoint.subtract(velocity);
        Point3D gloDirection = gloTarget.subtract(gloOrigin).normalize();
        //The first step will be checking if the ray intersects the bounding box
        //of our shape. In local coordinates of the shape we have 6 faces given
        //by their normals, with their 6 centers:
        Bounds locBounds = getBoundsInLocal();
        List<Point3D> normals = Arrays.asList(
            new Point3D(-1, 0, 0), new Point3D(1, 0, 0), new Point3D(0, -1, 0),
            new Point3D(0, 1, 0), new Point3D(0, 0, -1), new Point3D(0, 0, 1));
        List<Point3D> positions = Arrays.asList(
            new Point3D(locBounds.getMinX(), 0, 0), new Point3D(locBounds.getMaxX(), 0, 0),
            new Point3D(0, locBounds.getMinY(), 0), new Point3D(0, locBounds.getMaxY(), 0),
            new Point3D(0, 0, locBounds.getMinZ()), new Point3D(0, 0, locBounds.getMaxZ()));

        //Since we'll work on the local system, we need our origin point in this coordinates:
        Point3D gloOriginInLoc = sceneToLocal(gloOrigin);

        //Now, for any of the six faces, we get the distance t to the plane
        //Then we can check if the point belongs to the box or not.
        AtomicInteger counter = new AtomicInteger();

        List<Point3D> intersections = new ArrayList<>();
        IntStream.range(0, 6).forEach(i -> {
            double d = -normals.get(i).dotProduct(positions.get(i));
            double t = -(gloOriginInLoc.dotProduct(normals.get(i)) + d)
                / (gloDirection.dotProduct(normals.get(i)));

            Point3D locInter = gloOriginInLoc.add(gloDirection.multiply(t));
            if (locBounds.contains(locInter)) {
                Point3D sceneIntersection = localToScene(locInter);
                intersections.add(sceneIntersection);
                //is the scene intersection point on this current line segment
                if (Line.between(gloOrigin, gloTarget, sceneIntersection)) {
                    counter.getAndIncrement();
                }
            }
        });
        //@DEBUG SMP helps plot a ray intersect point for debugging
        if (!intersections.isEmpty()) {
            getParent().getScene().getRoot().fireEvent(new HitEvent(
                HitEvent.RAY_INTERSECTS_BOX, this, intersections));
        }
        //If counter.get()>0  we have intersection between the line segment and the shape
        return counter.get() > 0;
    }

    public Point3D bounce(Point3D testPoint, Point3D testVelocity) {
        double boxTx = useLocalTransform ? getTranslateX() : getLocalToSceneTransform().getTx();
        double boxTy = useLocalTransform ? getTranslateY() : getLocalToSceneTransform().getTy();
        double boxTz = useLocalTransform ? getTranslateZ() : getLocalToSceneTransform().getTz();

        double x = testVelocity.getX(),
            y = testVelocity.getY(),
            z = testVelocity.getZ(),
            halfWidth = getBoundsInLocal().getWidth() / 2.0,
            halfHeight = getBoundsInLocal().getHeight() / 2.0,
            halfDepth = getBoundsInLocal().getDepth() / 2.0;
        //crossed lower x
        if (boxTx - halfWidth <= testPoint.getX() //current
            && boxTx - halfWidth >= testPoint.getX() - x) //previous
        {
            x = -x;
        } //crossed upper x
        else if (boxTx + halfWidth >= testPoint.getX() //current
            && boxTx + halfWidth <= testPoint.getX() - x) //previous
        {
            x = -x;
        }

        //crossed lower Y
        if (boxTy - halfHeight <= testPoint.getY() //current
            && boxTy - halfHeight >= testPoint.getY() - y) //previous
        {
            y = -y;
        } //crossed upper Y
        else if (boxTy + halfHeight >= testPoint.getY() //current
            && boxTy + halfHeight <= testPoint.getY() - y) //previous
        {
            y = -y;
        }

        //crossed lower Z
        if (boxTz - halfDepth <= testPoint.getZ() //current
            && boxTz - halfDepth >= testPoint.getZ() - z) //previous
        {
            z = -z;
        } //crossed upper Z
        else if (boxTz + halfDepth >= testPoint.getZ() //current
            && boxTz + halfDepth <= testPoint.getZ() - z) //previous
        {
            z = -z;
        }

        return new Point3D(x, y, z);
    }

    private boolean intersectsPlane(final Line line, final Plane plane) {
        /*   UNnormalized normal = (A,B,C);    P is a specific point in the plane;
         *   (x,y,z) is an arbitrary point in the plane
         *   D = -( A* P.x  +  B* P.y  +  C* P.z )
         *   ( A, B, C ) <dot> ( (x,y,z)  -  P ) = 0  because a plane's normal is
         *      orthogonal to the plane
         *   = Ax  +  By  +  Cz  +  D  =  0 ;
         *   A plane is specified by A,B,C,D.     */
        double denominator = (plane.a * line.v.x) + (plane.b * line.v.y) + (plane.c * line.v.z);
        double numerator = -((plane.a * line.p.x) + (plane.b * line.p.y) + (plane.c * line.p.z) + plane.d);

        if (Math.abs(denominator) < smallDiff) {
            if (Math.abs(numerator) > smallDiff) {    // no solutions case;
                return false;
            } else {// line and plane overlap
                return true;
            }
        } else { // regular solution
            //There is an intersection on an infinte ray somewhere. But Where?
            double u = numerator / denominator;
            Point3D p = new Point3D(line.p.x + (line.v.x * u),
                line.p.y + (line.v.y * u),
                line.p.z + (line.v.z * u));
            //is that point actually on the original line segment?
            //OLD SMP
//         if(line.inLine(p.getX(), p.getY(), p.getZ()))
//            return true;
            org.fxyz3d.geometry.Point3D fxyzP3D
                = org.fxyz3d.geometry.Point3D.convertFromJavaFXPoint3D(p);
            if (line.intersects(fxyzP3D)) {
                return true;
            }
        }
        return false;
    }

    public boolean intersectsPlanes(Point3D testPoint, Point3D velocity) {
//        if (id == 9001) {
//            System.out.println("Over 9000... " + testPoint.getX());
//        }
        //make line vector based on testPoint and velocity
        //test using previous point plus velocity
        Line line = new Line(testPoint.getX() - velocity.getX(),
            testPoint.getY() - velocity.getY(), testPoint.getZ() - velocity.getZ(),
            velocity.getX(), velocity.getY(), velocity.getZ());
        //get actual origin based on transform mode
        double boxTx = useLocalTransform ? getTranslateX() : getLocalToSceneTransform().getTx();
        double boxTy = useLocalTransform ? getTranslateY() : getLocalToSceneTransform().getTy();
        double boxTz = useLocalTransform ? getTranslateZ() : getLocalToSceneTransform().getTz();
        //get distances of plane corners
        double halfWidth = getBoundsInLocal().getWidth() / 2.0;
        double halfHeight = getBoundsInLocal().getHeight() / 2.0;
        double halfDepth = getBoundsInLocal().getDepth() / 2.0;

        //for each plane of the box check intersect
        ///////////////////////////////////////////////////////
        //start with the "left" X side
        double x1 = boxTx - halfWidth;
        double y1 = boxTy - halfHeight;
        double z1 = boxTz + halfDepth;

        double x2 = boxTx - halfWidth;
        double y2 = boxTy - halfHeight;
        double z2 = boxTz - halfDepth;

        double x3 = boxTx - halfWidth;
        double y3 = boxTy + halfHeight;
        double z3 = boxTz + halfDepth;

        //width X plane 1
        Plane xPlane1 = new Plane(x1, y1, z1, x2, y2, z2, x3, y3, z3);
        boolean hitPlane = intersectsPlane(line, xPlane1);
//       if(hitPlane)
//           System.out.println("hitplane");

        ///////////////////////////////////////////////////////
        //change X to "right" side
        x1 = x2 = x3 = boxTx + halfWidth;
        //width X plane 2
        Plane xPlane2 = new Plane(x1, y1, z1, x2, y2, z2, x3, y3, z3);
//       hitPlane |= intersectsPlane(line, xPlane2);
        //@DEBUG SMP
        boolean hitPlane2 = intersectsPlane(line, xPlane2);
        if (hitPlane2) {
            LOG.info("hitplane2");
            boolean debugCheck = intersectsPlane(line, xPlane2);
        }

        ///////////////////////////////////////////////////////
        //width Y "top" plane 1
        x1 = boxTx - halfWidth; //left side
        y1 = boxTy - halfHeight; //top
        z1 = boxTz + halfDepth; //back

        x2 = boxTx - halfWidth; //left side
        y2 = boxTy - halfHeight; //top
        z2 = boxTz - halfDepth; //front

        x3 = boxTx + halfWidth; //right side
        y3 = boxTy - halfHeight; //top
        z3 = boxTz + halfDepth; //back
        //make Y plane 1
        Plane yPlane1 = new Plane(x1, y1, z1, x2, y2, z2, x3, y3, z3);
        hitPlane |= intersectsPlane(line, yPlane1);

        ///////////////////////////////////////////////////////
        //width Y "bottom" plane 2
        //change Y to "bottom"
        y1 = y2 = y3 = boxTy + halfHeight;
        //Make y plane 2
        Plane yPlane2 = new Plane(x1, y1, z1, x2, y2, z2, x3, y3, z3);
        hitPlane |= intersectsPlane(line, yPlane2);

        ///////////////////////////////////////////////////////
        //depth Z "back" plane 1
        x1 = boxTx - halfWidth; //left side
        y1 = boxTy - halfHeight; //top
        z1 = boxTz + halfDepth; //back

        x2 = boxTx - halfWidth; //left side
        y2 = boxTy + halfHeight; //bottom
        z2 = boxTz + halfDepth; //back

        x3 = boxTx + halfWidth; //right side
        y3 = boxTy - halfHeight; //top
        z3 = boxTz + halfDepth; //back
        //make Z plane 1
        Plane zPlane1 = new Plane(x1, y1, z1, x2, y2, z2, x3, y3, z3);
        hitPlane |= intersectsPlane(line, zPlane1);

        ///////////////////////////////////////////////////////
        //depth Z "front" plane 2
        //change Z to "front"
        z1 = z2 = z3 = boxTz - halfDepth;
        //Make y plane 2
        Plane zPlane2 = new Plane(x1, y1, z1, x2, y2, z2, x3, y3, z3);
        hitPlane |= intersectsPlane(line, zPlane2);

        return hitPlane;
    }

    public boolean insideBox(Point3D testPoint) {
        double boxTx = useLocalTransform ? getTranslateX() : getLocalToSceneTransform().getTx();
        double boxTy = useLocalTransform ? getTranslateY() : getLocalToSceneTransform().getTy();
        double boxTz = useLocalTransform ? getTranslateZ() : getLocalToSceneTransform().getTz();

        double halfWidth = getBoundsInLocal().getWidth() / 2.0;
        if (boxTx - halfWidth <= testPoint.getX() && testPoint.getX() <= boxTx + halfWidth) {
            double halfHeight = getBoundsInLocal().getHeight() / 2.0;
            if (boxTy - halfHeight <= testPoint.getY() && testPoint.getY() <= boxTy + halfHeight) {
                double halfDepth = getBoundsInLocal().getDepth() / 2.0;
                if (boxTz - halfDepth <= testPoint.getZ() && testPoint.getZ() <= boxTz + halfDepth) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * @return the useLocalTransform
     */
    public boolean isUseLocalTransform() {
        return useLocalTransform;
    }

    /**
     * @param useLocalTransform the useLocalTransform to set
     */
    public void setUseLocalTransform(boolean useLocalTransform) {
        this.useLocalTransform = useLocalTransform;
    }

    @Override
    public void setStart(Point3D p3D) {
        start = p3D;
    }

    @Override
    public Point3D getStart() {
        return start;
    }

    @Override
    public void setLocation(Point3D p3D) {
        location = p3D;
    }

    @Override
    public Point3D getLocation() {
        return location;
    }

    @Override
    public void setVelocity(Point3D p3D) {
        velocity = p3D;
    }

    @Override
    public Point3D getVelocity() {
        return velocity;
    }

    @Override
    public void flipCheck(double absSafetyPosition) {
        double bufferX = 50;
        double bufferY = 50;
        double bufferZ = 50;
        Point3D loc = getLocation().add(getStart());
        if (loc.getX() < -absSafetyPosition)
            setLocation(new Point3D(absSafetyPosition - bufferX, loc.getY(), loc.getZ()));
        if (loc.getX() > absSafetyPosition)
            setLocation(new Point3D(-absSafetyPosition + bufferX, loc.getY(), loc.getZ()));
        if (loc.getY() < -absSafetyPosition)
            setLocation(new Point3D(loc.getX(), absSafetyPosition - bufferY, loc.getZ()));
        if (loc.getY() > absSafetyPosition)
            setLocation(new Point3D(loc.getX(), -absSafetyPosition + bufferY, loc.getZ()));
        if (loc.getZ() < -absSafetyPosition)
            setLocation(new Point3D(loc.getX(), loc.getY(), absSafetyPosition - bufferZ));
        if (loc.getZ() > absSafetyPosition)
            setLocation(new Point3D(loc.getX(), loc.getY(), -absSafetyPosition + bufferZ));
    }

    public void thrust() {
        //default starting pointing direction is Z+.
        Point3D rotatedThrust = new Point3D(0, 0, defaultThrustMagnitude);
        velocity = velocity.add(getTransforms().get(0)
            .transform(rotatedThrust));
        if (velocity.magnitude() > maxThrustMagnitude) {
            Point3D maxThrust = new Point3D(0, 0, maxThrustMagnitude);
            velocity = getTransforms().get(0).transform(maxThrust);
        }
    }

    @Override
    public boolean update(double _time) {
        location = location.add(velocity);
        setTranslateX(location.getX());
        setTranslateY(location.getY());
        setTranslateZ(location.getZ());
        if (enableRotationAnimation) {
            setRotationAxis(rotateAxis);
            setRotate(getRotate() + rotateIncrementDegrees);
        }
        return true;
    }
}
