package edu.jhuapl.trinity.utils;

/*-
 * #%L
 * trinity-2023.09.26
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

import edu.jhuapl.trinity.utils.AnalysisUtils.ANALYSIS_METHOD;
import edu.jhuapl.trinity.utils.AnalysisUtils.SOURCE;

/**
 *
 * @author phillsm1
 */
public class PCAConfig {
    public SOURCE source;
    public ANALYSIS_METHOD method;
    public int pcaDimensions;
    public double scaling;
    public int startIndex;
    public int endIndex; //-1 means use max

    public PCAConfig(SOURCE source, ANALYSIS_METHOD method, int pcaDimensions, double scaling, int startIndex, int endIndex) {
        this.source = source;
        this.method = method;
        this.pcaDimensions = pcaDimensions;
        this.scaling = scaling;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    
}
