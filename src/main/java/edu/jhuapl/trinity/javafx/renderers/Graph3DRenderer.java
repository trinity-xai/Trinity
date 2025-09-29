package edu.jhuapl.trinity.javafx.renderers;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedSphere;
import edu.jhuapl.trinity.javafx.javafx3d.animated.Tracer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import org.fxyz3d.geometry.Point3D;

/**
 * Turns a GraphDirectedCollection into a 3D Group (nodes = AnimatedSphere, edges = Tracer).
 */
public final class Graph3DRenderer {

    public static final class Params {
        public double nodeRadius = 20.0;
        public int nodeDivisions = 64;
        public float edgeWidth = 6.0f;
        public double positionScalar = 1.0;
        public Color defaultNodeColor = Color.CYAN;
        public Color defaultEdgeColor = Color.ALICEBLUE;

        public Params withNodeRadius(double r) { nodeRadius = r; return this; }
        public Params withNodeDivisions(int d) { nodeDivisions = d; return this; }
        public Params withEdgeWidth(float w) { edgeWidth = w; return this; }
        public Params withPositionScalar(double s) { positionScalar = s; return this; }
        public Params withDefaultNodeColor(Color c) { if (c != null) defaultNodeColor = c; return this; }
        public Params withDefaultEdgeColor(Color c) { if (c != null) defaultEdgeColor = c; return this; }
    }

    private Graph3DRenderer() {}

    public static Group buildGraphGroup(GraphDirectedCollection graph, Params params) {
        Group root = new Group();
        if (graph == null) return root;
        Params p = (params != null) ? params : new Params();

        Color nodeDefault = (graph.getDefaultNodeColor() != null)
                ? Color.valueOf(graph.getDefaultNodeColor()) : p.defaultNodeColor;
        Color edgeDefault = (graph.getDefaultEdgeColor() != null)
                ? Color.valueOf(graph.getDefaultEdgeColor()) : p.defaultEdgeColor;

        // Nodes
        List<AnimatedSphere> nodes = new ArrayList<>(graph.getNodes().size());
        for (GraphNode gN : graph.getNodes()) {
            Color nodeColor = (gN.getColor() != null) ? Color.valueOf(gN.getColor()) : nodeDefault;
            PhongMaterial mat = new PhongMaterial(nodeColor);
            AnimatedSphere s = new AnimatedSphere(mat, p.nodeRadius, p.nodeDivisions, true);
            Point3D p3 = JavaFX3DUtils.getGraphNodePoint3D(gN, p.positionScalar);
            s.setTranslateX(p3.x); s.setTranslateY(p3.y); s.setTranslateZ(p3.z);
            s.setUserData(gN);
            nodes.add(s);
        }

        // Edges
        List<Tracer> edges = new ArrayList<>(graph.getEdges().size());
        for (GraphEdge ge : graph.getEdges()) {
            Optional<GraphNode> a = graph.findNodeById(ge.getStartID());
            Optional<GraphNode> b = graph.findNodeById(ge.getEndID());
            if (a.isEmpty() || b.isEmpty()) continue;
            Point3D pa = JavaFX3DUtils.getGraphNodePoint3D(a.get(), p.positionScalar);
            Point3D pb = JavaFX3DUtils.getGraphNodePoint3D(b.get(), p.positionScalar);
            Color ec = (ge.getColor() != null) ? Color.valueOf(ge.getColor()) : edgeDefault;
            Tracer t = new Tracer(pa, pb, p.edgeWidth, ec);
            t.setUserData(ge);
            edges.add(t);
        }

        root.getChildren().addAll(nodes);
        root.getChildren().addAll(edges);
        return root;
    }
}
