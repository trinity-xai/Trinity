package edu.jhuapl.trinity.utils.fun.planetary;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
/**
 *
 * @author Sean Phillips
 */
public class ScanlineEffect implements PlanetaryEffect {
    private final Group lines = new Group();
    private final int count;
    private final Color color;

    public ScanlineEffect(int count, Color color) {
        this.count = count;
        this.color = color;
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        lines.getChildren().clear();
        double spacing = disc.getRadius() * 2 / count;

        for (int i = 0; i < count; i++) {
            Line line = new Line();
            line.setStroke(color);
            line.setStartX(0);
            line.setEndX(disc.getRadius() * 2);
            line.setTranslateY(i * spacing);
            line.setMouseTransparent(true);
            lines.getChildren().add(line);
        }

        lines.layoutXProperty().bind(disc.getPlanetCircle().centerXProperty().subtract(disc.getRadius()));
        lines.layoutYProperty().bind(disc.getPlanetCircle().centerYProperty().subtract(disc.getRadius()));
    }

    @Override
    public void update(double occlusionFactor) {
        lines.setOpacity(occlusionFactor);
    }

    @Override
    public Node getNode() {
        return lines;
    }
}

