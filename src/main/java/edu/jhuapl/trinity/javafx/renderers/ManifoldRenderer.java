package edu.jhuapl.trinity.javafx.renderers;

/*-
 * #%L
 * trinity
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

import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.data.messages.PointCluster;
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

    public void clearAllManifolds();

    public void addManifold(Manifold manifold, Manifold3D manifold3D);

    //a null tolerance means using automatic distancing.
    public void makeManifold(boolean useVisiblePoints, String label, Double tolerance);

    public List<Manifold3D> getAllManifolds();

    public Group getManifoldViews();

    public void findClusters(ProjectionConfig pc);

    public void addClusters(List<PointCluster> clusters);
}
