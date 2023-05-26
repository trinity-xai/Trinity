package edu.jhuapl.trinity.javafx.javafx3d.particle;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

import org.fxyz3d.utils.geom.Vec3f;

public abstract class AgingParticle extends Particle {

    public final Vec3f velocity = new Vec3f();
    public float gravity; // The downward force adjusting the velocity
    public float rotation;
    public float rotationCounter;

    public float[] color = new float[4]; // The Particle's Color
    public float[] colorCounter = new float[4]; // The Color Counter!

    public float alpha; // The Particle's Current Transparency
    public float alphaCounter; // Adds/Subtracts Transparency Over Time

    public float size; // The Particle's Current Size
    public float sizeCounter; // Adds/Subtracts size Over Time

    public float birthAge = -1; //negative means hasn't been born yet
    public float age; // The Particle's Current Age
    public float dyingAge;
    public float ageRateOfChange = 1;

    public AgingParticle() {
        super();
    }

    @Override
    public boolean update(final double _time) {
        Double t = _time;
        //first time through?
        if (birthAge < 0) {
            birthAge = t.floatValue();
        }
        // Increment the age
        age = t.floatValue() - birthAge;
        //if its dead
        if (age > dyingAge) {
            activeProperty.set(false);
            reset();
            return false;
        }

        // Increment alpha and rotation
        size += age * sizeCounter;
        alpha += age * alphaCounter;
        rotation += age * rotationCounter;

        // Adjust for velocity
        location.x = location.getX() + velocity.x;
        location.y = location.getY() + velocity.y;
        location.z = location.getZ() + velocity.z;
        getNode().setTranslateX(location.x);
        getNode().setTranslateY(location.y);
        getNode().setTranslateZ(location.z);

        // Change the velocity.y according to the gravity
        velocity.y += gravity;

        color[0] += ageRateOfChange * colorCounter[0];
        color[1] += ageRateOfChange * colorCounter[1];
        color[2] += ageRateOfChange * colorCounter[2];
        color[3] += ageRateOfChange * colorCounter[3];
        return true;
    }
}
