package edu.jhuapl.trinity.data.audio;

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
 * Interface for audio decoders that return successive
 * amplitude frames.
 *
 * @author mzechner
 */
public interface Decoder {
    /**
     * Reads in samples.length samples from the decoder. Returns
     * the actual number read in. If this number is smaller than
     * samples.length then the end of stream has been reached.
     *
     * @param samples The number of read samples.
     */
    public int readSamples(float[] samples);
}
