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

import javafx.geometry.Point3D;

import java.util.Random;

public interface Hittable {
    /**
     * Random instance for random behavior.
     */
    static Random random = new Random();

//   /**
//    * Flag indicating whether the particle is active.
//    */
//   public SimpleBooleanProperty activeProperty = new SimpleBooleanProperty(true);

    /**
     * physics of the hittable.
     */
    public void setStart(Point3D p3D);

    public Point3D getStart();

    public void setLocation(Point3D p3D);

    public Point3D getLocation();

    public void setVelocity(Point3D p3D);

    public Point3D getVelocity();


    public static Point3D getRandomPoint3D(float lowerBound, float upperBound) {
        return new Point3D(
            random.nextFloat() * (upperBound - lowerBound) + lowerBound,
            random.nextFloat() * (upperBound - lowerBound) + lowerBound,
            random.nextFloat() * (upperBound - lowerBound) + lowerBound
        );
    }
    
    public void flipCheck(double absSafetyPosition);

    /**
     * Updates the particle animation by the specified number of milliseconds.
     *
     * @param _time The number of milliseconds elapsed since the last update.
     * @return true if the particle is still 'alive' (requires further animation), false if it has terminated.
     */
    public abstract boolean update(final double _time);
}
