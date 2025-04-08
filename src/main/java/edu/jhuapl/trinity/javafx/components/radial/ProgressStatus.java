/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.radial;

import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class ProgressStatus {

    public Color fillStartColor = Color.CYAN;
    public Color fillEndColor = Color.CYAN;
    public Color innerStrokeColor = Color.CYAN;
    public Color outerStrokeColor = Color.CYAN;
    public String statusMessage = "";
    public String topMessage = "";
    public double percentComplete; //-1 for indeterminate

    public ProgressStatus(String statusMessage, double percentComplete) {
        this.statusMessage = statusMessage;
        this.percentComplete = percentComplete;
    }

}
