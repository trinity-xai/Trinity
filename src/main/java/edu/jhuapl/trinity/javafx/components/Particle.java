package edu.jhuapl.trinity.javafx.components;

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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public class Particle {

    private DoubleProperty x = new SimpleDoubleProperty();
    private DoubleProperty y = new SimpleDoubleProperty();

    private Point2D velocity = Point2D.ZERO;

    private Color color;

    private double life = 1.0;
    private boolean active = false;

    public Particle(int x, int y, Color color) {
        this.x.set(x);
        this.y.set(y);
        this.color = color;
    }

    public DoubleProperty xProperty() {
        return x;
    }

    public DoubleProperty yProperty() {
        return y;
    }

    public double getX() {
        return x.get();
    }

    public double getY() {
        return y.get();
    }

    public boolean isDead() {
        return life == 0;
    }

    public boolean isActive() {
        return active;
    }

    public void activate(Point2D velocity) {
        active = true;
        this.velocity = velocity;
    }

    public void update() {
        if (!active)
            return;

//        life -= 0.017 * 0.75;
//
//        if (life < 0)
//            life = 0;

        this.x.set(getX() + velocity.getX());
        this.y.set(getY() + velocity.getY());
    }

    public void draw(GraphicsContext g) {
        g.setFill(color);

//        g.setGlobalAlpha(life);
        g.fillOval(getX(), getY(), 1, 1);
    }
}
