package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.fxyz3d.geometry.Point3D;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * @author Sean Phillips
 */
public class PointListItem extends HBox {
    public static double PREF_LABEL_WIDTH = 100;
    private CheckBox includeCheckBox;
    private Label label;
    private Manifold manifold;
    private Point3D point3D;
    /**
     * Format for floating coordinate label
     */
    private NumberFormat format = new DecimalFormat("0.00000");

    public PointListItem(Manifold manifold, Point3D point3D, boolean included) {
        this.manifold = manifold;
        this.point3D = point3D;
        label = new Label(formattedString());
//        label.setPrefWidth(PREF_LABEL_WIDTH);
        includeCheckBox = new CheckBox("");
        includeCheckBox.setSelected(included);

        getChildren().addAll(includeCheckBox, label);
        setSpacing(5);
        includeCheckBox.selectedProperty().addListener(cl -> {
            if (null != includeCheckBox.getScene()) {
                getScene().getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.TOGGLE_HULL_POINT,
                    this.manifold, point3D));
            }
        });
    }

    private String formattedString() {
        return format.format(getPoint3D().getX()) + ", "
            + format.format(getPoint3D().getY()) + ", "
            + format.format(getPoint3D().getZ());
    }

    public boolean isSelected() {
        return includeCheckBox.isSelected();
    }

    /**
     * @return the point3D
     */
    public Point3D getPoint3D() {
        return point3D;
    }

    /**
     * @param point3D the point3D to set
     */
    public void setPoint3D(Point3D point3D) {
        this.point3D = point3D;
    }
}
