package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

/*-
 * #%L
 * trinity-2024.06.03
 * %%
 * Copyright (C) 2021 - 2024 The Johns Hopkins University Applied Physics Laboratory LLC
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

import java.util.Random;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.shape.Shape3D;

public abstract class Projectile {
   /**
    * Random instance for random behavior.
    */
   protected static Random random = new Random();
   
   /**
    * Flag indicating whether the particle is active.
    */
   public SimpleBooleanProperty activeProperty = new SimpleBooleanProperty(true);
   /**
    * physics of the particle.
    */
   public Point3D start = new Point3D(0,0,0);
   public Point3D location = new Point3D(0,0,0);
   public Point3D velocity = new Point3D(0,0,0);
   
   public static Point3D getRandomPoint3D(float lowerBound, float upperBound) {
       return new Point3D(
            random.nextFloat() * (upperBound - lowerBound) + lowerBound,
            random.nextFloat() * (upperBound - lowerBound) + lowerBound,
            random.nextFloat() * (upperBound - lowerBound) + lowerBound
       );
   }

   /**
    * Logic to tell implementation to reset to defaults/initial conditions
    */
   public abstract void reset();
   /**
    * @return the Shape3D that is added to the 3D scene
    */
   public abstract Shape3D getShape3D();
  /**
    * Updates the particle animation by the specified number of milliseconds.
    * @param _time The number of milliseconds elapsed since the last update.
    * @return true if the particle is still 'alive' (requires further animation), false if it has terminated.
    */
   public abstract boolean update(final double _time); 
}
