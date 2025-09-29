package edu.jhuapl.trinity.javafx.javafx3d.tasks;

import com.github.trinity.supermds.SuperMDS;
import com.github.trinity.supermds.SuperMDS.Params;
import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.utils.graph.GraphLayoutParams;
import edu.jhuapl.trinity.utils.graph.MatrixToGraphAdapter;
import edu.jhuapl.trinity.utils.graph.SuperMdsEmbedding3D;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static edu.jhuapl.trinity.utils.Utils.totalTimeString;

/**
 * BuildGraphFromMatrixTask
 * ------------------------
 * Background task that converts a SIMILARITY or DIVERGENCE matrix into a GraphDirectedCollection,
 * laying out nodes using either:
 *   - MDS_3D (via SuperMDS),
 *   - FORCE_FR (Fruchterman–Reingold 3D),
 *   - or a static layout (CIRCLE_XZ/CIRCLE_XY/SPHERE).
 *
 * Emits GraphEvent.NEW_GRAPHDIRECTED_COLLECTION on success.
 *
 * @author Sean Phillips
 */
public class BuildGraphFromMatrixTask extends Task<GraphDirectedCollection> {
    private static final Logger LOG = LoggerFactory.getLogger(BuildGraphFromMatrixTask.class);

    private final Scene scene;
    private final double[][] matrix;                   // NxN similarity or divergence
    private final List<String> labels;                 // size N
    private final MatrixToGraphAdapter.MatrixKind kind;
    private final MatrixToGraphAdapter.WeightMode weightMode;
    private final GraphLayoutParams layoutParams;

    // Optional: SuperMDS params if using MDS_3D
    private final SuperMDS.Params mdsParams;

    public BuildGraphFromMatrixTask(Scene scene,
                                    double[][] matrix,
                                    List<String> labels,
                                    MatrixToGraphAdapter.MatrixKind kind,
                                    MatrixToGraphAdapter.WeightMode weightMode,
                                    GraphLayoutParams layoutParams,
                                    SuperMDS.Params mdsParams) {
        this.scene = scene;
        this.matrix = matrix;
        this.labels = labels;
        this.kind = kind;
        this.weightMode = weightMode;
        this.layoutParams = layoutParams;
        this.mdsParams = mdsParams;

        setOnSucceeded(e -> Platform.runLater(() ->
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR))
        ));
        setOnFailed(e -> Platform.runLater(() ->
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR))
        ));
        setOnCancelled(e -> Platform.runLater(() ->
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR))
        ));
    }

    @Override
    protected GraphDirectedCollection call() throws Exception {
        if (isCancelled()) return null;
        showBusy("Preparing graph layout...");

        long start = System.nanoTime();
        Thread.sleep(Duration.ofMillis(150)); // small pacing for UX parity with other tasks

        // Select an MDS provider if the chosen layout is MDS_3D; null otherwise.
        MatrixToGraphAdapter.MdsEmbedding3D mds3d = null;
        if (layoutParams.kind == GraphLayoutParams.LayoutKind.MDS_3D) {
            SuperMDS.Params p = (mdsParams != null) ? mdsParams : defaultMdsParams();
            if (p.outputDim != 3) p.outputDim = 3; // ensure 3D embedding
            mds3d = new SuperMdsEmbedding3D(p);
        }

        updateMessage("Building graph from matrix...");
        GraphDirectedCollection gc = MatrixToGraphAdapter.build(
            matrix,
            labels,
            kind,
            layoutParams,
            weightMode,
            mds3d
        );

        String elapsed = totalTimeString(start);
        LOG.info("Graph build complete. {}", elapsed);
        postConsole(elapsed);

        // Publish to the app (same event pattern used in your GraphDirectedTest)
        Platform.runLater(() ->
            scene.getRoot().fireEvent(new GraphEvent(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, gc))
        );

        return gc;
    }

    // --- helpers to keep parity with your busy indicator + console pattern ---

    private void showBusy(String msg) {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus(msg, 0.5);
            ps.fillStartColor = Color.AQUA;
            ps.fillEndColor = Color.DODGERBLUE;
            ps.innerStrokeColor = Color.AQUA;
            ps.outerStrokeColor = Color.DODGERBLUE;
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_TEXT_CONSOLE, msg, true));
        });
    }

    private void postConsole(String msg) {
        Platform.runLater(() ->
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_TEXT_CONSOLE, msg, true))
        );
    }

    private static Params defaultMdsParams() {
        Params p = new Params();
        // Sensible defaults; feel free to override when constructing the task.
        // e.g., p.mode = SuperMDS.Mode.SMACOF;
        p.outputDim = 3;
        return p;
    }
}
