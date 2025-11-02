package edu.jhuapl.trinity.utils.clustering;

/**
 * the mixture distribution is defined by a distribution
 * and its weight in the mixture.
 *
 * @param priori       The priori probability of component.
 * @param distribution The distribution of component.
 * @author Sean Phillips
 */
public record GaussianMixtureComponent(double priori, GaussianDistribution distribution) {
}
