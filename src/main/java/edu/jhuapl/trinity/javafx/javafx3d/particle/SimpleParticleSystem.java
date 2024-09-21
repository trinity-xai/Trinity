package edu.jhuapl.trinity.javafx.javafx3d.particle;

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

import edu.jhuapl.trinity.utils.Utils;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import org.fxyz3d.geometry.Point3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class SimpleParticleSystem {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleParticleSystem.class); //extends BillboardNode {

    public AnimationTimer particleTimer;
    private ParticleSet particles;
    private float particlesPerMS = 0.005f;
    public Point3D spawnPoint = new Point3D(0, 0, 0);
    public Group parentGroup;
    public int msInterval = 30;

    public SimpleParticleSystem(Particle _spawnType, Group parentGroup, int msInterval) {
        this.parentGroup = parentGroup;
        this.msInterval = msInterval;
        particleTimer = new AnimationTimer() {
            long msCounter = 0;
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;

            @Override
            public void handle(long now) {
                sleepNs = msInterval * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) {
                    return;
                }
                prevTime = now;
                msCounter += msInterval;
                updateParticles(msCounter);
                //System.out.println("Updated Matrix " + now / 1000000000);
            }
        };
        long startTime = System.nanoTime();
        setParticle(_spawnType);
        LOG.info("Particles generated: {}", Utils.totalTimeString(startTime));
        Platform.runLater(() -> {
            parentGroup.getChildren().addAll(particles.group);
        });
    }

    public void setEnableParticleTimer(boolean enable) {
        if (enable) {
            particleTimer.start();
        } else {
            particleTimer.stop();
        }
    }

    public void updateParticles(long millis) {
        if (null != particles) {
            particles.doAdvance(millis);
        }
    }

    /**
     * Change the particle type.
     *
     * @param _spawnType The new type of particle to spawn.
     */
    public void setParticle(final Particle _spawnType) {
        particles = new ParticleSet(_spawnType.getClass(), spawnPoint);
    }

    /**
     * Get the number of particles that are in the system right now.
     *
     * @return
     */
    public int getNumberOfParticles() {
        return particles.getActiveParticleCount();
    }

    /**
     * Set the number of new particles to spawn per millisecond (not to exceed
     * the max particles though).
     *
     * @param _particlesPerMillisecond
     */
    public void setParticlesPerMillisecond(final float _particlesPerMillisecond) {
        particlesPerMS = _particlesPerMillisecond;
        if (particles.getParticlesPerMillisecond() > 0) {
            particles.setParticlesPerMillisecond(_particlesPerMillisecond);
        }
    }

    /**
     * Get the number of new particles to spawn per millisecond (not to exceed
     * the max particles though).
     */
    public float getParticlesPerMillisecond() {
        return particlesPerMS;
    }

    /**
     * Set the maximum number of particles for the particle system.
     *
     * @param _maxParticles The maximum number of particles.
     */
    public void setMaxNumParticles(final int _maxParticles) {
        try {
            particles.setMaxParticleCount(_maxParticles);
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | InstantiationException | IllegalAccessException ex) {
            LOG.error(null, ex);
        }
    }

    /**
     * Get the maximum number of particles for the particle system.
     *
     * @return The maximum number of requested particles.
     */
    public int getMaxNumParticles() {
        return particles.getMaxParticleCount();
    }

    /**
     * Check to see if this particle system is running (spawning new particles).
     */
    public boolean isRunning() {
        return particles.getParticlesPerMillisecond() > 0;
    }

    /**
     * Tell the particle system to start/stop running (spawning new particles).
     */
    public void setRunning(final boolean _isRunning) {
        if (_isRunning) {
            particles.setParticlesPerMillisecond(particlesPerMS);
        } else {
            particles.setParticlesPerMillisecond(0);
        }
    }

    //   /** This draws my display list, this dispatches to the postDrawRender implementation. */
//   @Override
//   public void postDraw(final GL2 _gl, final GLU _glu, final Graph3D _graph3D, final AppearanceSettings3D _settings) {
//      postDrawRender(_gl, _glu, _graph3D, _settings);
//   }
//   @Override
//   public void getDepthRange(final DepthRangeQuery in) {
//      particles.getDepthRange(this, in);
//   }
//
//    @Override
    protected Node getBillboardNode() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    //    @Override
    protected Node getTarget() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    /**
     * @return the particles
     */
    public Object[] getParticleArray() {
        return particles.getParticles();
    }
}
