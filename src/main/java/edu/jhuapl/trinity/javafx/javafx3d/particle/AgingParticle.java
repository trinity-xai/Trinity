/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.javafx3d.particle;

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
