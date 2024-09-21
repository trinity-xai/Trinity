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
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static javafx.scene.media.MediaPlayer.INDEFINITE;

public class ProjectileSystem {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectileSystem.class);

    public AnimationTimer projectileTimer;
    private ArrayList<Projectile> projectiles;
    private ArrayList<HitShape3D> hittables;
    private CollisionSweeper collisionSweeper;
    public Point3D spawnPoint = Point3D.ZERO;
    public Group parentGroup;
    private int msInterval = 30;
    private Random random = new Random();
    private boolean inMotion = true;
    private boolean running = true;
    private boolean collisions = true;
    private boolean autoCull = true;
    Media asteriods1981 = null;
    MediaPlayer asteriods1981MediaPlayer = null;
    AudioClip fireSound = null;
    AudioClip bigBoom = null;
    AudioClip smallBoom = null;
    AudioClip thrust = null;
    AudioClip saucerBig = null;

    double absSafetyPosition = 2000;
    HitBox xPlusBox;
    HitBox xMinusBox;
    HitBox yPlusBox;
    HitBox yMinusBox;
    HitBox zPlusBox;
    HitBox zMinusBox;
    public PlayerShip playerShip;
    public Alien alienShip;
    public boolean splittingEnabled = true;
    public double bigThreshold = 200;
    public double fireVelocity = 30.0;
    public boolean introPlayed = false;

    public ProjectileSystem(Group parentGroup, int millisInterval) {
        this.parentGroup = parentGroup;
        msInterval = millisInterval;
        projectiles = new ArrayList<>();
        hittables = new ArrayList<>();
        collisionSweeper = new CollisionSweeper();
        addOuterBox();
        playerShip = new PlayerShip(Point3D.ZERO);
        parentGroup.getChildren().add(playerShip);
        playerShip.addRotation(30, Rotate.Z_AXIS);
        alienShip = new Alien(Color.FIREBRICK, 100);

        try {
            asteriods1981 = ResourceUtils.loadMediaWav("asteroids1981");
            asteriods1981MediaPlayer = new MediaPlayer(asteriods1981);
            asteriods1981MediaPlayer.setCycleCount(INDEFINITE);
            asteriods1981MediaPlayer.setMute(true);
            fireSound = ResourceUtils.loadAudioClipWav("fire");
            bigBoom = ResourceUtils.loadAudioClipWav("bigBoom");
            smallBoom = ResourceUtils.loadAudioClipWav("smallBoom");
            thrust = ResourceUtils.loadAudioClipWav("thrust");
            saucerBig = ResourceUtils.loadAudioClipWav("saucerBig");

//            //EXPERIMENTAL Load 3D model for king/warlord
//            MaterialModel crowns3D = new MaterialModel("crowns", Color.CYAN, 100);
//            crowns3D.setChildVisible(0, false);
//            crowns3D.setChildVisible(2, false);
//            parentGroup.getChildren().add(crowns3D);
//            crowns3D.setOnMouseClicked(e -> {
//                if (e.getClickCount() > 1 && e.isControlDown()) {
//                    crowns3D.resetMaterials(Color.BLACK);
//                }
//            });
//            //Milkywaygalaxy as diffuse plus a colored self illumination looks good
//            crowns3D.setRotationAxis(Rotate.Z_AXIS);
//            crowns3D.setRotate(180);

        } catch (IOException ex) {
            LOG.error(null, ex);
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
                    updatePlayer(msCounter);
                    updateAlien(msCounter);
                    //move objects based on simple linear physics, no gravity
                    if (inMotion) {
                        updateHittables(msCounter);
                    }
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
                            if (!projectiles.get(i).activeProperty.get()) {
                                removeProjectile(projectiles.get(i));
                            }
                        }
                    }
                }
            }
        };

    }

    public void toggleAlien() {
        if (!parentGroup.getChildren().contains(alienShip)) {
            Point3D start = alienShip.randomStart(absSafetyPosition);
            //reverse the order of the points because alienship is already rotated
            JavaFX3DUtils.lookAt(alienShip, playerShip.getLocation(),
                start, false);
            alienShip.setStart(start);
            alienShip.setLocation(start);
            if (start.getX() < 0) {
                alienShip.setVelocity(new Point3D(10, 0, 0));
            } else {
                alienShip.setVelocity(new Point3D(-10, 0, 0));
            }

            parentGroup.getChildren().add(alienShip);
            saucerBig.setCycleCount(AudioClip.INDEFINITE);
            saucerBig.setVolume(0.2);
            saucerBig.setRate(0.5);
            saucerBig.play();
        } else {
            saucerBig.stop();
            parentGroup.getChildren().remove(alienShip);
            alienShip.reset();

        }
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

    public void thrustPlayer() {
        thrust.play();
        playerShip.thrust();
    }

    public void fire() {
        //default starting pointing direction is Z+.
        Point3D velocity = new Point3D(0, 0, fireVelocity);
        velocity = playerShip.getTransforms().get(0).transform(velocity);
        FireBall fireBall = new FireBall(FireBall.DEFAULT_FIREBALL_RADIUS,
            playerShip.getLocation(), velocity);
        addProjectile(fireBall);
        fireSound.play();
    }

    public void fireTracer() {
        //default starting pointing direction is Z+.
        Point3D velocity = new Point3D(0, 0, fireVelocity);
        velocity = playerShip.getTransforms().get(0).transform(velocity);
        Point3D end = playerShip.getTransforms().get(0).transform(
            new Point3D(0, 0, TracerRound.DEFAULT_TRACER_LENGTH));
        TracerRound tracer = new TracerRound(TracerRound.DEFAULT_TRACER_WIDTH,
            playerShip.getLocation(), end, velocity);

        addProjectile(tracer);
        fireSound.play();
    }

    private boolean isBig(HitShape3D hit, double threshold) {
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
        Point3D velocity = new Point3D(
            Hittable.random.nextGaussian() * 0.25,
            Hittable.random.nextGaussian() * 2.1, //initial velocity mostly vertical
            Hittable.random.nextGaussian() * 0.25);
        hitShape.setVelocity(velocity);
        addHitShape(hitShape);
        addHittable(hitShape);
        Platform.runLater(() -> {
            parentGroup.getChildren().add(hitShape);
        });
    }

    public void explodo(HitShape3D hit) {
        List<org.fxyz3d.geometry.Point3D> northList = new ArrayList<>();
        List<org.fxyz3d.geometry.Point3D> southList = new ArrayList<>();
        org.fxyz3d.geometry.Point3D center = JavaFX3DUtils.toFXYZ3D.apply(hit.getLocation());
        for (org.fxyz3d.geometry.Point3D vert : hit.texturedManifold.getVertices()) {
            if (vert.substract(center).getY() < 0) {
                southList.add(vert);
            } else {
                northList.add(vert);
            }
        }
        if (southList.size() > bigThreshold / 2) {
            try {
                makeAsteroidFromPoints(southList);
            } catch (Exception ex) {
                LOG.info("Could not make Asteroid: {}", ex.getMessage(), ex);
            }
        }
        if (northList.size() > bigThreshold / 2) {
            try {
                makeAsteroidFromPoints(northList);
            } catch (Exception ex) {
                LOG.info("Could not make Asteroid: {}", ex.getMessage(), ex);
            }
        }
    }

    private void processProjectileImpact(Projectile p, HitShape3D hit) {
        if (!p.activeProperty.get()) return; //don't do a second impact
        removeProjectile(p);
        p.activeProperty.set(false);
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
        //fire off hit event
        parentGroup.getScene().getRoot().fireEvent(
            new HitEvent(HitEvent.PROJECTILE_HIT_SHAPE, p, hit));
        if (isBig(hit, bigThreshold)) {
            bigBoom.play();
            //Will we split it?
            if (splittingEnabled) {
                //Is this HitShape3D large enough to break up into chunks??
                LOG.info("Splitting an biggun... ");
                explodo(hit);
            }
        } else {
            //its a wee one
            smallBoom.play();
        }
        t.start();

    }

    private void projectileSweep() {
        List<Pair<Projectile, HitShape3D>> cullList = new ArrayList<>();
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

    public void updateHittables(long millis) {
        if (null != hittables) {
            hittables.forEach(p -> {
                if (!p.update(millis)) {
                    //p.activeProperty.set(false);
                } else {
                    p.flipCheck(absSafetyPosition);
                }
            });
        }
    }

    public void updateAlien(long millis) {
        if (null != alienShip && parentGroup.getChildren().contains(alienShip)) {
            if (!alienShip.update(millis)) //has it expired somehow?
            {
                alienShip.activeProperty.set(false);
            }

            alienShip.flipCheck(absSafetyPosition); //did it flip over?
            //reverse the order of the points because alienship is already rotated
            JavaFX3DUtils.lookAt(alienShip, playerShip.getLocation(),
                alienShip.getLocation(), false);
            if (random.nextDouble() >= 0.985) {
                //Minus because the Alien is rotated 180 degrees ahead of time
                Point3D velocity = new Point3D(0, 0, -fireVelocity);
                velocity = alienShip.getTransforms().get(0).transform(velocity);
                FireBall fireBall = new FireBall(15, alienShip.getLocation(), velocity);
                addProjectile(fireBall);
                fireSound.play();
            }
            //After all that is it no longer active logically?
            if (!alienShip.activeProperty.get()) //if not we should remove it
            {
                toggleAlien();
            }
        }
    }

    public void updatePlayer(long millis) {
        if (null != playerShip) {
            if (!playerShip.update(millis)) {
                playerShip.activeProperty.set(false);
            }
            playerShip.flipCheck(absSafetyPosition);
        }
    }

    public void updateProjectiles(long millis) {
        if (null != projectiles) {
            projectiles.forEach(p -> {
                if (!p.update(millis)) {
                    p.activeProperty.set(false);
                }
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
     * Check to see if this projectile system is animating objects.
     *
     * @return boolean running
     */
    public boolean isInMotion() {
        return inMotion;
    }

    /**
     * Tell the projectile system to start/stop motion.
     *
     * @param _isRunning
     */
    public void setInMotion(final boolean _inMotion) {
        inMotion = _inMotion;
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
        if (null != parentGroup.getScene()) {
            parentGroup.getScene().getRoot().fireEvent(
                new HitEvent(HitEvent.TRACKING_PROJECTILE_EVENTS, running, null));
        }
        if (null != asteriods1981MediaPlayer) {
            if (running) {
                asteriods1981MediaPlayer.setMute(false);
                asteriods1981MediaPlayer.play();
            } else {
                asteriods1981MediaPlayer.setMute(true);
                asteriods1981MediaPlayer.pause();
            }
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
        if (this.msInterval < 10) {
            this.msInterval = 10;
        }
    }
}
