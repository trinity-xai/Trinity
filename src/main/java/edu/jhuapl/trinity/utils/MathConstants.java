package edu.jhuapl.trinity.utils;

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

/**
 *
 * @author phillsm1
 */
public class MathConstants {

   /**
    * This defines the number of meters in one arc second for the flat earth
    * model.
    **/
   public static final double METERS_PER_SEC          = 30.86666666;

   /**
    * Math.PI / 2.
    */
   public static final double HALF_PI                 = Math.PI / 2.;
   /**
    * Math.PI / 4.
    */
   public static final double QUARTER_PI              = Math.PI / 4.;

   /**
    * Math.PI * 2.
    */
   public static final double DOUBLE_PI               = Math.PI * 2.;

   /**
    * Math.PI / 180.
    */
   public static final double DEG_TO_RADS             = Math.PI / 180.;

   /**
    * 180. / Math.PI
    */
   public static final double RADS_TO_DEG             = 180. / Math.PI;

   /**
    * ( Math.PI / 180 ) / 2.
    */
   public static final double HALF_DEGREES_TO_RADIANS = MathConstants.DEG_TO_RADS / 2.;

   /**
    * Small floating point value (in single-precision range).
    */
   public static final double SMALL_VALUE             = 1e-15;

   /**
    * Meters to Feet.
    */
   public static final double METERS_TO_FEET          = 3.28083989501;

   /**
    * Feet to Meters.
    */
   public static final double FEET_TO_METERS          = 1 / MathConstants.METERS_TO_FEET;
   

   /** Converts miles into meters. */
   public static final double                         MILE_TO_METERS          = 1609.34;   

   /** Converts meters into miles. */
   public static final double                         METER_TO_MILES          = 0.000621371;   

   /** Converts meters into kilometers. */
   public static final double                         METER_TO_KILOMETERS     = 0.001;   

   /** Converts kilometers into meters. */
   public static final double                         KILOMETER_TO_METER      = 1000;

}
