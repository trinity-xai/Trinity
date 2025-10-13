package edu.jhuapl.trinity.javafx.javafx3d.animated;

import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.TriangleMesh;
import javafx.util.Duration;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.composites.PolyLine3D;

import java.util.ArrayList;
import java.util.List;

import static javafx.animation.Animation.INDEFINITE;

/**
 * @author Sean Phillips
 */
public class Tracer extends PolyLine3D {
    public double DEFAULT_RATEOFCHANGE = 0.01;
    private double rateOfChange = DEFAULT_RATEOFCHANGE;
    public IntegerProperty keyCycle = new SimpleIntegerProperty();
    Timeline tm;
    TriangleMesh mesh;
    float[] uvCoords = {
        0f, 0f,
        0.25f, 0.5f,
        0.5f, 0f,
        0.5f, 1f,
        0.75f, 0.5f,
        1f, 0f
    };
    boolean animating = false;
    Point3D currentStart;
    Point3D currentEnd;
    float currentWidth;
    Color diffuseColor;
    float[] originalUVCoords = {
        0.25f, 0.5f, 0.9f, 0.5f,


        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,

        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,

        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,
        0.25f, 0.5f, 0.9f, 0.5f,

        0.25f, 0.5f, 0.9f, 0.5f,

    };

    public Tracer(Point3D start, Point3D end, float width, Color color) {
        super(makePointList(start, end), width, color, LineType.TRIANGLE);
        currentStart = start;
        currentEnd = end;
        currentWidth = width;
        diffuseColor = color;
        mesh = (TriangleMesh) this.meshView.getMesh();
        mesh.getTexCoords().setAll(originalUVCoords);
//        meshView.setCullFace(CullFace.NONE);
        setCycle(20, 30);

        keyCycle.addListener(e -> {
            float add = keyCycle.getValue() / 30000f;
            //i=0;i+=2 is right to left
            //i=1;i+=2 is bottom to top
            for (int i = 1; i < uvCoords.length; i += 2) {
                uvCoords[i] -= rateOfChange;
            }
            mesh.getTexCoords().set(0, uvCoords, 0, uvCoords.length);
        });
//        setOnScroll(e -> {
//            setRateOfChange(rateOfChange - e.getDeltaY() / 10000f);
//        });
//        setOnMouseClicked(e -> {
//            if (e.isSecondaryButtonDown()) {
//                setRateOfChange(DEFAULT_RATEOFCHANGE);
//            }
//            if (e.getClickCount() > 1) {
//                enableCycle(!animating);
//            }
//        });
//        animating = true;
//        enableCycle(animating);
    }

    public static List<Point3D> makePointList(Point3D start, Point3D end) {
        List<Point3D> points = new ArrayList<>(2);
        points.add(start);
        Point3D midPoint = JavaFX3DUtils.toFXYZ3D.apply(
            JavaFX3DUtils.toFX.apply(start)
                .midpoint(JavaFX3DUtils.toFX.apply(end)));
        points.add(midPoint);
        points.add(end);
        return points;
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

    /**
     * Update the diffuse (base) color of this edge's material at runtime, without rebuilding the mesh.
     * If the current material is not a PhongMaterial, a new PhongMaterial is attached.
     *
     * @param color new diffuse color (ignored if null)
     */
    public void setDiffuseColor(Color color) {
        if (color == null) return;
        this.diffuseColor = color;
        if (this.meshView != null) {
            Material m = this.meshView.getMaterial();
            if (m instanceof PhongMaterial pm) {
                pm.setDiffuseColor(color);
            } else {
                this.meshView.setMaterial(new PhongMaterial(color));
            }
        }
    }

    public void setOpacityAlpha(double alpha) {
        double a = Math.max(0.0, Math.min(1.0, alpha));
        if (this.meshView != null) {
            javafx.scene.paint.Material m = this.meshView.getMaterial();
            Color base;
            if (m instanceof PhongMaterial pm) {
                base = pm.getDiffuseColor();
                if (base == null) base = (diffuseColor != null) ? diffuseColor : Color.WHITE;
                pm.setDiffuseColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), a));
                // Optional: keep specular alpha aligned
                Color spec = pm.getSpecularColor();
                if (spec != null) {
                    pm.setSpecularColor(new Color(spec.getRed(), spec.getGreen(), spec.getBlue(), a));
                }
            } else {
                base = (diffuseColor != null) ? diffuseColor : Color.WHITE;
                this.meshView.setMaterial(new PhongMaterial(new Color(base.getRed(), base.getGreen(), base.getBlue(), a)));
            }
        }
    }
}
