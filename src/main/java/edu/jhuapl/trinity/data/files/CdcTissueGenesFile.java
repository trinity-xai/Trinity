package edu.jhuapl.trinity.data.files;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.CdcTissueGenes;
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

import static edu.jhuapl.trinity.data.CdcTissueGenes.csvToCdcTissueGenes;

/**
 * @author Sean Phillips
 */
public class CdcTissueGenesFile extends File implements Transferable {
    public static String FILE_DESC = "Tissue_name";
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(CdcTissueGenesFile.class, "CDCCSVFile");
    public List<CdcTissueGenes> cdcTissueGenesList = null;

    /**
     * Constructor that extends File super constructor
     *
     * @param pathname Full path string to the file
     */
    public CdcTissueGenesFile(String pathname) {
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
    public CdcTissueGenesFile(String pathname, Boolean parseExisting) throws IOException {
        super(pathname);
        if (parseExisting)
            cdcTissueGenesList = parseContent();
    }

    /**
     * Tests whether a given File is likely a CdcTissueGenes file
     *
     * @param file The File to be tested
     * @return True if the file being tested has header line containing FILE_DESC
     * @throws java.io.IOException
     */
    public static boolean isCdcTissueGenesFile(File file) throws IOException {
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
        cdcTissueGenesList = parseContent();
    }

    private List<CdcTissueGenes> parseContent() throws IOException {
        Scene scene = App.getAppScene();
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Loading Gene File...", -1);
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
        List<CdcTissueGenes> tissueList = new ArrayList<>(noOfLines);
        try (InputStream in = Files.newInputStream(toPath());
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = null;
            Integer current = 0;
            int updatePercent = noOfLines / 50; //Find a nice balanced increment for the animation
            if (updatePercent < 1) //safety check for the maths
                updatePercent = 1;
            while ((line = reader.readLine()) != null) {
                if (current != 0 && null != line)
                    tissueList.add(csvToCdcTissueGenes.apply(line));
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
