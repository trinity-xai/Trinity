package edu.jhuapl.trinity.data.audio.flac;

/*-
 * #%L
 * trinity-2023.11.01
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
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author phillsm1
 */
public class FlacConverter {

    public FlacStreamInfo streamInfo;

    public FlacConverter(FlacStreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public float[] convertToWavBytes(int[][] samples) throws IOException {
        float[] monoSamples = new float[samples[0].length];
        // Start writing WAV output file
        int bytesPerSample = streamInfo.sampleDepth / 8;
        float sample = 0;
        for (int i = 0; i < samples[0].length; i++) {
            for (int j = 0; j < samples.length; j++) {
                int val = samples[j][i];
                if (bytesPerSample == 1) {
                    // Convert to unsigned, as per WAV PCM conventions
                    //out.write(val + 128);  
                    monoSamples[i] = val + 128;
                } else {  // 2 <= bytesPerSample <= 4
                    //effectively the channels? not sure
                    for (int k = 0; k < bytesPerSample; k++) {
                        //out.write(val >>> (k * 8));  // Little endian
                        sample += val >>> (k * 8);
                    }
                    monoSamples[i] = sample / bytesPerSample;
                }
            }
        }
        return monoSamples;
    }

    // Helper members for writing WAV files
    public static void writeLittleInt16(DataOutputStream out, int x) throws IOException {
        out.writeShort(Integer.reverseBytes(x) >>> 16);
    }

    public static void writeLittleInt32(DataOutputStream out, int x) throws IOException {
        out.writeInt(Integer.reverseBytes(x));
    }

    public static void decodeFlacToWav(File flacFile, File wavFile) throws IOException {
        // Decode input FLAC file
        FlacStreamInfo streamInfo;
        int[][] samples;
        try (FlacDecoder dec = new FlacDecoder(flacFile)) {
            // Handle metadata header blocks
            while (dec.readAndHandleMetadataBlock() != null);
            streamInfo = dec.streamInfo;
            if (streamInfo.sampleDepth % 8 != 0) {
                throw new UnsupportedOperationException("Only whole-byte sample depth supported");
            }

            // Decode every block
            samples = new int[streamInfo.numChannels][(int) streamInfo.numSamples];
            for (int off = 0;;) {
                int len = dec.readAudioBlock(samples, off);
                if (len == 0) {
                    break;
                }
                off += len;
            }
        }

        // Check audio MD5 hash
        byte[] expectHash = streamInfo.md5Hash;
        if (Arrays.equals(expectHash, new byte[16])) {
            System.err.println("Warning: MD5 hash field was blank");
        } else if (!Arrays.equals(FlacStreamInfo.getMd5Hash(samples, streamInfo.sampleDepth), expectHash)) {
            throw new DataFormatException("MD5 hash check failed");
        }
        // Else the hash check passed

        // Start writing WAV output file
        int bytesPerSample = streamInfo.sampleDepth / 8;
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(wavFile)))) {
            // Header chunk
            int sampleDataLen = samples[0].length * streamInfo.numChannels * bytesPerSample;
            out.writeInt(0x52494646);  // "RIFF"
            writeLittleInt32(out, sampleDataLen + 36);
            out.writeInt(0x57415645);  // "WAVE"

            // Metadata chunk
            out.writeInt(0x666D7420);  // "fmt "
            writeLittleInt32(out, 16);
            writeLittleInt16(out, 0x0001);
            writeLittleInt16(out, streamInfo.numChannels);
            writeLittleInt32(out, streamInfo.sampleRate);
            writeLittleInt32(out, streamInfo.sampleRate * streamInfo.numChannels * bytesPerSample);
            writeLittleInt16(out, streamInfo.numChannels * bytesPerSample);
            writeLittleInt16(out, streamInfo.sampleDepth);

            // Audio data chunk ("data")
            out.writeInt(0x64617461);  // "data"
            writeLittleInt32(out, sampleDataLen);
            for (int i = 0; i < samples[0].length; i++) {
                for (int j = 0; j < samples.length; j++) {
                    int val = samples[j][i];
                    if (bytesPerSample == 1) {
                        out.write(val + 128);  // Convert to unsigned, as per WAV PCM conventions
                    } else {  // 2 <= bytesPerSample <= 4
                        for (int k = 0; k < bytesPerSample; k++) {
                            out.write(val >>> (k * 8));  // Little endian
                        }
                    }
                }
            }
        }
    }
}
