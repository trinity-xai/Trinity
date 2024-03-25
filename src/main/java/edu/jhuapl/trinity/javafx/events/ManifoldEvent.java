package edu.jhuapl.trinity.javafx.events;

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

import edu.jhuapl.trinity.utils.clustering.ClusterMethod;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class ManifoldEvent extends Event {

    public Object object1 = null;
    public Object object2 = null;
    public Object object3 = null;

    public static enum POINT_SOURCE {HYPERSPACE, HYPERSURFACE}
    public static class ProjectionConfig {
        public static enum COVARIANCE_MODE {
            DIAGONAL, FULL
        };        
        public boolean useVisiblePoints = true;
        public ClusterMethod clusterMethod = ClusterMethod.EX_MAX;
        public int components = 5;
        public int maxIterations = 100;
        public double tolerance = 1e4;
        public COVARIANCE_MODE covariance = COVARIANCE_MODE.DIAGONAL;
    }
    //Cluster selection events
    public static final EventType<ManifoldEvent> CLUSTER_SELECTION_MODE = new EventType(ANY, "CLUSTER_SELECTION_MODE");
    public static final EventType<ManifoldEvent> ADD_CLUSTER_SELECTION = new EventType(ANY, "ADD_CLUSTER_SELECTION");
    public static final EventType<ManifoldEvent> NEW_MANIFOLD_CLUSTER = new EventType(ANY, "NEW_MANIFOLD_CLUSTER");
            
    //Distance stuff
    public static final EventType<ManifoldEvent> DISTANCE_CONNECTOR_SELECTED = new EventType(ANY, "DISTANCE_CONNECTOR_SELECTED");
    public static final EventType<ManifoldEvent> DISTANCE_CONNECTOR_WIDTH = new EventType(ANY, "DISTANCE_CONNECTOR_WIDTH");
    public static final EventType<ManifoldEvent> DISTANCE_CONNECTOR_COLOR = new EventType(ANY, "DISTANCE_CONNECTOR_COLOR");
    public static final EventType<ManifoldEvent> DISTANCE_OBJECT_SELECTED = new EventType(ANY, "DISTANCE_OBJECT_SELECTED");
    public static final EventType<ManifoldEvent> DISTANCE_MODE_POINTPOINT = new EventType(ANY, "DISTANCE_MODE_POINTWISE");
    public static final EventType<ManifoldEvent> DISTANCE_MODE_POINTGROUP = new EventType(ANY, "DISTANCE_MODE_POINTGROUP");
    public static final EventType<ManifoldEvent> SELECT_DISTANCE_POINT1 = new EventType(ANY, "SELECT_DISTANCE_POINT1");
    public static final EventType<ManifoldEvent> SELECT_DISTANCE_POINT2 = new EventType(ANY, "SELECT_DISTANCE_POINT2");
    public static final EventType<ManifoldEvent> CREATE_NEW_DISTANCE = new EventType(ANY, "CREATE_NEW_DISTANCE");
    public static final EventType<ManifoldEvent> CLEAR_DISTANCE_CONNECTORS = new EventType(ANY, "CLEAR_DISTANCE_CONNECTORS");
    //PCA
    public static final EventType<ManifoldEvent> GENERATE_NEW_PCA = new EventType(ANY, "GENERATE_NEW_PCA");

    //UMAP
    public static final EventType<ManifoldEvent> SAVE_PROJECTION_DATA = new EventType(ANY, "SAVE_PROJECTION_DATA");
    public static final EventType<ManifoldEvent> GENERATE_NEW_UMAP = new EventType(ANY, "GENERATE_NEW_UMAP");
    public static final EventType<ManifoldEvent> USE_AUTOMATIC_TOLERANCE = new EventType(ANY, "USE_AUTOMATIC_TOLERANCE");
    public static final EventType<ManifoldEvent> SET_DISTANCE_TOLERANCE = new EventType(ANY, "SET_DISTANCE_TOLERANCE");
    public static final EventType<ManifoldEvent> GENERATE_HYPERSPACE_MANIFOLD = new EventType(ANY, "GENERATE_HYPERSPACE_MANIFOLD");
    public static final EventType<ManifoldEvent> GENERATE_PROJECTION_MANIFOLD = new EventType(ANY, "GENERATE_PROJECTION_MANIFOLD");
    public static final EventType<ManifoldEvent> NEW_PROJECTION_VECTOR = new EventType(ANY, "NEW_PROJECTION_VECTOR");
        
    //Clustering
    public static final EventType<ManifoldEvent> FIND_PROJECTION_CLUSTERS = new EventType(ANY, "FIND_PROJECTION_CLUSTERS");
    public static final EventType<ManifoldEvent> NEW_CLUSTER_COLLECTION = new EventType(ANY, "NEW_CLUSTER_COLLECTION");

    //Manifold Geometry
    public static final EventType<ManifoldEvent> NEW_MANIFOLD_DATA = new EventType(ANY, "NEW_MANIFOLD_DATA");
    public static final EventType<ManifoldEvent> EXPORT_MANIFOLD_DATA = new EventType(ANY, "EXPORT_MANIFOLD_DATA");
    public static final EventType<ManifoldEvent> SET_USED_HULLPOINTS = new EventType(ANY, "SET_USED_HULLPOINTS");
    public static final EventType<ManifoldEvent> TOGGLE_HULL_POINT = new EventType(ANY, "TOGGLE_HULL_POINT");
    public static final EventType<ManifoldEvent> SELECT_PROJECTION_POINT3D = new EventType(ANY, "SELECT_PROJECTION_POINT3D");
    public static final EventType<ManifoldEvent> SELECT_SHAPE3D_POINT3D = new EventType(ANY, "SELECT_SHAPE3D_POINT3D");
    public static final EventType<ManifoldEvent> MANIFOLD3D_OBJECT_GENERATED = new EventType(ANY, "MANIFOLD3D_OBJECT_GENERATED");
    public static final EventType<ManifoldEvent> MANIFOLD_OBJECT_SELECTED = new EventType(ANY, "MANIFOLD_OBJECT_SELECTED");
    public static final EventType<ManifoldEvent> MANIFOLD_3D_SELECTED = new EventType(ANY, "MANIFOLD_3D_SELECTED");
    public static final EventType<ManifoldEvent> MANIFOLD_3D_VISIBLE = new EventType(ANY, "MANIFOLD_3D_VISIBLE");
    public static final EventType<ManifoldEvent> CLEAR_ALL_MANIFOLDS = new EventType(ANY, "CLEAR_ALL_MANIFOLDS");
    public static final EventType<ManifoldEvent> USE_VISIBLE_POINTS = new EventType(ANY, "USE_VISIBLE_POINTS");
    public static final EventType<ManifoldEvent> USE_ALL_POINTS = new EventType(ANY, "USE_ALL_POINTS");
    public static final EventType<ManifoldEvent> MANIFOLD_SET_SCALE = new EventType(ANY, "MANIFOLD_SET_SCALE");
    public static final EventType<ManifoldEvent> MANIFOLD_SET_YAWPITCHROLL = new EventType(ANY, "MANIFOLD_SET_YAWPITCHROLL");
    public static final EventType<ManifoldEvent> MANIFOLD_ROTATE_X = new EventType(ANY, "MANIFOLD_ROTATE_X");
    public static final EventType<ManifoldEvent> MANIFOLD_ROTATE_Y = new EventType(ANY, "MANIFOLD_ROTATE_Y");
    public static final EventType<ManifoldEvent> MANIFOLD_ROTATE_Z = new EventType(ANY, "MANIFOLD_ROTATE_Z");
    public static final EventType<ManifoldEvent> MANIFOLD_DIFFUSE_COLOR = new EventType(ANY, "MANIFOLD_DIFFUSE_COLOR");
    public static final EventType<ManifoldEvent> MANIFOLD_SPECULAR_COLOR = new EventType(ANY, "MANIFOLD_SPECULAR_COLOR");
    public static final EventType<ManifoldEvent> MANIFOLD_WIREFRAME_COLOR = new EventType(ANY, "MANIFOLD_WIREFRAME_COLOR");
    public static final EventType<ManifoldEvent> MANIFOLD_FRONT_CULLFACE = new EventType(ANY, "MANIFOLD_FRONT_CULLFACE");
    public static final EventType<ManifoldEvent> MANIFOLD_BACK_CULLFACE = new EventType(ANY, "MANIFOLD_BACK_CULLFACE");
    public static final EventType<ManifoldEvent> MANIFOLD_NONE_CULLFACE = new EventType(ANY, "MANIFOLD_NONE_CULLFACE");
    public static final EventType<ManifoldEvent> MANIFOLD_FILL_DRAWMODE = new EventType(ANY, "MANIFOLD_FILL_DRAWMODE");
    public static final EventType<ManifoldEvent> MANIFOLD_LINE_DRAWMODE = new EventType(ANY, "MANIFOLD_LINE_DRAWMODE");
    public static final EventType<ManifoldEvent> MANIFOLD_SHOW_WIREFRAME = new EventType(ANY, "MANIFOLD_SHOW_WIREFRAME");
    public static final EventType<ManifoldEvent> MANIFOLD_SHOW_CONTROL = new EventType(ANY, "MANIFOLD_SHOW_CONTROL");

    public ManifoldEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public ManifoldEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object1 = arg1;
    }

    public ManifoldEvent(EventType<? extends Event> arg0, Object arg1, Object arg2) {
        this(arg0);
        object1 = arg1;
        object2 = arg2;
    }
    //Hello... is it this hack you're looking for...?
    public ManifoldEvent(EventType<? extends Event> arg0, Object arg1, Object arg2, Object arg3) {
        this(arg0);
        object1 = arg1;
        object2 = arg2;
        object3 = arg3;
    }

    public ManifoldEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object1 = arg0;
    }
}
