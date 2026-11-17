package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;

/**
 * Projection-sensitive spatial reference group, independent of graph entities.
 * Attach to the stable world/scene-reference layer, never the graph-data layer.
 *
 * The initial state deliberately shows a Cartesian grid even with no graph data.
 * The caller MUST derive setHyperbolic(...) from the active projection mode and
 * setHyperbolicRadius(...) from the layout's actual rendered ball radius.
 */
public final class ContextGraphReferenceGrid3D extends Group {
    private final Grid3D cartesianGrid = new Grid3D();
    private final PoincareGrid3D poincareGrid = new PoincareGrid3D();
    private final BooleanProperty hyperbolic = new SimpleBooleanProperty(this, "hyperbolic", false);
    private final BooleanProperty autoFitCartesian = new SimpleBooleanProperty(this, "autoFitCartesian", true);
    private ContextGraphCartesianGridBounds currentBounds = ContextGraphCartesianGridBounds.empty();

    public ContextGraphReferenceGrid3D() {
        setMouseTransparent(true);
        cartesianGrid.setMouseTransparent(true);
        poincareGrid.setMouseTransparent(true);
        cartesianGrid.setWidth(200);
        cartesianGrid.setLength(200);
        cartesianGrid.setXDivs(20);
        cartesianGrid.setZDivs(20);
        getChildren().addAll(cartesianGrid, poincareGrid);
        cartesianGrid.setLineColor(javafx.scene.paint.Color.color(0.45, 0.58, 0.92, 0.24));
        cartesianGrid.setMajorLineColor(javafx.scene.paint.Color.color(0.65, 0.78, 1.0, 0.48));
        hyperbolic.addListener((obs, oldValue, newValue) -> refreshProjection());
        autoFitCartesian.addListener((obs, oldValue, enabled) -> {
            if (enabled) applyCartesianBounds(currentBounds);
        });
        refreshProjection();
    }

    public void setHyperbolic(boolean enabled) {
        hyperbolic.set(enabled);
    }

    private void refreshProjection() {
        cartesianGrid.setVisible(!hyperbolic.get());
        poincareGrid.setVisible(hyperbolic.get());
    }

    public boolean isHyperbolic() { return hyperbolic.get(); }
    public BooleanProperty hyperbolicProperty() { return hyperbolic; }

    public void setHyperbolicRadius(double ballRadius) {
        poincareGrid.setBallRadius(ballRadius);
    }

    public boolean isAutoFitCartesian() { return autoFitCartesian.get(); }
    public void setAutoFitCartesian(boolean enabled) { autoFitCartesian.set(enabled); }
    public BooleanProperty autoFitCartesianProperty() { return autoFitCartesian; }

    /** Cache the last source-data bounds even while manual sizing is active. */
    public void updateCartesianBounds(ContextGraphCartesianGridBounds bounds) {
        if (bounds == null || bounds.equals(currentBounds)) return;
        currentBounds = bounds;
        if (autoFitCartesian.get()) applyCartesianBounds(bounds);
    }

    private void applyCartesianBounds(ContextGraphCartesianGridBounds bounds) {
        cartesianGrid.setTranslateX(bounds.centerX());
        cartesianGrid.setTranslateY(bounds.floorY());
        cartesianGrid.setTranslateZ(bounds.centerZ());
        if (cartesianGrid.getWidth() != bounds.width()) cartesianGrid.setWidth(bounds.width());
        if (cartesianGrid.getLength() != bounds.length()) cartesianGrid.setLength(bounds.length());
    }

    public void setCartesianWidth(double width) {
        cartesianGrid.setWidth(width);
    }

    public void setCartesianLength(double length) {
        cartesianGrid.setLength(length);
    }

    public Grid3D getCartesianGrid() { return cartesianGrid; }
    public PoincareGrid3D getPoincareGrid() { return poincareGrid; }
}
