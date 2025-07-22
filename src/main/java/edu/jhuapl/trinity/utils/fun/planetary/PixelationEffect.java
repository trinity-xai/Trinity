package edu.jhuapl.trinity.utils.fun.planetary;

import edu.jhuapl.trinity.utils.fun.Pixelate;
import edu.jhuapl.trinity.utils.fun.Pixelate.PixelationMode;
import javafx.scene.Group;
import javafx.scene.Node;

/**
 * @author Sean Phillips
 */
public class PixelationEffect implements PlanetaryEffect {

    private final Group group = new Group();
    private final int basePixelSize;
    private final double updateIntervalMs;
    private final boolean jitterPixelSize;

    private Pixelate pixelate;
    private PlanetaryDisc discWorld;

    public PixelationEffect(int basePixelSize, double updateIntervalMs, boolean jitterPixelSize) {
        this.basePixelSize = basePixelSize;
        this.updateIntervalMs = updateIntervalMs;
        this.jitterPixelSize = jitterPixelSize;

        group.setMouseTransparent(true);

        group.visibleProperty().addListener(e -> {
            if (pixelate != null) {
                if (group.isVisible()) {
                    if (!pixelate.isRunning()) {
                        pixelate.start();
                    }
                } else {
                    pixelate.stop();
                }
            }
        });

        group.parentProperty().addListener(il -> {
            if (group.getParent() == null && pixelate != null) {
                pixelate.stop();
            }
        });
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        this.discWorld = disc;
        int diameter = (int) (disc.getRadius() * 2);

        pixelate = new Pixelate(
            disc,
            diameter,
            diameter,
            basePixelSize,
            jitterPixelSize,
            updateIntervalMs
        );
        pixelate.setPixelateTime(800);
        pixelate.setMode(PixelationMode.RANDOM_BLOCKS);
        pixelate.setBlockCount(10);
        pixelate.setBlockSizeRange(20, 40);
        group.getChildren().clear();
        group.getChildren().add(pixelate.getCanvas());

        ClipUtils.applyCircularClip(group, disc.getPlanetCircle(), 4.0);
        pixelate.start();
    }

    @Override
    public void update(double occlusion) {
        group.setOpacity(occlusion);
    }

    @Override
    public Node getNode() {
        return group;
    }
}
