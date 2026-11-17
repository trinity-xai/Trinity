package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;
import javafx.util.Duration;

/** Per-node interaction state; does not own or modify shared geometry. */
final class ContextGraphNodeInteraction {
    private final Shape3D node;
    private Material normalMaterial;
    private Material selectedMaterial;
    private boolean selected;
    private boolean hoverEnabled;
    private boolean neighborhoodHighlighted;
    private boolean neighborhoodDimmed;
    private Material neighborhoodHighlightMaterial;
    private Material neighborhoodDimMaterial;
    private double expansionScale = 1.10;
    private double animationTimeMS = 100;
    private ScaleTransition transition;

    ContextGraphNodeInteraction(Shape3D node, Material normalMaterial, Material selectedMaterial,
                                boolean hoverEnabled) {
        this.node = node;
        this.normalMaterial = normalMaterial;
        this.selectedMaterial = selectedMaterial;
        this.hoverEnabled = hoverEnabled;
        refreshMaterial();
        node.setOnMouseEntered(event -> { if (this.hoverEnabled) animate(expansionScale); });
        node.setOnMouseExited(event -> { if (this.hoverEnabled) animate(1.0); });
    }

    void setNormalMaterial(Material material) {
        normalMaterial = material;
        refreshMaterial();
    }

    void setSelectedMaterial(PhongMaterial material) {
        selectedMaterial = material;
        refreshMaterial();
    }

    void select() {
        selected = true;
        refreshMaterial();
    }

    void unselect() {
        selected = false;
        refreshMaterial();
    }

    boolean isSelected() { return selected; }

    void setNeighborhoodState(boolean highlighted, boolean dimmed,
                              Material highlightMaterial, Material dimMaterial) {
        if (neighborhoodHighlighted == highlighted && neighborhoodDimmed == dimmed
            && neighborhoodHighlightMaterial == highlightMaterial
            && neighborhoodDimMaterial == dimMaterial) return;
        neighborhoodHighlighted = highlighted;
        neighborhoodDimmed = dimmed;
        neighborhoodHighlightMaterial = highlightMaterial;
        neighborhoodDimMaterial = dimMaterial;
        refreshMaterial();
    }

    private void refreshMaterial() {
        node.setMaterial(selected ? selectedMaterial
            : neighborhoodHighlighted && neighborhoodHighlightMaterial != null ? neighborhoodHighlightMaterial
            : neighborhoodDimmed && neighborhoodDimMaterial != null ? neighborhoodDimMaterial
            : normalMaterial);
    }
    void setHoverEnabled(boolean enabled) {
        hoverEnabled = enabled;
        if (!enabled) {
            if (transition != null) transition.stop();
            node.setScaleX(1);
            node.setScaleY(1);
            node.setScaleZ(1);
        }
    }
    boolean isHoverEnabled() { return hoverEnabled; }
    void setExpansionScale(double value) {
        if (!Double.isFinite(value) || value < 1.0 || value > 1.10) {
            throw new IllegalArgumentException("Context Graph hover scale must be in [1.0, 1.10]");
        }
        expansionScale = value;
    }
    double getExpansionScale() { return expansionScale; }
    void setAnimationTimeMS(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid duration");
        animationTimeMS = value;
    }
    double getAnimationTimeMS() { return animationTimeMS; }

    private void animate(double target) {
        if (transition != null) transition.stop();
        transition = new ScaleTransition(Duration.millis(animationTimeMS), node);
        transition.setToX(target);
        transition.setToY(target);
        transition.setToZ(target);
        transition.setInterpolator(Interpolator.EASE_OUT);
        transition.play();
    }
}
