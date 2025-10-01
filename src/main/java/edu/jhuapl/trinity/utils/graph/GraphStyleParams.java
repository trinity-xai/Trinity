package edu.jhuapl.trinity.utils.graph;

import java.io.Serial;
import java.io.Serializable;
import javafx.scene.paint.Color;

/**
 * Visual styling parameters for 3D graph rendering.
 */
public final class GraphStyleParams implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

// Nodes
    public Color nodeColor = Color.CYAN; // default aligned with Graph3DRenderer.Params
    public double nodeRadius = 20.0;
    public double nodeOpacity = 1.0; // [0..1]

// Edges
    public Color edgeColor = Color.ALICEBLUE; // default aligned with Graph3DRenderer.Params
    public double edgeWidth = 8.0; // rendered as float in Tracer
    public double edgeOpacity = 1.0; // [0..1]

// Fluent setters
    public GraphStyleParams withNodeColor(Color c) {
        this.nodeColor = c;
        return this;
    }

    public GraphStyleParams withNodeRadius(double r) {
        this.nodeRadius = r;
        return this;
    }

    public GraphStyleParams withNodeOpacity(double o) {
        this.nodeOpacity = o;
        return this;
    }

    public GraphStyleParams withEdgeColor(Color c) {
        this.edgeColor = c;
        return this;
    }

    public GraphStyleParams withEdgeWidth(double w) {
        this.edgeWidth = w;
        return this;
    }

    public GraphStyleParams withEdgeOpacity(double o) {
        this.edgeOpacity = o;
        return this;
    }

// Shallow copy helpers
    public static GraphStyleParams copyOf(GraphStyleParams src) {
        GraphStyleParams p = new GraphStyleParams();
        if (src == null) {
            return p;
        }
        p.nodeColor = src.nodeColor;
        p.nodeRadius = src.nodeRadius;
        p.nodeOpacity = src.nodeOpacity;
        p.edgeColor = src.edgeColor;
        p.edgeWidth = src.edgeWidth;
        p.edgeOpacity = src.edgeOpacity;
        return p;
    }
}
