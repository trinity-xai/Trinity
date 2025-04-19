package edu.jhuapl.trinity.javafx.javafx3d.particle;

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class DualTextureParticle extends AgingParticle {
    private static final Logger LOG = LoggerFactory.getLogger(DualTextureParticle.class);

    public static double DEFAULT_FIT_WIDTH = 16;
    public float[] color2 = new float[4]; // The Particle's Color
    public float[] colorCounter2 = new float[4]; // The Color Counter!
    public ImageView texture1;
    public ImageView texture2;

    public DualTextureParticle(String t1, String t2) {
        try {
            setTexture1(ResourceUtils.load3DTextureImage(t1));
            setTexture2(ResourceUtils.load3DTextureImage(t2));
        } catch (final IOException _e) {
            LOG.info("Unable to load texture");
        }
    }

    public DualTextureParticle(Image image1, Image image2) {
        setTexture1(image1);
        setTexture2(image2);
    }

    public final void setTexture1(Image image1) {
        texture1 = new ImageView(image1);
        texture1.setDepthTest(DepthTest.ENABLE);
        texture1.setPreserveRatio(true);
        texture1.setFitWidth(DEFAULT_FIT_WIDTH);
        texture1.visibleProperty().bind(activeProperty);

    }

    public final void setTexture2(Image image2) {
        texture2 = new ImageView(image2);
        texture2.setDepthTest(DepthTest.ENABLE);
        texture2.setPreserveRatio(true);
        texture2.setFitWidth(DEFAULT_FIT_WIDTH);
        texture2.visibleProperty().bind(activeProperty);
    }

    @Override
    public Node getNode() {
        return texture1;
    }
}
