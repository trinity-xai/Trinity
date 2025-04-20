package edu.jhuapl.trinity.utils.marchingcubes;

import java.util.ArrayList;

/**
 * Created by Primoz on 8.7.2016.
 */
abstract class CallbackMC implements Runnable {
    private ArrayList<float[]> vertices;

    void setVertices(ArrayList<float[]> vertices) {
        this.vertices = vertices;
    }

    ArrayList<float[]> getVertices() {
        return this.vertices;
    }
}
