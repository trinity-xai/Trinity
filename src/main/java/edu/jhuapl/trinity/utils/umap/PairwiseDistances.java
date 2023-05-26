/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
package edu.jhuapl.trinity.utils.umap;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.javafx.components.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.utils.umap.metric.Metric;
import edu.jhuapl.trinity.utils.umap.metric.PrecomputedMetric;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.util.ArrayList;

/**
 * Compute pairwise distances between instances using a specified metric.
 *
 * @author Sean A. Irvine
 * @author Richard Littin
 */
public class PairwiseDistances {

    // replacement for sklearn.pairwise_distances
    int rowIndex;
    float[] xkRow;
    float[] kjDistances;
    private static int count = 0;


    public PairwiseDistances(int rowIndex, int totalRows, float[] xkRow) {
        this.rowIndex = rowIndex;
        this.xkRow = xkRow;
        kjDistances = new float[totalRows]; //distances matrix will be n x n
    }

    static Matrix parallelPairwise(final Matrix x, final Metric metric, boolean mVerbose) {
        if (PrecomputedMetric.SINGLETON.equals(metric)) {
            return x;
        }
        final int n = x.rows();
        final float[][] distances = new float[n][n];
        ArrayList<PairwiseDistances> pdList = new ArrayList<>(n);
        for (int k = 0; k < n; ++k) {
            pdList.add(new PairwiseDistances(k, n, x.row(k)));
        }
        Scene scene = App.getAppScene();
        count = 0; //this is the start of the hack
        int updatePercent = n / 100;
        pdList.parallelStream()
            .peek(e -> {
                count++;
                if (count % updatePercent == 0) {
                    double percentComplete = Double.valueOf(count) / Double.valueOf(n);
                    Platform.runLater(() -> {
                        ProgressStatus ps = new ProgressStatus(
                            "Computing Pairwise Distances...", percentComplete);
                        ps.fillStartColor = Color.AZURE;
                        ps.fillEndColor = Color.LIME;
                        ps.innerStrokeColor = Color.AZURE;
                        ps.outerStrokeColor = Color.LIME;
                        scene.getRoot().fireEvent(
                            new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
                    });
                }
            })
            .forEach(row -> {
                for (int j = 0; j < n; ++j) {
                    row.kjDistances[j] = metric.distance(row.xkRow, x.row(j));
                }
                System.arraycopy(row.kjDistances, 0, distances[row.rowIndex], 0, row.kjDistances.length);
            });

        return new DefaultMatrix(distances);
    }

    static Matrix pairwiseDistances(final Matrix x, final Metric metric) {
        return pairwiseDistances(x, metric, false);
    }

    static Matrix pairwiseDistances(final Matrix x, final Metric metric, boolean mVerbose) {
        if (PrecomputedMetric.SINGLETON.equals(metric)) {
            return x;
        }
        final int n = x.rows();
        final float[][] distances = new float[n][n];
        if (mVerbose) {
            Utils.message("computing " + n + " squared pairwise distances");
        }
        for (int k = 0; k < n; ++k) {
            final float[] xk = x.row(k);
            for (int j = 0; j < n; ++j) {
                distances[k][j] = metric.distance(xk, x.row(j));
            }
            if (mVerbose && k % (n / 100) == 0) {
                Utils.message(k + " of " + n + " rows computed...");
            }
        }
        return new DefaultMatrix(distances);
    }

    static Matrix pairwiseDistances(final Matrix x, final Matrix y, final Metric metric) {
        if (PrecomputedMetric.SINGLETON.equals(metric)) {
            throw new IllegalArgumentException("Cannot use this method with precomputed");
        }
        final int xn = x.rows();
        final int yn = y.rows();
        final float[][] distances = new float[xn][yn];
        for (int k = 0; k < xn; ++k) {
            final float[] xk = x.row(k);
            for (int j = 0; j < yn; ++j) {
                distances[k][j] = metric.distance(xk, y.row(j));
            }
        }
        return new DefaultMatrix(distances);
    }

}
