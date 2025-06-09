package edu.jhuapl.trinity.javafx.components.projector;

/**
 * @author Sean Phillips
 */
public class ProjectorRow {
    public String rowLabel;
    public int row;
    //Radians
    public double currentAngle = 0.0;
    //Pixels
    public double currentRadius = 9001;
    //Radians
    public double angleStepSize = 0.07; //@TODO SMP Dynamically figure this out
    //Pixels
    public double radiusStepSize = 2000; //@TODO SMP Dynamically figure this out

    public ProjectorRow(String rowLabel, int row, double currentRadius) {
        this.rowLabel = rowLabel;
        this.row = row;
        this.currentRadius = currentRadius;
    }

    public double getRadius() {
        return currentRadius;
    }

    public double getAngleAndStep() {
        double t = currentAngle;
        currentAngle += angleStepSize;
        //check if we've made a complete circle
        if (currentAngle > 6.283) { //angle in radians
            currentAngle = 0.0; //reset
            currentRadius += radiusStepSize;
        }
        return t;
    }

}
