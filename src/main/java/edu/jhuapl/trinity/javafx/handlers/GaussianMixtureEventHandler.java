package edu.jhuapl.trinity.javafx.handlers;

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

import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.messages.GaussianMixture;
import edu.jhuapl.trinity.data.messages.GaussianMixture.COVARIANCE_MODE;
import edu.jhuapl.trinity.data.messages.GaussianMixtureCollection;
import edu.jhuapl.trinity.data.messages.GaussianMixtureData;
import edu.jhuapl.trinity.javafx.components.ColorMap;
import edu.jhuapl.trinity.javafx.events.GaussianMixtureEvent;
import edu.jhuapl.trinity.javafx.renderers.GaussianMixtureRenderer;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import javafx.event.EventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class GaussianMixtureEventHandler implements EventHandler<GaussianMixtureEvent> {

    List<GaussianMixtureRenderer> renderers;
    ColorMap colorMap;
    int colorIndex = 0;
    int colorCount = 0;

    public GaussianMixtureEventHandler() {
        renderers = new ArrayList<>();
        colorMap = ColorMap.tableau();
        colorCount = colorMap.getFixedColorCount();
    }

    public void handleGaussianMixtureEvent(GaussianMixtureEvent event) {
        if (event.getEventType().equals(GaussianMixtureEvent.NEW_GAUSSIAN_COLLECTION)) {
            GaussianMixtureCollection gaussianMixtureCollection = (GaussianMixtureCollection) event.object;
            if (null != gaussianMixtureCollection.getAxesLabels()
                && !gaussianMixtureCollection.getAxesLabels().isEmpty()) {
                //update labels on axes
                for (GaussianMixtureRenderer renderer : renderers) {
                    renderer.setFeatureLabels(gaussianMixtureCollection.getAxesLabels());
                }
            }
            gaussianMixtureCollection.getMixtures().forEach(gaussianMixture -> {
                updateGMLabelsAndColor(gaussianMixture);
            });

            //generate the diagonal for ellipsoid rendering
            //for each Gaussian Mixture
            for (GaussianMixture gm : gaussianMixtureCollection.getMixtures()) {
                //go through each gaussian mixture
                for (GaussianMixtureData gmd : gm.getData()) {
                    //If we have a full covariance matrix
                    if (gm.getCovarianceMode().contentEquals(COVARIANCE_MODE.FULL.name())) {
                        //perform SVD and then Rotation, result is the diagonal
                        gmd.setEllipsoidDiagonal(
                            AnalysisUtils.gmmFullCovToDiag(gmd.getCovariance())
                        );
                    } else { //the current covariance object is already a diagonal
                        //Just copy the first (only) vector of doubles as the diagonal
                        gmd.setEllipsoidDiagonal(gmd.getCovariance().get(0));
                    }
                }
            }
            //send the updated collection out to the renderers
            for (GaussianMixtureRenderer renderer : renderers) {
                renderer.addGaussianMixtureCollection(gaussianMixtureCollection);
            }
        } else if (event.getEventType().equals(GaussianMixtureEvent.NEW_GAUSSIAN_MIXTURE)) {
            GaussianMixture gaussianMixture = (GaussianMixture) event.object;
            updateGMLabelsAndColor(gaussianMixture);
            for (GaussianMixtureRenderer renderer : renderers) {
                renderer.addGaussianMixture(gaussianMixture);
            }
        }
    }

    private void updateGMLabelsAndColor(GaussianMixture gaussianMixture) {
        //Have we seen this label before?
        FactorLabel matchingLabel = FactorLabel.getFactorLabel(gaussianMixture.getLabel());
        //The label is new... add a new FactorLabel row
        if (null == matchingLabel) {
            if (colorIndex > colorCount) {
                colorIndex = 0;
            }
            FactorLabel fl = new FactorLabel(gaussianMixture.getLabel(),
                colorMap.getColorByIndex(colorIndex));
            FactorLabel.addFactorLabel(fl);
            colorIndex++;
        }
    }

    public void addGaussianMixtureRenderer(GaussianMixtureRenderer renderer) {
        renderers.add(renderer);
    }

    @Override
    public void handle(GaussianMixtureEvent event) {
        handleGaussianMixtureEvent(event);
    }
}
