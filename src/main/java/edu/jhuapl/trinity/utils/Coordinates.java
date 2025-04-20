package edu.jhuapl.trinity.utils;

/**
 * coordinates defines a set of coordinate systems used when
 * defining objects in layers below
 *
 * @author Sean Phillips
 */
public class Coordinates {

    public Dimensions domain;
    public Dimensions range;

    /**
     * optional offset to account for size of object being placed
     */
    public double dataRadius = 0;

    public Coordinates() {

    }

    /**
     * @param domain room coordinates for mapping to the canvas
     * @param range  Range in canvas coordinates
     */
    public Coordinates(Dimensions domain, Dimensions range) {
        this.domain = domain;
        this.range = range;
    }

    /**
     * Performs a linear transformation of the provided value from the domain
     * X axis to its equivalent in the range X axis.
     * Typically the domain dimension is the local or data coordinate system
     * while the range could be the global, screen (pixels), scene (as in
     * scenegraph) or parent node.
     *
     * @param dataXcoord the X axis value in the domain coordinate system
     * @return The X axis value in the range coordinate system
     */
    public double transformXToScreen(double dataXcoord) {
//        //@DEBUG SMP Very useful print to debug the transformations
//        System.out.println("Transform X to Screen => domain minX: " + domain.minX.get() + " maxX: " + domain.maxX.get());
//        System.out.println("Transform X to Screen => range height: " + range.getWidth() + " domain width: " + domain.getWidth());
        //New maintains aspect ratio way
        return (((dataXcoord - domain.getMinX().get()) * (range.getSquare() - dataRadius)) / domain.getWidth()) + dataRadius;
    }

    /**
     * Performs a linear transformation of the provided value from the domain
     * Y axis to its equivalent in the range Y axis.
     * Typically the domain dimension is the local or data coordinate system
     * while the range could be the global, screen (pixels), scene (as in
     * scenegraph) or parent node.
     *
     * @param dataYcoord the Y axis value in the domain coordinate system
     * @return The Y axis value in the range coordinate system
     */
    public double transformYToScreen(double dataYcoord) {
//        //@DEBUG SMP Very useful print to debug the transformations
//        System.out.println("Transform Y to Screen => domain minY: " + domain.minY.get() + " maxY: " + domain.maxY.get());
//        System.out.println("Transform Y to Screen => range height: " + range.getHeight() + " domain height: " + domain.getHeight());
        //New maintains aspect ratio way
        return (((dataYcoord - domain.getMinY().get()) * (range.getSquare() - dataRadius)) / domain.getHeight()) + dataRadius - range.getSquareOffset();
    }

    /**
     * Performs a linear transformation of the provided value from the range
     * X axis to its equivalent in the domain X axis. This is the reverse of
     * transformXToScreen().
     * Typically the domain dimension is the local or data coordinate system
     * while the range could be the global, screen (pixels), scene (as in
     * scenegraph) or parent node.
     *
     * @param dataXcoord the X axis value in the range coordinate system
     * @return The X axis value in the domain coordinate system
     */
    public double transformScreenToX(double dataXcoord) {
        //New maintains aspect ratio way
        return domain.getMinX().get() + ((dataXcoord * domain.getWidth()) / range.getSquare());
    }

    /**
     * Performs a linear transformation of the provided value from the range
     * Y axis to its equivalent in the domain Y axis. This is the reverse of
     * transformYToScreen().
     * Typically the domain dimension is the local or data coordinate system
     * while the range could be the global, screen (pixels), scene (as in
     * scenegraph) or parent node.
     *
     * @param dataYcoord the Y axis value in the range coordinate system
     * @return The Y axis value in the domain coordinate system
     */
    public double transformScreenToY(double dataYcoord) {
        //New maintains aspect ratio way
        return domain.getMinY().get() + (((dataYcoord + range.getSquareOffset()) * domain.getHeight()) / range.getSquare());
    }
}
