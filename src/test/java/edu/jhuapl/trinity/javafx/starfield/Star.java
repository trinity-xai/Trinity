package edu.jhuapl.trinity.javafx.starfield;

/*-
 * #%L
 * trinity
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

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;

public class Star {

    double x;
    double y;
    double z;
    Circle body;
    Color color = Color.ALICEBLUE;
    double r = 3;
    public static ArrayList<Star> stars = new ArrayList<>();

    public Star() {
        x = Utils.getRandom(-Starfield.width / 2, Starfield.width / 2);
        y = Utils.getRandom(-Starfield.height / 2, Starfield.height / 2);
        z = Utils.getRandom(Starfield.width);
        double sx = Utils.map(x / z, 0, 1, 0, Starfield.width);
        double sy = Utils.map(y / z, 0, 1, 0, Starfield.height);
        r = Utils.map(z, 0, Starfield.width, 5, 0);
        color = Color.gray(Utils.map(z, 0, Starfield.width, 1, 0.7));
        body = new Circle(sx + Starfield.width / 2, sy + Starfield.height / 2, r, color);
        stars.add(this);
    }

    public void update() {
        z = z - Starfield.speed;
        if (z < 1) {
            z = Starfield.width;
            x = Utils.getRandom(-Starfield.width / 2, Starfield.width / 2);
            y = Utils.getRandom(-Starfield.height / 2, Starfield.height / 2);
        }

        double sx = Utils.map(x / z, 0, 1, 0, Starfield.width);
        double sy = Utils.map(y / z, 0, 1, 0, Starfield.height);
        body.setCenterX(sx + Starfield.width / 2);
        body.setCenterY(sy + Starfield.height / 2);
        r = Utils.map(z, 0, Starfield.width, 5, 0);
        body.setRadius(r);
        color = Color.gray(Utils.map(z, 0, Starfield.width, 1, 0.7));
        body.setFill(color);
    }

    public Node getBody() {
        return this.body;
    }
}
