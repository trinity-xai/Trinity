/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.javafx3d.tasks;

import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import org.fxyz3d.geometry.Point3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Sean Phillips
 */
public abstract class ClusterTask extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterTask.class);
    static AtomicInteger ai = new AtomicInteger(0);
    Scene scene;
    PerspectiveCamera camera;
    private boolean cancelledByUser = false;
    boolean filterByLabel = false;
    String filterLabel = "";
    //used for sizing values to 3D scene later but most do.
    private Color diffuseColor = Color.CYAN;
    private double projectionScalar = 100.0; //used for sizing values to 3D scene later

    public ClusterTask(Scene scene, PerspectiveCamera camera) {
        this.scene = scene;
        this.camera = camera;

        setOnSucceeded(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        setOnFailed(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        setOnCancelled(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });

    }

    protected abstract void processTask() throws Exception;

    @Override
    protected Void call() throws Exception {
        if (isCancelled()) return null;
        processTask();
        return null;
    }

    protected void createManifold(List<Point3D> points, String label) {
        ArrayList<javafx.geometry.Point3D> fxPoints = points.stream()
            .map(p3D -> new javafx.geometry.Point3D(p3D.x, p3D.y, p3D.z))
            .collect(Collectors.toCollection(ArrayList::new));
        Manifold manifold = new Manifold(fxPoints, label, label, getDiffuseColor());
        //Create the 3D manifold shape
        Manifold3D manifold3D = new Manifold3D(points,
            true, true, true, null
        );
        manifold3D.quickhullMeshView.setCullFace(CullFace.FRONT);
        manifold3D.setManifold(manifold);
        ((PhongMaterial) manifold3D.quickhullMeshView.getMaterial()).setDiffuseColor(getDiffuseColor());

        manifold3D.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            scene.getRoot().fireEvent(
                new ManifoldEvent(ManifoldEvent.MANIFOLD_3D_SELECTED, manifold));
        });
        //Add this Manifold data object to the global tracker
        Manifold.addManifold(manifold);
        //update the manifold to manifold3D mapping
        Manifold.globalManifoldToManifold3DMap.put(manifold, manifold3D);
        //announce to the world of the new manifold and its shape
        //System.out.println("Manifold3D generation complete for " + label);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.NEW_MANIFOLD_CLUSTER, manifold, manifold3D));
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD3D_OBJECT_GENERATED, manifold, manifold3D));
        });
    }

    protected void convertToManifoldGeometry(double[][] observations,
                                             int[] labels, int clusters, String prefix) {
        for (int clusterIndex = 0; clusterIndex < clusters; clusterIndex++) {
            String label = prefix + clusterIndex;
            List<Point3D> points = new ArrayList<>();
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] == clusterIndex) {
                    points.add(new Point3D(
                        observations[i][0] * getProjectionScalar(),
                        observations[i][1] * -getProjectionScalar(),
                        observations[i][2] * getProjectionScalar())
                    );
                }
            }
            if (points.size() >= 4) {
                createManifold(points, label);
            } else {
                LOG.info("Cluster has less than 4 points");
            }
        }
    }

    /**
     * @return the cancelledByUser
     */
    public boolean isCancelledByUser() {
        return cancelledByUser;
    }

    /**
     * @param cancelledByUser the cancelledByUser to set
     */
    public void setCancelledByUser(boolean cancelledByUser) {
        this.cancelledByUser = cancelledByUser;
    }

    /**
     * @return the projectionScalar
     */
    public double getProjectionScalar() {
        return projectionScalar;
    }

    /**
     * @param projectionScalar the projectionScalar to set
     */
    public void setProjectionScalar(double projectionScalar) {
        this.projectionScalar = projectionScalar;
    }

    /**
     * @return the diffuseColor
     */
    public Color getDiffuseColor() {
        return diffuseColor;
    }

    /**
     * @param diffuseColor the diffuseColor to set
     */
    public void setDiffuseColor(Color diffuseColor) {
        this.diffuseColor = diffuseColor;
    }
}
