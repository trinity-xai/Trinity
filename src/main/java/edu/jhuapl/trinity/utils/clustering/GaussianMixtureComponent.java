/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.clustering;

/**
 * the mixture distribution is defined by a distribution
 * and its weight in the mixture.
 *
 * @author Sean Phillips
 */
public class GaussianMixtureComponent {
    /**
     * The priori probability of component.
     */
    public final double priori;

    /**
     * The distribution of component.
     */
    public final GaussianDistribution distribution;

    /**
     * Constructor.
     *
     * @param priori       the priori probability of component.
     * @param distribution the distribution of component.
     */
    public GaussianMixtureComponent(double priori, GaussianDistribution distribution) {
        this.priori = priori;
        this.distribution = distribution;
    }
}
