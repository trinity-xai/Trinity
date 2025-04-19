package edu.jhuapl.trinity.javafx.javafx3d.animated;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.util.Duration;

import static javafx.animation.Animation.INDEFINITE;

/**
 * @author Sean Phillips
 */
public class AnimatedBox extends MeshView {
    public double DEFAULT_RATEOFCHANGE = 0.01;
    private double rateOfChange = DEFAULT_RATEOFCHANGE;
    public TriangleMesh triangleMesh;
    public IntegerProperty keyCycle = new SimpleIntegerProperty();
    Timeline tm;
    float[] uvCoords = {0, 0, 1, 0, 1, 1, 0, 1};
    boolean animating = false;

    public AnimatedBox(float width, float height, float depth) {
        super();
        triangleMesh = createMesh(width, height, depth);
        this.setMesh(triangleMesh);
        setCycle(20, 30);
        keyCycle.addListener(e -> {
            float add = keyCycle.getValue() / 30000f;
            //i=0;i+=2 is right to left
            //i=1;i+=2 is bottom to top
            for (int i = 1; i < uvCoords.length; i += 2) {
                uvCoords[i] -= rateOfChange;
            }
            triangleMesh.getTexCoords().set(0, uvCoords, 0, uvCoords.length);
        });
        setOnScroll(e -> {
            setRateOfChange(rateOfChange - e.getDeltaY() / 10000f);
        });
        setOnMouseClicked(e -> {
            if (e.isSecondaryButtonDown()) {
                setRateOfChange(DEFAULT_RATEOFCHANGE);
            }
            if (e.getClickCount() > 1) {
                enableCycle(!animating);
            }
        });
    }

    public void setCycle(double cycleSeconds, double fps) {
        KeyValue start = new KeyValue(keyCycle, 0, Interpolator.LINEAR);
        KeyValue end = new KeyValue(keyCycle, fps * cycleSeconds, Interpolator.LINEAR);
        KeyFrame kf = new KeyFrame(Duration.seconds(cycleSeconds), start, end);
//        KeyFrame cycleFinished = new KeyFrame(Duration.seconds(cycleSeconds), e->{
//
//        });
        tm = new Timeline(kf);
        tm.setCycleCount(INDEFINITE);
    }

    public void enableCycle(boolean enable) {
        if (enable)
            tm.play();
        else
            tm.stop();
    }

    /**
     * Basically the the code from JavaFX's 3D Box
     *
     * @param w
     * @param h
     * @param d
     * @return
     */
    private TriangleMesh createMesh(float w, float h, float d) {
        // NOTE: still create mesh for degenerated box
        float hw = w / 2f;
        float hh = h / 2f;
        float hd = d / 2f;

        float points[] = {
            -hw, -hh, -hd,
            hw, -hh, -hd,
            hw, hh, -hd,
            -hw, hh, -hd,
            -hw, -hh, hd,
            hw, -hh, hd,
            hw, hh, hd,
            -hw, hh, hd};

        float texCoords[] = {0, 0, 1, 0, 1, 1, 0, 1};

        // Specifies hard edges.
        int faceSmoothingGroups[] = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        int faces[] = {
            0, 0, 2, 2, 1, 1,
            2, 2, 0, 0, 3, 3,
            1, 0, 6, 2, 5, 1,
            6, 2, 1, 0, 2, 3,
            5, 0, 7, 2, 4, 1,
            7, 2, 5, 0, 6, 3,
            4, 0, 3, 2, 0, 1,
            3, 2, 4, 0, 7, 3,
            3, 0, 6, 2, 2, 1,
            6, 2, 3, 0, 7, 3,
            4, 0, 1, 2, 5, 1,
            1, 2, 4, 0, 0, 3,
        };

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);
        mesh.getFaceSmoothingGroups().setAll(faceSmoothingGroups);

        return mesh;
    }

    /**
     * @return the rateOfChange
     */
    public synchronized double getRateOfChange() {
        return rateOfChange;
    }

    /**
     * @param rateOfChange the rateOfChange to set
     */
    public synchronized void setRateOfChange(double rateOfChange) {
        this.rateOfChange = rateOfChange;
    }
}
