package edu.jhuapl.trinity.javafx.components;

import javafx.scene.paint.Color;

/**
 *
 * @author Sean Phillips
 */
public class VectorAction  {
    public Color color;
    public String label;
    public String name;
    
    public VectorAction() {
        
    }
    public VectorAction(String name, String label, Color colorObject) {
        this.label = label;
        this.name = name;
        color = colorObject;
    }
}
