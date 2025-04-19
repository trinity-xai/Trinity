package edu.jhuapl.trinity.javafx.javafx3d.animated;

import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Random;

/**
 * @author Sean Phillips
 */
public class BillBoard extends BillboardNode<Group> {

    //    private Group group;
    private Node other;
    private ImageView view;
    double fitWidth = 5;
    double particleSpread = 100;
    int particleCount = 10000;

    public BillBoard(Node other, Image image) {
        super();
        this.other = other;
//        group = new Group();
//        getChildren().add(group);
        Random rando = new Random();

        for (int i = 0; i < particleCount; i++) {
            ImageView v = new ImageView(image);
            v.setFitWidth(fitWidth);
            v.setPreserveRatio(true);
            v.setSmooth(true);
//                group.getChildren().add(v);
            getChildren().add(v);
            v.setTranslateX(rando.nextGaussian() * particleSpread);
            v.setTranslateY(rando.nextGaussian() * particleSpread);
            v.setTranslateZ(rando.nextGaussian() * particleSpread);
        }

        //group.setManaged(false);
        setManaged(false);
        setDepthTest(DepthTest.ENABLE);

    }

    @Override
    protected Group getBillboardNode() {
        return this;
    }

    @Override
    protected Node getTarget() {
        return other;
    }
}
