package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Node;

public interface PlanetaryEffect {
    /**
     * Called once to attach any visuals or listeners to the disc.
     * Should not modify disc’s internal state.
     */
    void attachTo(PlanetaryDisc disc);

    /**
     * Called on any occlusion update (or optionally every frame).
     */
    void update(double occlusionFactor);

    /**
     * Optional visual node this effect contributes.
     * May return null if the effect is logic-only or self-managed.
     */
    Node getNode();
}