package edu.jhuapl.trinity.javafx.renderers;

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

import edu.jhuapl.trinity.data.messages.GaussianMixture;
import edu.jhuapl.trinity.data.messages.GaussianMixtureCollection;

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
