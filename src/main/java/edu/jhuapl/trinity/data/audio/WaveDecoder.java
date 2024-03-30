package edu.jhuapl.trinity.data.audio;

/*-
 * #%L
 * trinity-2023.10.03
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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * A simple class that can read in the PCM data from a Wav file, converting the
 * data to signed 32-bit floats in the range [-1,1], merging stereo channels to
 * a mono channel for processing. This only supports 16-bit signed stereo and
 * mono Wav files with a sampling rate of 44100.
 *
 * @author mzechner
 */
public class WaveDecoder implements Decoder {

    /**
     * inverse max short value as float *
     */
    private final float MAX_VALUE = 1.0f / Short.MAX_VALUE;

    /**
     * the input stream we read from *
     */
    private final EndianDataInputStream in;

    /**
     * number of channels *
     */
    private final int channels;

    /**
     * sample rate in Herz*
     */
    private final float sampleRate;

    /**
     * *
     */
    /**
     * Constructor, sets the input stream to read the Wav file from.
     *
     * @param stream The input stream.
     * @throws Exception in case the input stream couldn't be read properly
     */
    public WaveDecoder(InputStream stream) throws Exception {
        if (stream == null) {
            throw new IllegalArgumentException("Input stream must not be null");
        }

        in = new EndianDataInputStream(new BufferedInputStream(stream, 1024 * 1024));
        if (!in.read4ByteString().equals("RIFF")) {
            throw new IllegalArgumentException("not a wav");
        }

        in.readIntLittleEndian();

        if (!in.read4ByteString().equals("WAVE")) {
            throw new IllegalArgumentException("expected WAVE tag");
        }

        if (!in.read4ByteString().equals("fmt ")) {
            throw new IllegalArgumentException("expected fmt tag");
        }

        if (in.readIntLittleEndian() != 16) {
            throw new IllegalArgumentException("expected wave chunk size to be 16");
        }

        if (in.readShortLittleEndian() != 1) {
            throw new IllegalArgumentException("expected format to be 1");
        }

        channels = in.readShortLittleEndian();
        sampleRate = in.readIntLittleEndian();
        if (sampleRate != 44100) {
            //throw new IllegalArgumentException("Not 44100 sampling rate");
            Logger.getLogger(WaveDecoder.class.getName()).warning(
                "Not 44100 sampling rate.\nSome functions may not behave.");
        }
        in.readIntLittleEndian();
        in.readShortLittleEndian();
        int fmt = in.readShortLittleEndian();

        if (fmt != 16) {
            throw new IllegalArgumentException("Only 16-bit signed format supported");
        }

        if (!in.read4ByteString().equals("data")) {
            throw new RuntimeException("expected data tag");
        }

        in.readIntLittleEndian();
    }

    /**
     * Tries to read in samples.length samples, merging stereo to a mono channel
     * by averaging and converting non float formats to float 32-bit. Returns
     * the number of samples actually read. Guarantees that samples.length
     * samples are read in if there was enough data in the stream.
     *
     * @param samples The samples array to write the samples to
     * @return The number of samples actually read.
     */
    @Override
    public int readSamples(float[] samples) {
        int readSamples = 0;
        for (int i = 0; i < samples.length; i++) {
            float sample = 0;
            try {
                for (int j = 0; j < channels; j++) {
                    int shortValue = in.readShortLittleEndian();
                    sample += (shortValue * MAX_VALUE);
                }
                sample /= channels;
                samples[i] = sample;
                readSamples++;
            } catch (Exception ex) {
                break;
            }
        }

        return readSamples;
    }
}
