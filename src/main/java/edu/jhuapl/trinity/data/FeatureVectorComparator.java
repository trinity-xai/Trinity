package edu.jhuapl.trinity.data;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;

import java.util.Comparator;

/**
 * @author Sean Phillips
 */
public class FeatureVectorComparator implements Comparator<FeatureVector> {
    private int compareIndex = 0;

    public FeatureVectorComparator(int compareIndex) {
        this.compareIndex = compareIndex;
    }

    @Override
    public int compare(FeatureVector o1, FeatureVector o2) {
        if (o1.getData().get(getCompareIndex()) > o2.getData().get(getCompareIndex())) {
            return 1;
        } else if (o1.getData().get(getCompareIndex()) < o2.getData().get(getCompareIndex())) {
            return -1;
        } else
            return 0;
    }

    /**
     * @return the compareIndex
     */
    public int getCompareIndex() {
        return compareIndex;
    }

    /**
     * @param compareIndex the compareIndex to set
     */
    public void setCompareIndex(int compareIndex) {
        this.compareIndex = compareIndex;
    }


}
