/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.scene.shape.Sphere;

/**
 * @author Sean Phillips
 */
public class TrajectoryState extends Sphere {
    public int dayNumber;
    public int trialNumber;

    public TrajectoryState(double radius, int dayNumber, int trialNumber) {
        super(radius);
        this.dayNumber = dayNumber;
        this.trialNumber = trialNumber;
    }
}
