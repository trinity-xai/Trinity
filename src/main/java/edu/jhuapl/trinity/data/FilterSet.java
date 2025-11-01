package edu.jhuapl.trinity.data;

/**
 * @author Sean Phillips
 */
public class FilterSet {

    public enum Inclusion {
        INNER, OUTER, BOUNDARIES
    }

    public Inclusion inclusion;
    public Number minimum = -1;
    public Number maximum = 1;

    public FilterSet(Number minimum, Number maximum, Inclusion inclusion) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.inclusion = inclusion;
    }

    @Override
    public String toString() {
        return minimum + " to " + maximum + " " + inclusion.name();
    }
}
