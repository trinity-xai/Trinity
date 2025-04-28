package edu.jhuapl.trinity.data.files;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import edu.jhuapl.trinity.data.SaturnShot;
import edu.jhuapl.trinity.utils.Utils;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Sean Phillips
 */
public class SaturnFile extends File implements Transferable {
    public static String VARCOL = "VarName1";
    public static String TIMECOL = "Time_ms";
    public static String SHUTTERCOL = "Shutter";
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(SaturnFile.class, "SaturnFile");
    public List<SaturnShot> shots = null;

    //Example Header and single csv line
    //VarName1,Time_ms,Shutter,X_mm,Y_mm,Z_mm,Power,PD_0,PD_1,PD_2,PD_3
    //20808943,115605.238888889,1,140.648198254215,205.063043298452,3.2,6.248054042,0.000562797779999902,0.0171936016,-0.00541654708,-0.00408584207999962
    
    /**
     * Constructor that extends File super constructor
     *
     * @param pathname Full path string to the file
     */
    public SaturnFile(String pathname) {
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
    public SaturnFile(String pathname, Boolean parseExisting) throws IOException {
        super(pathname);
        if (parseExisting)
            shots = parseContent();
    }

    /**
     * Tests whether a given File is likely a Saturn csv file
     *
     * @param file The File to be tested
     * @return True if the file being tested has header line we need
     * @throws java.io.IOException
     */
    public static boolean isSaturnFile(File file) throws IOException {
        Optional<String> firstLine = Files.lines(file.toPath()).findFirst();
        return firstLine.isPresent() && firstLine.get().trim().startsWith(VARCOL)
            && firstLine.get().contains(TIMECOL) && firstLine.get().contains(SHUTTERCOL);
    }

    /**
     * Parses the file specified via this object's constructor
     * Assumes the file exists and is readable and conforms to expected format.
     *
     * @throws java.io.IOException
     */
    public void parse() throws IOException {
        shots = parseContent();
    }

    public List<SaturnShot> parseContent() throws IOException {
//        Scene scene = App.getAppScene();
//        Platform.runLater(() -> {
//            ProgressStatus ps = new ProgressStatus("Loading Saturn File...", -1);
//            ps.fillStartColor = Color.CYAN;
//            ps.fillEndColor = Color.DEEPPINK;
//            ps.innerStrokeColor = Color.CYAN;
//            ps.outerStrokeColor = Color.DEEPPINK;
//            scene.getRoot().fireEvent(
//                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
//        });

        long channelCount = 0;
        try {
            System.out.println("Parallel FileChannel sample count... ");
            long startTime = System.nanoTime();
            char charizar = '\n';
            channelCount = Utils.charCount(this, charizar);
            Utils.printTotalTime(startTime);
            System.out.println("Number of lines: " + channelCount);        
        } catch (InterruptedException ex) {
            Logger.getLogger(SaturnFile.class.getName()).log(Level.SEVERE, null, ex);
        }
////////////        
        List<SaturnShot> shotList = new ArrayList<>();
//
//        final FileChannel channel = new FileInputStream(this).getChannel();
//        MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
//        // when finished
//        channel.close();        

        System.out.println("Parsing with Files.lines()... ");
        long startTime = System.nanoTime();
        shotList = Files.lines(this.toPath())
            .skip(1).map(SaturnShot.csvToSaturnShot)
            .collect(Collectors.toCollection(ArrayList::new)); //makes the new list mutable
        Utils.printTotalTime(startTime);

//        try (InputStream in = Files.newInputStream(toPath());
//             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
//            String line = null;
//            Integer current = 0;
//            int updatePercent = noOfLines / 50; //Find a nice balanced increment for the animation
//            if (updatePercent < 1) //safety check for the maths
//                updatePercent = 1;
//            while ((line = reader.readLine()) != null) {
//                if (current != 0 && null != line)
//                    tissueList.add(csvToCdcTissueGenes.apply(line));
//                current++;
//                if (current % updatePercent == 0) {
//                    double percentComplete = Double.valueOf(current) / Double.valueOf(noOfLines);
//                    //System.out.println("percentComplete: " + percentComplete);
//                    Platform.runLater(() -> {
//                        ProgressStatus ps = new ProgressStatus(
//                            "Loading Gene File...", percentComplete);
//                        ps.fillStartColor = Color.CYAN;
//                        ps.fillEndColor = Color.DEEPPINK;
//                        ps.innerStrokeColor = Color.CYAN;
//                        ps.outerStrokeColor = Color.DEEPPINK;
//                        scene.getRoot().fireEvent(
//                            new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
//                    });
//                }
//            }
//        } catch (IOException x) {
//            System.err.println(x);
//        }

        return shotList;
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
