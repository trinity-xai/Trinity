package edu.jhuapl.trinity.javafx.renderers;

import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.data.messages.xai.PointCluster;
import edu.jhuapl.trinity.data.messages.xai.UmapConfig;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent.ProjectionConfig;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import javafx.scene.Group;
import org.fxyz3d.geometry.Point3D;

import java.util.List;

/**
 * @author Sean Phillips
 */
public interface ManifoldRenderer {

    public Point3D projectVector(FeatureVector featureVector);

    public void transformFeatureVector(FeatureVector featureVector);
    
    public void clearAllManifolds();

    public void addManifold(Manifold manifold, Manifold3D manifold3D);

    //a null tolerance means using automatic distancing.
    public void makeManifold(boolean useVisiblePoints, String label, Double tolerance);

    public List<Manifold3D> getAllManifolds();

    public Group getManifoldViews();

    public void findClusters(ProjectionConfig pc);

    public void addClusters(List<PointCluster> clusters);

    public void setUmapConfig(UmapConfig config);
}
