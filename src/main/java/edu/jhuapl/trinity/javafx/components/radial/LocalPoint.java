package edu.jhuapl.trinity.javafx.components.radial;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

import java.util.ArrayList;
import java.util.List;

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

/**
 * Just a plain old pojo for x, y and z coordinates, mimicking Point3D in a
 * shallow manner. This allows us to perform serialization and decouples the
 * model package from JavaFX's implementation.
 *
 * @author Sean Phillips
 */
public class LocalPoint implements Observable {

    //    @JsonIgnore
    List<InvalidationListener> listeners;

    /**
     * specific anchor location in the X axis (that can be referenced by other objects)
     */
    public double x;
    /**
     * specific anchor location in the Y axis (that can be referenced by other objects)
     */
    public double y;
    /**
     * specific anchor location in the Z axis (that can be referenced by other objects)
     * For 2D renderings this will be ignored.
     */
    public double z;

    public LocalPoint() {
        this(0, 0, 0);
    }

    public LocalPoint(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        listeners = new ArrayList<>();
    }

    /**
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(double x) {
        this.x = x;
        notifyListeners();
    }

    /**
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(double y) {
        this.y = y;
        notifyListeners();
    }

    /**
     * @return the z
     */
    public double getZ() {
        return z;
    }

    /**
     * @param z the z to set
     */
    public void setZ(double z) {
        this.z = z;
        notifyListeners();
    }

    private void notifyListeners() {
        listeners.forEach(listener -> listener.invalidated(this));
    }

    @Override
    public void addListener(InvalidationListener il) {
        listeners.add(il);
    }

    @Override
    public void removeListener(InvalidationListener il) {
        listeners.remove(il);
    }
}
