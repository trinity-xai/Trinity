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

import edu.jhuapl.trinity.javafx.events.HitEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.media.AudioClip;

import static javafx.scene.media.MediaPlayer.INDEFINITE;
import javafx.util.Pair;

public class ProjectileSystem {
    
    public AnimationTimer projectileTimer;
    private ArrayList<Projectile> projectiles;
    private ArrayList<HitShape3D> hittables;
    private CollisionSweeper collisionSweeper;
    public Point3D spawnPoint = Point3D.ZERO;
    public Group parentGroup;
    private int msInterval = 30;
    private boolean running = true;
    private boolean collisions = true;
    private boolean autoCull = true;
    Media asteriods1981 = null;
    MediaPlayer asteriods1981MediaPlayer = null;
    AudioClip fireSound = null;
    AudioClip bigBoom = null;
    AudioClip smallBoom = null;

    double absSafetyPosition = 2000;
    HitBox xPlusBox;
    HitBox xMinusBox;
    HitBox yPlusBox;
    HitBox yMinusBox;
    HitBox zPlusBox;
    HitBox zMinusBox;
    public PlayerShip playerShip;
    public boolean splittingEnabled = true;
    public double bigThreshold = 200;
    public double fireVelocity = 10.0;
    
    public ProjectileSystem(Group parentGroup, int millisInterval) {
        this.parentGroup = parentGroup;
        msInterval = millisInterval;
        projectiles = new ArrayList<>();
        hittables = new ArrayList<>();
        collisionSweeper = new CollisionSweeper();
        addOuterBox();
        playerShip = new PlayerShip(Point3D.ZERO);
        parentGroup.getChildren().add(playerShip);
        try {
            asteriods1981 = ResourceUtils.loadMediaWav("asteroids1981");
            asteriods1981MediaPlayer = new MediaPlayer(asteriods1981);
            asteriods1981MediaPlayer.setCycleCount(INDEFINITE);
            asteriods1981MediaPlayer.setMute(true);
            fireSound = ResourceUtils.loadAudioClipWav("fire");
            bigBoom = ResourceUtils.loadAudioClipWav("bigBoom");
            smallBoom = ResourceUtils.loadAudioClipWav("smallBoom");
            
        } catch (IOException ex) {
            Logger.getLogger(ProjectileSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        projectileTimer = new AnimationTimer() {
            long msCounter = 0;
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;

            @Override
            public void handle(long now) {
                sleepNs = getMsInterval() * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) {
                    return;
                }
                prevTime = now;
                msCounter += getMsInterval();
                if (running) {
                    //move objects based on simple linear physics, no gravity
                    updateHittables(msCounter);
                    updateProjectiles(msCounter);
                    if (collisions) {
                        //Check to see if any hittable Shape bounced off a barrier
//                        hittableSweep();
                        //check to see if a projectile hit a hittable
                        projectileSweep();
                    }
                    if (autoCull) {
                        //maybe convert to dropWhile?
                        for (int i = 0; i < projectiles.size(); i++) {
                            if (!projectiles.get(i).activeProperty.get())
                                removeProjectile(projectiles.get(i));
                            //projectiles.remove(i);
                        }
                    }
                }
            }
        };
    }
    private void hittableSweep() {
        hittables.stream().forEach(p -> {
            //@TODO SMP might need to do BOTH an inner check and ray check
            //HitBox hit = collisionSweeper.checkCollision(p.location);
            //if(null != hit)
            //    System.out.println("box collision...");
            //sweep through existing hitboxes
            HitBox rayCheck = collisionSweeper.rayCheckFirst(
                p.getLocation().add(p.getStart()),
//                    p.getLocation(),
                p.getVelocity());
            if (null != rayCheck) {
                processCollision(rayCheck, p);
            }
        });
    }
    private void processCollision(HitBox rayCheck, HitShape3D p) {
        HitBox hit = rayCheck;
        //compute the ricochet based on the normal
        Point3D ricochet = hit.ricochet(p.getLocation(), p.getVelocity());
        p.setVelocity(ricochet);
        //bump the position so it doesn't get stuck on plane
        p.setLocation(p.getLocation().add(p.getVelocity()));
        //fire off hit event
        parentGroup.getScene().getRoot().fireEvent(
            new HitEvent(HitEvent.SHAPE_HIT_BOX, p, hit));
    }
    public void fire() {
        javafx.geometry.Point3D start = javafx.geometry.Point3D.ZERO;
        //default starting pointing direction is Z+. 
        javafx.geometry.Point3D velocity = new Point3D(0, 0, fireVelocity); 
        velocity = playerShip.getParent().getTransforms().get(0).transform(velocity);
        FireBall fireBall = new FireBall(20, start, velocity);
        addProjectile(fireBall);
        fireSound.play();
    }
    private boolean isBig(HitShape3D hit, double threshold){ 
        return hit.getBoundsInLocal().getWidth() > threshold 
        || hit.getBoundsInLocal().getHeight() > threshold
        || hit.getBoundsInLocal().getDepth() > threshold;
    }
    

    public void makeAsteroidFromPoints(List<org.fxyz3d.geometry.Point3D> list) throws Exception {
        Manifold3D man3D = new Manifold3D(list, true, false, false, 1.0);
        makeAsteroidFromPoints(man3D);
    }
    public void makeAsteroidFromPoints(Manifold3D man3D) throws Exception {
        HitShape3D hitShape = new HitShape3D(
            man3D.texturedManifold.getVertices(),
            man3D.texturedManifold.getFaces(),
            JavaFX3DUtils.toFX.apply(man3D.getBoundsCentroid())
        );
        javafx.geometry.Point3D velocity = new javafx.geometry.Point3D(
            Hittable.random.nextGaussian() * 0.5,
            Hittable.random.nextGaussian() * 2.1, //initial velocity mostly vertical
            Hittable.random.nextGaussian() * 0.5);
        hitShape.setVelocity(velocity);
        addHitShape(hitShape);
        addHittable(hitShape);
        Platform.runLater(()-> {
        parentGroup.getChildren().add(hitShape);
        });
    }

    public void explodo(HitShape3D hit){
        List<org.fxyz3d.geometry.Point3D> northList = new ArrayList<>();
        List<org.fxyz3d.geometry.Point3D> southList = new ArrayList<>();
        org.fxyz3d.geometry.Point3D center = JavaFX3DUtils.toFXYZ3D.apply(hit.getLocation());
        for(org.fxyz3d.geometry.Point3D vert : hit.texturedManifold.getVertices()) {
            if(vert.substract(center).getY() < 0) {
                southList.add(vert);
            } else {
                northList.add(vert);
            }
        }
        if(southList.size() > 3) {
            try {
                makeAsteroidFromPoints(southList);
            } catch (Exception ex) {
                System.out.println("Could not make Asteroid: " + ex.getMessage());
            }
        }
        if(northList.size() > 3) {
            try {
                makeAsteroidFromPoints(northList);
            } catch (Exception ex) {
                System.out.println("Could not make Asteroid: " + ex.getMessage());
            }
        }
    }
    private void processProjectileImpact(Projectile p, HitShape3D hit) {
        removeProjectile(p);
        Task task = hit.vaporizeTask();
        task.setOnSucceeded(e -> {
            //mark to be culled from scene
            p.activeProperty.set(false); 
            //remove nodes from view
            removeHittable(hit);
            removeHitShape(hit);            
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        //Is it a biggun?
        if(isBig(hit, bigThreshold) ) {
            bigBoom.play();
            //Will we split it?
            if(splittingEnabled) {
                //Is this HitShape3D large enough to break up into chunks??
                System.out.println("Splitting an biggun... ");
                explodo(hit);
            }
        } else {
            //its a wee one
            smallBoom.play();
        }        
        t.start();
        
    }
    private void projectileSweep() {
        List<Pair<Projectile,HitShape3D>> cullList = new ArrayList<>();
        projectiles.stream().forEach(p -> {
            //@TODO SMP might need to do BOTH an inner check and ray check
            //HitBox hit = collisionSweeper.checkCollision(p.location);
            //if(null != hit)
            //    System.out.println("box collision...");
            //sweep through existing hitboxes
            HitShape3D rayCheck = collisionSweeper.rayShapeCheckFirst(p.location, p.velocity);
            if (null != rayCheck) {
                HitShape3D hit = rayCheck;
//For now we are just blowing stuff up
//                //compute the ricochet based on the normal
//                Point3D ricochet = hit.ricochet(p.location, p.velocity);
//                p.velocity = ricochet;
//                //bump the position so it doesn't get stuck on plane
//                p.location = p.location.add(p.velocity);
//                //fire off hit event
//                parentGroup.getScene().getRoot().fireEvent(
//                    new HitEvent(HitEvent.PROJECTILE_HIT_SHAPE, p, hit));
                cullList.add(new Pair<>(p, hit));
            }
        });
        //this will cull these objects while potentially creating more if splitting enabled
        cullList.forEach(pair -> processProjectileImpact(pair.getKey(), pair.getValue()));

    }

    public void addOuterBox() {
        double size = 2000;
        double position = 1000;
        double depth = 100;
        //Do some Projectile Commenting
        xPlusBox = new HitBox(new Point3D(position, 0, 0), depth, size, size);
        xMinusBox = new HitBox(new Point3D(-position, 0, 0), depth, size, size);
        xMinusBox.id = 9001;
        yPlusBox = new HitBox(new Point3D(0, position, 0), size, depth, size);
        yMinusBox = new HitBox(new Point3D(0, -position, 0), size, depth, size);
        zPlusBox = new HitBox(new Point3D(0, 0, position), size, size, depth);
        zMinusBox = new HitBox(new Point3D(0, 0, -position), size, size, depth);

//        //@DEBUG SMP
//        parentGroup.getChildren().add(yPlusBox);
//        ((PhongMaterial)yPlusBox.getMaterial()).setDiffuseColor(
//        Color.CYAN.deriveColor(1, 1, 1, 0.1));
//        parentGroup.getChildren().add(yMinusBox);
//        ((PhongMaterial)yMinusBox.getMaterial()).setDiffuseColor(
//        Color.RED.deriveColor(1, 1, 1, 0.1));

        addHitBox(xPlusBox);
        addHitBox(xMinusBox);

        addHitBox(yPlusBox);
        addHitBox(yMinusBox);

        addHitBox(zPlusBox);
        addHitBox(zMinusBox);

    }

    public void addHitShape(HitShape3D hitShape) {
        collisionSweeper.getHitShapes().add(hitShape);
    }

    public void removeHitShape(HitShape3D hitShape) {
        collisionSweeper.getHitShapes().remove(hitShape);
    }

    public void clearAllHitShapes() {
        collisionSweeper.getHitShapes().clear();
    }

    public void addHitBox(HitBox hitBox) {
        collisionSweeper.getHitBoxes().add(hitBox);
        //parentGroup.getChildren().add(hitBox.box);
    }

    public void removeHitBox(HitBox hitBox) {
        collisionSweeper.getHitBoxes().remove(hitBox);
    }

    public void clearAllHitBoxes() {
        collisionSweeper.getHitBoxes().clear();
    }

    public void addHittable(HitShape3D hittable) {
        hittables.add(hittable);
//        parentGroup.getChildren().add(hittable.getShape3D());
    }

    public void removeHittable(HitShape3D hittable) {
        hittables.remove(hittable);
        parentGroup.getChildren().remove(hittable);
    }

    public void clearAllHittables() {
//        for(Hittable p : projectiles) {
//            parentGroup.getChildren().remove(p.getShape3D());
//        }
        hittables.clear();
    }

    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
        parentGroup.getChildren().add(projectile.getShape3D());
    }

    public void removeProjectile(Projectile projectile) {
        projectiles.remove(projectile);
        parentGroup.getChildren().remove(projectile.getShape3D());
    }

    public void clearAllProjectiles() {
        for (Projectile p : projectiles) {
            parentGroup.getChildren().remove(p.getShape3D());
        }
        projectiles.clear();
    }

    public void setEnableProjectileTimer(boolean enable) {
        if (enable) {
            projectileTimer.start();
        } else {
            projectileTimer.stop();
        }
    }

    private HitBox safetyCheck(Point3D p) {
        if (p.getX() < -absSafetyPosition) return xMinusBox;
        else if (p.getX() > absSafetyPosition) return xPlusBox;
        else if (p.getY() < -absSafetyPosition) return yMinusBox;
        else if (p.getY() > absSafetyPosition) return yPlusBox;
        else if (p.getZ() < -absSafetyPosition) return zMinusBox;
        else if (p.getZ() > absSafetyPosition) return zPlusBox;
        return null;
    }

    private void flipCheck(HitShape3D hitShape) {
        double bufferX = hitShape.getVelocity().getX() * 2;
        double bufferY = hitShape.getVelocity().getY() * 2;
        double bufferZ = hitShape.getVelocity().getZ() * 2;
        Point3D loc = hitShape.getLocation().add(hitShape.getStart());
        if (loc.getX() < -absSafetyPosition)
            hitShape.setLocation(new Point3D(absSafetyPosition - bufferX, loc.getY(), loc.getZ()));
        if (loc.getX() > absSafetyPosition)
            hitShape.setLocation(new Point3D(-absSafetyPosition + bufferX, loc.getY(), loc.getZ()));
        if (loc.getY() < -absSafetyPosition)
            hitShape.setLocation(new Point3D(loc.getX(), absSafetyPosition - bufferY, loc.getZ()));
        if (loc.getY() > absSafetyPosition)
            hitShape.setLocation(new Point3D(loc.getX(), -absSafetyPosition + bufferY, loc.getZ()));
        if (loc.getZ() < -absSafetyPosition)
            hitShape.setLocation(new Point3D(loc.getX(), loc.getY(), absSafetyPosition - bufferZ));
        if (loc.getZ() > absSafetyPosition)
            hitShape.setLocation(new Point3D(loc.getX(), loc.getY(), -absSafetyPosition + bufferZ));

    }

    public void updateHittables(long millis) {
        if (null != hittables) {
            hittables.forEach(p -> {
                if (!p.update(millis)) {
                    //p.activeProperty.set(false);
                } else
                    flipCheck(p);
            });
        }
    }

    public void updateProjectiles(long millis) {
        if (null != projectiles) {
            projectiles.forEach(p -> {
                if (!p.update(millis))
                    p.activeProperty.set(false);
            });
        }
    }

    /**
     * Get the number of projectiles that are in the system right now.
     *
     * @return
     */
    public int getNumberOfProjectiles() {
        return projectiles.size();
    }

    /**
     * Check to see if this projectile system is running.
     *
     * @return boolean running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Tell the projectile system to start/stop running.
     *
     * @param _isRunning
     */
    public void setRunning(final boolean _isRunning) {
        running = _isRunning;
        if (null != asteriods1981MediaPlayer)
            if (running) {
                asteriods1981MediaPlayer.setMute(false);
                asteriods1981MediaPlayer.play();
            } else {
                asteriods1981MediaPlayer.setMute(true);
                asteriods1981MediaPlayer.pause();
            }
    }

    /**
     * @return the projectiles
     */
    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    /**
     * @return the autoCullInactive
     */
    public boolean isAutoCullInactive() {
        return autoCull;
    }

    /**
     * @param autoCullInactive the autoCullInactive to set
     */
    public void setAutoCullInactive(boolean autoCullInactive) {
        this.autoCull = autoCullInactive;
    }

    /**
     * @return the msInterval
     */
    public int getMsInterval() {
        return msInterval;
    }

    /**
     * @param msInterval the msInterval to set
     */
    public void setMsInterval(int msInterval) {
        this.msInterval = msInterval;
        if (this.msInterval < 10)
            this.msInterval = 10;
    }
}
