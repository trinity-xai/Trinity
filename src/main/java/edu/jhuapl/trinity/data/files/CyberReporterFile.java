package edu.jhuapl.trinity.data.files;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.xai.CyberReport;
import edu.jhuapl.trinity.data.messages.xai.CyberReportIO;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class CyberReporterFile extends DroppableFile {
    public List<CyberReport> cyberReports = null;

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
        return new DataFlavor(CyberReporterFile.class, "CYBERREPORT");
    }

    @Override
    public void parseContent() throws IOException {
        /** Provides deserialization support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String message = Files.readString(this.toPath());
        // From a string:
        cyberReports = CyberReportIO.readReports(message);
    }

    @Override
    public void writeContent() throws IOException {
        if (null != cyberReports) {
            ObjectMapper mapper = new ObjectMapper();
            //mapper.configure(SerializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.writeValue(this, cyberReports);
            LOG.info("CyberReports serialized to file.");
        }
    }
}
