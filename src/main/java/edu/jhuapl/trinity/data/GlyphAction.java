/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data;

//import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javafx.scene.paint.Color;


/**
 * @author Sean Phillips
 */
//@JsonTypeInfo(
//    use = JsonTypeInfo.Id.MINIMAL_CLASS,
//    include = JsonTypeInfo.As.PROPERTY,
//    property = "@type")
public class GlyphAction {
    public Color color;
    public String label;
    public String name;

    public GlyphAction() {

    }

    public GlyphAction(String name, String label, Color colorObject) {
        this.name = name;
        this.label = label;
        color = colorObject;
    }
}
