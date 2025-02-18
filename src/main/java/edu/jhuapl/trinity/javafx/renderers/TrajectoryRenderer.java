/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.renderers;

import edu.jhuapl.trinity.javafx.javafx3d.Trajectory3D;
import javafx.scene.Group;

/**
 * @author Sean Phillips
 */
public interface TrajectoryRenderer {
    public void addTrajectory(Group trajGroup, Group pointGroup, Trajectory3D trajectory3D);

    public void addTrajectoryGroup(Group newTrajGroup, Group newTrajPointGroup);

    //    public Trajectory
    public void clearTrajectories();
}
