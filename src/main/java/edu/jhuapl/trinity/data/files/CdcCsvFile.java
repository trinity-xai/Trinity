package edu.jhuapl.trinity.data.files;

import edu.jhuapl.trinity.data.CdcCsv;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static edu.jhuapl.trinity.data.CdcCsv.csvToCdcCsv;

/**
 * @author Sean Phillips
 */
public class CdcCsvFile extends File implements Transferable {
    public static String FILE_DESC = "county,county_fips,state,county_population";
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(CdcCsvFile.class, "CDCCSVFile");
    public List<CdcCsv> cdcCsvList = null;

    /**
     * Constructor that extends File super constructor
     *
     * @param pathname Full path string to the file
     */
    public CdcCsvFile(String pathname) {
        super(pathname);
    }

    /**
     * Constructor that allows for automatically parsing the content of the file
     *
     * @param pathname      Full path string to the file
     * @param parseExisting True to automatically parse the content of the file
     *                      specified by pathname
     * @throws java.io.IOException
     */
    public CdcCsvFile(String pathname, Boolean parseExisting) throws IOException {
        super(pathname);
        if (parseExisting)
            cdcCsvList = parseContent();
    }

    /**
     * Tests whether a given File is a LayerableObject file or not
     *
     * @param file The File to be tested
     * @return True if the file being tested has header line matching FILE_DESC
     * @throws java.io.IOException
     */
    public static boolean isCdcCsvFile(File file) throws IOException {
        Optional<String> firstLine = Files.lines(file.toPath()).findFirst();
        return firstLine.isPresent() && firstLine.get().startsWith(FILE_DESC);
    }

    /**
     * Parses the file specified via this object's constructor
     * Assumes the file exists and is readable and conforms to expected format.
     *
     * @throws java.io.IOException
     */
    public void parse() throws IOException {
        cdcCsvList = parseContent();
    }

    private List<CdcCsv> parseContent() throws IOException {
        List<String> csvLines = Files.readAllLines(toPath());
        //Skip header row
        return csvLines.subList(1, csvLines.size())
            .stream().map(csvToCdcCsv).toList();
    }

    /**
     * Writes out the content of this File at the location
     * specified via this object's constructor.
     * Assumes the file exists.
     *
     * @throws java.io.IOException
     */
    public void writeContent() throws IOException {
        //@TODO SMP
        throw new UnsupportedOperationException("Write support unavailable");
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DATA_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor df) {
        return df == DATA_FLAVOR;
    }

    @Override
    public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException {
        if (df == DATA_FLAVOR) {
            return this;
        } else {
            throw new UnsupportedFlavorException(df);
        }
    }
}
