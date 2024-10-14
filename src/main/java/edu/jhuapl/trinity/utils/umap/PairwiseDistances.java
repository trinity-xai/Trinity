/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils.umap;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.utils.metric.Metric;
import edu.jhuapl.trinity.utils.metric.PrecomputedMetric;
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
    double[] xkRow;
    double[] kjDistances;
    private static int count = 0;


    public PairwiseDistances(int rowIndex, int totalRows, double[] xkRow) {
        this.rowIndex = rowIndex;
        this.xkRow = xkRow;
        kjDistances = new double[totalRows]; //distances matrix will be n x n
    }

    static Matrix parallelPairwise(final Matrix x, final Metric metric, boolean mVerbose) {
        if (PrecomputedMetric.SINGLETON.equals(metric)) {
            return x;
        }
        final int n = x.rows();
        final double[][] distances = new double[n][n];
        ArrayList<PairwiseDistances> pdList = new ArrayList<>(n);
        for (int k = 0; k < n; ++k) {
            pdList.add(new PairwiseDistances(k, n, x.row(k)));
        }
        Scene scene = App.getAppScene();
        count = 0; //this is the start of the hack
        int percent = 100;
        if (n < 100)
            percent = 10;
        if (n < 10)
            percent = 1;
        int updatePercent = n / percent;

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
        final double[][] distances = new double[n][n];
        if (mVerbose) {
            Utils.message("computing " + n + " squared pairwise distances");
        }
        for (int k = 0; k < n; ++k) {
            final double[] xk = x.row(k);
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
        final double[][] distances = new double[xn][yn];
        for (int k = 0; k < xn; ++k) {
            final double[] xk = x.row(k);
            for (int j = 0; j < yn; ++j) {
                distances[k][j] = metric.distance(xk, y.row(j));
            }
        }
        return new DefaultMatrix(distances);
    }

}
