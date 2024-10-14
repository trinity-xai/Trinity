/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.javafx3d.particle;

import edu.jhuapl.trinity.utils.CircularConcurrentQueue;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import org.fxyz3d.geometry.Point3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Class to manage a set of animated particles.
 */
public class ParticleSet {
    private static final Logger LOG = LoggerFactory.getLogger(ParticleSet.class);

    public static int DEFAULT_PARTICLE_COUNT = 50;
    private final Class<? extends Particle> particleType;
    public Group group = new Group();
    public Particle[] particles = new Particle[0]; // The array of particles;
    public CircularConcurrentQueue<Particle> particleQueue;
    private float particlesPerMillisecond = 0; // how fast this thing emits
    private int numAlive; // The number of currently living particles, defaults to 0
    private double msSinceSpawn = 0; // Milliseconds since last spawn, if the updates come faster than we are spawning,
    private double lastSpawn = 0;
    // accumulate
    private Point3D spawnPoint = new Point3D(0, 0, 0);

    /**
     * Creates a new animated particle set that uses particles of the specified
     * type.
     *
     * @param particleType The particle type, must provide a public, no-argument
     *                     constructor.
     */
    public ParticleSet(Class<? extends Particle> particleType) {
        this(particleType, new Point3D(0, 0, 0));
    }

    public ParticleSet(Class<? extends Particle> particleType, Point3D spawnPoint) {
        this.particleType = particleType;
        this.spawnPoint = spawnPoint;
        try {
            generateParticles(DEFAULT_PARTICLE_COUNT);
        } catch (NoSuchMethodException | IllegalArgumentException
                 | InvocationTargetException | InstantiationException
                 | IllegalAccessException ex) {
            LOG.error(null, ex);
        }

    }

    private void generateParticles(final int maxParticles)
        throws NoSuchMethodException, IllegalArgumentException,
        InvocationTargetException, InstantiationException, IllegalAccessException {
        final Particle[] newParticles = Arrays.copyOf(particles, maxParticles);
        ArrayList<Node> nodes = new ArrayList<>(newParticles.length);
        if (newParticles.length > particles.length) {
            for (int i = particles.length; i < newParticles.length; ++i) {
                newParticles[i] = particleType.getDeclaredConstructor().newInstance();
                newParticles[i].location = new Point3D(
                    spawnPoint.x, spawnPoint.y, spawnPoint.z);
                newParticles[i].activeProperty.set(false);  //default to off
                nodes.add(newParticles[i].getNode());
            }
        }
        particles = newParticles;
        particleQueue = new CircularConcurrentQueue<>(Arrays.asList(particles));
        Platform.runLater(() -> {
            group.getChildren().addAll(nodes);
        });
    }

    protected void doAdvance(final double millis) {
        final float particlesPerMS = this.particlesPerMillisecond;
        int numLiving = 0;
        /*
         * For every particle, check to see if the particle is active,
         * update it if it is, and increase the numLinving counter.
         * If the update returns false it died.
         */
        for (Object object : particleQueue.getElements()) {
            Particle man = (Particle) object; //does whatever a particle can...
            if (man.activeProperty.get()) {
                if (man.update(millis)) {
                    numLiving++;
                }
            }
        }

        // Check on the time since the last spawn
        msSinceSpawn = millis - lastSpawn;
        // If we are not suppose to spawn new particles
        if (particlesPerMS <= 0) {
            // record how many are living right now
            numAlive = numLiving;
            // Do not accumulate time since spawn
            msSinceSpawn = 0;
            return;
        }
        // Figure out how many particles we are suppose to make
        int particlesToCreate = (int) (particlesPerMS * msSinceSpawn);
        LOG.info("Particles to Create: {}", particlesToCreate);

        // If we didn't create a single particle remember the time and bail
        if (particlesToCreate >= 1) {
            // This is the most recent spawn time, reset before proceeding
            lastSpawn = millis;
            msSinceSpawn = 0;
            // Do not spawn TOO many particles
            final int maxParticles = particleQueue.getElementsLength();
            if ((particlesToCreate + numLiving) > maxParticles) {
                particlesToCreate = (maxParticles - numLiving);
            }
            int particlesCreated = 0;
            //if we go through the whole queue without breaking that means maximum
            //available particles have been activated.  Shouldn't be able to get this
            //far if that is true but....
            for (int i = 0; i < maxParticles; i++) {
                Particle man = particleQueue.get(); //does whatever a particle can...
                if (!man.activeProperty.get()) {
                    man.setRandomLocation(-2f, 2f);
                    man.activeProperty.set(true);
                    particlesCreated++;
                    numLiving++;
                    if (particlesCreated > particlesToCreate || numLiving > maxParticles)
                        break;
                }
            }
            numAlive = numLiving;
        }
    }

    /**
     * Get the number of particles that are in the system right now.
     *
     * @return The number of active particles.
     */
    public int getActiveParticleCount() {
        return numAlive;
    }

