package edu.jhuapl.trinity.data.audio;

/*-
 * #%L
 * trinity-2024.01.24
 * %%
 * Copyright (C) 2021 - 2024 The Johns Hopkins University Applied Physics Laboratory LLC
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jflac.PCMProcessor;
import org.jflac.FLACDecoder;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;
import org.jflac.util.WavWriter;

/**
 * Decode FLAC file to WAV file application.
 * @author kc7bfi
 */
public class FlacToWav implements PCMProcessor {
    private WavWriter wav;
    public FlacToWav() {
        
    }
    /**
     * Decode a FLAC file to a WAV file.
     * @param inFileName    The input FLAC file name
     * @param outFileName   The output WAV file name
     * @throws IOException  Thrown if error reading or writing files
     */
    public  void decode(String inFileName, String outFileName) throws IOException {
        System.out.println("Decode [" + inFileName + "][" + outFileName + "]");
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(inFileName);
            os = new FileOutputStream(outFileName);
            wav = new WavWriter(os);
            FLACDecoder decoder = new FLACDecoder(is);
            decoder.addPCMProcessor(this);
            decoder.decode();
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }
    
    /**
     * Process the StreamInfo block.
     * @param info the StreamInfo block
     * @see org.jflac.PCMProcessor#processStreamInfo(org.jflac.metadata.StreamInfo)
     */
    @Override
    public void processStreamInfo(StreamInfo info) {
        try {
            wav.writeHeader(info);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Process the decoded PCM bytes.
     * @param pcm The decoded PCM data
     * @see org.jflac.PCMProcessor#processPCM(org.jflac.util.ByteSpace)
     */
    @Override
    public void processPCM(ByteData pcm) {
        try {
            wav.writePCM(pcm);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
//    /**
//     * Main routine.
//     * <p>args[0] is the input file name
//     * <p>args[1] is the output file name
//     * @param args  Command line arguments
//     */
//    public static void main(String[] args) {
//        try {
//            Decoder decoder = new Decoder();
//            decoder.decode(args[0], args[1]);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
     
}
