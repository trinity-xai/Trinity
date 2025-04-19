package edu.jhuapl.trinity.data.audio;

import org.jflac.FLACDecoder;
import org.jflac.PCMProcessor;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;
import org.jflac.util.WavWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Decode FLAC file to WAV file application.
 *
 * @author kc7bfi
 */
public class FlacToWav implements PCMProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FlacToWav.class);
    private WavWriter wav;

    public FlacToWav() {

    }

    /**
     * Decode a FLAC file to a WAV file.
     *
     * @param inFileName  The input FLAC file name
     * @param outFileName The output WAV file name
     * @throws IOException Thrown if error reading or writing files
     */
    public void decode(String inFileName, String outFileName) throws Exception {
        LOG.info("Decode [{}][{}]", inFileName, outFileName);
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            //@DEBUG SMP System.out.println("Creating File Streams...");
            is = new FileInputStream(inFileName);
            os = new FileOutputStream(outFileName);
            wav = new WavWriter(os);
            //@DEBUG SMP System.out.println("Initializing FLAC Decoder...");
            FLACDecoder decoder = new FLACDecoder(is);
            //@DEBUG SMP System.out.println("Adding PCM Processor...");
            decoder.addPCMProcessor(this);
            //@DEBUG SMP System.out.println("Attempting to decode...");
            decoder.decode();
            LOG.info("Decoding complete.");
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
     *
     * @param info the StreamInfo block
     * @see org.jflac.PCMProcessor#processStreamInfo(org.jflac.metadata.StreamInfo)
     */
    @Override
    public void processStreamInfo(StreamInfo info) {
        try {
            wav.writeHeader(info);
        } catch (IOException e) {
            LOG.error("Exception", e);
        }
    }

    /**
     * Process the decoded PCM bytes.
     *
     * @param pcm The decoded PCM data
     * @see org.jflac.PCMProcessor#processPCM(org.jflac.util.ByteSpace)
     */
    @Override
    public void processPCM(ByteData pcm) {
        try {
            wav.writePCM(pcm);
        } catch (IOException e) {
            LOG.error("Exception", e);
        }
    }
}
