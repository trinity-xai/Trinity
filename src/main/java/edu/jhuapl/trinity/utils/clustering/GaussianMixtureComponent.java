package edu.jhuapl.trinity.utils.clustering;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
