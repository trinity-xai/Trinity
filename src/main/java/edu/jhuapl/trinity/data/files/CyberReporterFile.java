package edu.jhuapl.trinity.data.files;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.xai.CyberReport;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Sean Phillips
 */
public class CyberReporterFile extends DroppableFile {
    public CyberReport cyberReport = null;

    public CyberReporterFile(String pathname) {
        super(pathname);
    }
    public CyberReporterFile(String pathname, Boolean parseExisting) throws IOException {
        super(pathname);
        if (parseExisting)
            parseContent();
    }    
    /**
     * Tests whether a given File is this type of file or not
     *
     * @param file The File to be tested
     * @return True if the file being tested is this type of file
     * @throws java.io.IOException
     */    
    public static boolean isFileType(File file) throws IOException {
        String extension = file.getName().substring(file.getName().lastIndexOf("."));
        if (extension.equalsIgnoreCase(".json")) {
            String body = Files.readString(file.toPath());
            return CyberReport.isCyberReport(body);
        }
        return false;
    }
    @Override
    public DataFlavor getDataFlavor() {
        return new DataFlavor(CyberReportFile.class, "CYBERREPORT");
    }

    @Override
    public void parseContent() throws IOException {
        /** Provides deserialization support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String message = Files.readString(this.toPath());
        cyberReport = mapper.readValue(message, CyberReport.class);
    }

    @Override
    public void writeContent() throws IOException {
        if (null != cyberReport) {
            ObjectMapper mapper = new ObjectMapper();
            //mapper.configure(SerializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.writeValue(this, cyberReport);
            LOG.info("CyberReport serialized to file.");
        }
    }
}