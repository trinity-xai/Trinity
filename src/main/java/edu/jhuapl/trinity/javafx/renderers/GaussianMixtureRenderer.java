/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.renderers;

import edu.jhuapl.trinity.data.messages.xai.GaussianMixture;
import edu.jhuapl.trinity.data.messages.xai.GaussianMixtureCollection;

import java.util.List;

/**
 * @author Sean Phillips
 */
public interface GaussianMixtureRenderer {
    public void addGaussianMixtureCollection(GaussianMixtureCollection gaussianMixtureCollection);

    public void addGaussianMixture(GaussianMixture gaussianMixture);

    public void clearGaussianMixtures();

    public void setDimensionLabels(List<String> labelStrings);
}
