package edu.jhuapl.trinity.data.files;

/*-
 * #%L
 * trinity
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

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.ZeroPilotLatents;
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

/**
 * @author Sean Phillips
 */
public class ZeroPilotLatentsFile extends File implements Transferable {
    public static String FILE_DESC = "labels,traj_num";
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(ZeroPilotLatentsFile.class, "ZeroPilotLatentsFile");
    public List<ZeroPilotLatents> zeroPilotLatentsList = null;

    /**
     * Constructor that extends File super constructor
     *
     * @param pathname Full path string to the file
     */
    public ZeroPilotLatentsFile(String pathname) {
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
    public ZeroPilotLatentsFile(String pathname, Boolean parseExisting) throws IOException {
        super(pathname);
        if (parseExisting)
            zeroPilotLatentsList = parseContent();
    }

    /**
     * Tests whether a given File is likely a ZeroPilotLatents file
     *
     * @param file The File to be tested
     * @return True if the file being tested has header line containing FILE_DESC
     * @throws java.io.IOException
     */
    public static boolean isZeroPilotLatentsFile(File file) throws IOException {
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
        zeroPilotLatentsList = parseContent();
    }

    private List<ZeroPilotLatents> parseContent() throws IOException {
        Scene scene = App.getAppScene();
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Loading Zero Pilot Latents File...", -1);
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
        List<ZeroPilotLatents> latentList = new ArrayList<>(noOfLines);
        try (InputStream in = Files.newInputStream(toPath());
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = null;
            Integer current = 0;
            int updatePercent = noOfLines / 10; //Find a nice balanced increment for the animation
            if (updatePercent < 1) //safety check for the maths
                updatePercent = 1;
            //For each line in the file convert to an object
            while ((line = reader.readLine()) != null) {
                if (current != 0 && null != line) //skip header row and don't read <EOF>
                    latentList.add(ZeroPilotLatents.csvToZero.apply(line));
                current++;
                //Send updates for loading indicators everywhere to rejoice
                if (current % updatePercent == 0) {
                    double percentComplete = Double.valueOf(current) / Double.valueOf(noOfLines);
                    //System.out.println("percentComplete: " + percentComplete);
                    Platform.runLater(() -> {
                        ProgressStatus ps = new ProgressStatus(
                            "Loading Zero Pilot Latents File...", percentComplete);
                        ps.fillStartColor = Color.CYAN;
                        ps.fillEndColor = Color.CADETBLUE;
                        ps.innerStrokeColor = Color.DARKMAGENTA;
                        ps.outerStrokeColor = Color.CADETBLUE;
                        scene.getRoot().fireEvent(
                            new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
                    });
                }
            }
        } catch (IOException x) {
            System.err.println(x);
        }

        return latentList;
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
