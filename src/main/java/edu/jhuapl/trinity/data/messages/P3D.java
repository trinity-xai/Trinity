package edu.jhuapl.trinity.data.messages;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.function.Function;
import org.fxyz3d.geometry.Point3D;

/**
 * @author Sean Phillips
 * Cheesy little JSONable version of Point3D without all the cruft
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class P3D extends MessageData {
    public static Function<Point3D, P3D> fxyzPoint3DToP3D = 
        p -> new P3D(p.x, p.y, p.z);

    public static final String TYPESTRING = "p3d";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {
        "x":1.0,
        "y":2.5,
        "z":9000.1
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private double x;
    private double y;
    private double z;
    //</editor-fold>

    public P3D() {
    }
    public P3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

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
    }

    //</editor-fold>
}
