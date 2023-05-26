package edu.jhuapl.trinity.javafx.javafx3d.animated;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
