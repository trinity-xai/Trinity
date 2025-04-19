/*******************************************************************************
 *    Original Copyright 2015, 2016 Taylor G Smith
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/

package com.clust4j.algo;

import com.clust4j.algo.BaseNeighborsModel.BaseNeighborsPlanner;
import com.clust4j.algo.BaseNeighborsModel.NeighborsAlgorithm;
import com.clust4j.metrics.pairwise.GeometricallySeparable;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.Random;

public class NearestNeighborsParameters extends BaseNeighborsPlanner<NearestNeighbors> {
    private static final long serialVersionUID = -4848896423352149405L;
    private final int k;


    public NearestNeighborsParameters() {
        this(BaseNeighborsModel.DEF_K);
    }

    public NearestNeighborsParameters(int k) {
        this.k = k;
    }

    @Override
    public NearestNeighbors fitNewModel(RealMatrix data) {
        return new NearestNeighbors(data, this.copy()).fit();
    }

    @Override
    public NearestNeighborsParameters setAlgorithm(NeighborsAlgorithm algo) {
        this.algo = algo;
        return this;
    }

    @Override
    public NearestNeighborsParameters copy() {
        return new NearestNeighborsParameters(k)
            .setAlgorithm(algo)
            .setSeed(seed)
            .setMetric(metric)
            .setVerbose(verbose)
            .setLeafSize(leafSize)
            .setForceParallel(parallel);
    }

    @Override
    final public Integer getK() {
        return k;
    }

    @Override
    final public Double getRadius() {
        return null;
    }

    public NearestNeighborsParameters setLeafSize(int leafSize) {
        this.leafSize = leafSize;
        return this;
    }

    @Override
    public NearestNeighborsParameters setSeed(Random rand) {
        this.seed = rand;
        return this;
    }

    @Override
    public NearestNeighborsParameters setVerbose(boolean b) {
        this.verbose = b;
        return this;
    }

    @Override
    public NearestNeighborsParameters setMetric(GeometricallySeparable dist) {
        this.metric = dist;
        return this;
    }

    @Override
    public NearestNeighborsParameters setForceParallel(boolean b) {
        this.parallel = b;
        return this;
    }
}
