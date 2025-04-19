package edu.jhuapl.trinity.data.files;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.cislunar.McclodSplitDataTsv;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static edu.jhuapl.trinity.data.cislunar.McclodSplitDataTsv.tsvToMcclodSplitDataTsv;

/**
 * @author Sean Phillips
 */
public class McclodSplitDataTsvFile extends File implements Transferable {
    public static String FILE_DESC = "MCCLOD Split Data";
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(McclodSplitDataTsvFile.class, "McclodSplitDataTsvFile");
    public List<McclodSplitDataTsv> mcclodSplitDataTsvList = null;

    /**
     * Constructor that extends File super constructor
     *
     * @param pathname Full path string to the file
     */
    public McclodSplitDataTsvFile(String pathname) {
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
    public McclodSplitDataTsvFile(String pathname, Boolean parseExisting) throws IOException {
        super(pathname);
        if (parseExisting)
            mcclodSplitDataTsvList = parseContent();
    }

    /**
     * Tests whether a given File is likely a CdcTissueGenes file
     *
     * @param file The File to be tested
     * @return True if the file being tested has header line containing FILE_DESC
     * @throws java.io.IOException
     */
    public static boolean isMcclodSplitDataTsvFile(File file) throws IOException {
        Optional<String> firstLine = Files.lines(file.toPath()).findFirst();
        return firstLine.isPresent() && firstLine.get().contains(FILE_DESC);
    }

    /**
     * Parses the file specified via this object's constructor
     * Assumes the file exists and is readable and conforms to expected format.
     *
     * @throws java.io.IOException
     */
    public void parse() throws IOException {
        mcclodSplitDataTsvList = parseContent();
    }

    private List<McclodSplitDataTsv> parseContent() throws IOException {
        Scene scene = App.getAppScene();
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Loading Split Data File...", -1);
            ps.fillStartColor = Color.CYAN;
            ps.fillEndColor = Color.DEEPPINK;
            ps.innerStrokeColor = Color.CYAN;
            ps.outerStrokeColor = Color.DEEPPINK;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });

        Integer noOfLines = null;
        try (LineNumberReader reader = new LineNumberReader(new FileReader(this))) {
            reader.skip(Integer.MAX_VALUE);
            noOfLines = reader.getLineNumber();
        }
        List<McclodSplitDataTsv> tissueList = new ArrayList<>(noOfLines);
        try (InputStream in = Files.newInputStream(toPath());
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = null;
            Integer current = 0;
            int updatePercent = noOfLines / 50;
            while ((line = reader.readLine()) != null) {
                if (current != 0 && null != line)
                    tissueList.add(tsvToMcclodSplitDataTsv.apply(line));
                current++;
                if (current % updatePercent == 0) {
                    double percentComplete = Double.valueOf(current) / Double.valueOf(noOfLines);
                    //System.out.println("percentComplete: " + percentComplete);
                    Platform.runLater(() -> {
                        ProgressStatus ps = new ProgressStatus(
                            "Loading Gene File...", percentComplete);
                        ps.fillStartColor = Color.CYAN;
                        ps.fillEndColor = Color.DEEPPINK;
                        ps.innerStrokeColor = Color.CYAN;
                        ps.outerStrokeColor = Color.DEEPPINK;
                        scene.getRoot().fireEvent(
                            new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
                    });
                }
            }
        } catch (IOException x) {
            System.err.println(x);
        }

        return tissueList;
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
