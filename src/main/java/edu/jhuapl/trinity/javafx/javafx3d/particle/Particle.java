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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import org.fxyz3d.geometry.Point3D;

import java.util.Random;

public abstract class Particle {
    /**
     * Random instance for random behavior.
     */
    protected static final Random random = new Random();

    /**
     * Flag indicating whether the particle is active.
     */
    public SimpleBooleanProperty activeProperty = new SimpleBooleanProperty(false);
    /**
     * Initial/Current Location of the particle.
     */
    public Point3D location = new Point3D(0, 0, 0);

    public void setRandomLocation(float lowerBound, float upperBound) {
        location.x = random.nextFloat() * (upperBound - lowerBound) + lowerBound;
        location.y = random.nextFloat() * (upperBound - lowerBound) + lowerBound;
        location.z = random.nextFloat() * (upperBound - lowerBound) + lowerBound;
    }

    public abstract void reset();

    /**
     * @return he Node (typically ImageView) that is added to the scene
     */
    public abstract Node getNode();

    /**
     * Updates the particle animation by the specified number of milliseconds.
     *
     * @param _time The number of milliseconds elapsed since the last update.
     * @return true if the particle is still 'alive' (requires further animation), false if it has terminated.
     */
    public boolean update(final double _time) {
        return false;
    }
}