    /**
     * Set the number of new particles to spawn per millisecond (not to exceed
     * the max particles though).
     *
     * @param _particlesPerMillisecond The number of particles to spawn per ms.
     */
    public synchronized void setParticlesPerMillisecond(final float _particlesPerMillisecond) {
        particlesPerMillisecond = _particlesPerMillisecond;
    }

    /**
     * Get the number of new particles to spawn per millisecond (not to exceed
     * the max particles though).
     *
     * @return The number of particles to spawn per ms.
     */
    public synchronized float getParticlesPerMillisecond() {
        return particlesPerMillisecond;
    }

    /**
     * Set the maximum number of particles for the particle system.
     *
     * @param _maxParticles The maximum number of particles.
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.InstantiationException
     * @throws java.lang.reflect.InvocationTargetException
     * @throws java.lang.IllegalAccessException
     */
    public void setMaxParticleCount(final int _maxParticles) throws
        NoSuchMethodException, IllegalArgumentException, InvocationTargetException,
        InstantiationException, IllegalAccessException {
        generateParticles(_maxParticles);
    }

    /**
     * Get the maximum number of particles for the particle system.
     *
     * @return The maximum number of requested particles.
     */
    public int getMaxParticleCount() {
        return particles.length;
    }

    public final Object[] getParticles() {
        return particleQueue.getElements();
    }

//   /**
//    * Translates all active particles by the specified vector.
//    *
//    * @param x
//    *           The x translation component.
//    * @param y
//    *           The y translation component.
//    * @param z
//    *           The z translation component.
//    */
//   private void translateParticles(final Particle[] particles, final double x, final double y, final double z) {
//      for (final Particle particle : particles) {
//         // Draw the particle, if it's alive
//         if (particle.active) {
//            particle.location.x += x;
//            particle.location.y += y;
//            particle.location.z += z;
//         }
//      }
//   }
//   public void renderParticles(final GL2 gl, final Graph3D graph, final Tuple3d origin,
//         final AppearanceSettings3D settings) {
//
//      final Particle[] particles = this.particles;
//
//      final Camera camera = graph.getCamera3D();
//      final Vector3d locale = graph.getCamera3D().getLocaleOrigin();
//      final double[] baseLoc = origin.get();
//
//      /*
//       * If the scene element moved since the last time we rendered, translate all active particles so they draw at
//       * their original location.
//       */
//      if (previousLoc != null && !Arrays.equals(baseLoc, previousLoc)) {
//         final double particleTransX = previousLoc[0] - baseLoc[0];
//         final double particleTransY = previousLoc[1] - baseLoc[1];
//         final double particleTransZ = previousLoc[2] - baseLoc[2];
//
//         translateParticles(particles, particleTransX, particleTransY, particleTransZ);
//      }
//      previousLoc = baseLoc.clone();
//
//      final double xTrans = baseLoc[0] - locale.x;
//      final double yTrans = baseLoc[1] - locale.y;
//      final double zTrans = baseLoc[2] - locale.z;
//
//      particleSettings.applySettings(gl, settings);
//
//      gl.glEnable(GL2.GL_TEXTURE_2D);
//      gl.glDepthMask(false);
//
//      gl.glPushMatrix();
//      gl.glLoadIdentity();
//      graph.getCamera3D().positionCamera(gl);
//      final double[] axisAngle = camera.getOrientation().toAxisAngle(null);
//
//      for (final Particle particle : particles) {
//         // Draw the particle, if it's alive
//         if (particle.active) {
//            gl.glPushMatrix();
//            final Tuple3f loc = particle.location;
//            gl.glTranslated(loc.x + xTrans, loc.y + yTrans, loc.z + zTrans);
//
//            gl.glRotated(axisAngle[3] * MathConstants.RADS_TO_DEG, axisAngle[0], axisAngle[1], axisAngle[2]);
//            particle.render(graph, gl, Graph3D.sharedGLU);
//            gl.glPopMatrix();
//         }
//      }
//
//      gl.glPopMatrix();
//      gl.glDepthMask(true);
//      gl.glDisable(GL2.GL_TEXTURE_2D);
//
//      settings.applySettings(gl, particleSettings);
//   }
//   public void getDepthRange(final BaseSceneElement element, final DepthRangeQuery in) {
//      final Matrix4d invert = element.getScaledMatrix();
//      invert.invert();
//
//      final DepthRangeQuery out = in.getTransformedInstance(invert);
//
//      double min = Double.MAX_VALUE;
//      double max = -Double.MAX_VALUE;
//
//      for (final Particle p : particles) {
//         final Plane plane = out.imagePlane;
//         final double distance = plane.distance(p.location.x, p.location.y, p.location.z);
//
//         if (distance > 0) {
//            min = Math.min(distance, min);
//            max = Math.max(distance, max);
//         }
//      }
//
//      out.setMinFar(max);
//      out.setNearDistance(min);
//
//      in.mergeWith(out);
//   }
}
