package edu.jhuapl.trinity.data.terrain;

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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class FireAreaTextFile extends File implements Transferable {
    public static final String FIREAREA_TEXT_MARKER = "FIRE_AREA";
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(FireAreaTextFile.class, FIREAREA_TEXT_MARKER);
    public ArrayList<ArrayList<Double>> dataGrid = null;

    /**
     * Constructor that extends File super constructor
     *
     * @param pathname Full path string to the file
     */
    public FireAreaTextFile(String pathname) {
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
    public FireAreaTextFile(String pathname, Boolean parseExisting) throws IOException {
        super(pathname);
        if (parseExisting)
            parseContent();
    }

    /**
     * Tests whether a given File is a SemanticMapCollection by looking at
     * the first two lines for one of the file descriptor string signatures.
     *
     * @param file The File to be tested
     * @return True if the file being tested has header line matching FILE_DESC
     * @throws java.io.IOException
     */
    public static boolean isFireAreaTextFile(File file) throws IOException {
        return Files.lines(file.toPath())
            .findFirst().get().contains(FIREAREA_TEXT_MARKER);
    }

    /**
     * Parses the file specified via this object's constructor
     * Assumes the file exists and is readable and conforms to expected format.
     *
     * @throws java.io.IOException
     */
    public final void parseContent() throws IOException {
        dataGrid = new ArrayList<>();
        ArrayList<Double> row = new ArrayList<>();
        List<String> csvLines = Files.readAllLines(toPath());

        //Skip header row
        for (String csvLine : csvLines.subList(1, csvLines.size())) {
            String[] values = csvLine.split(",");
            if (values.length > 0) {
                //Have we hit the wierd non standard final terminator character?
                if (values[0].contains("}"))
                    return; //break out. We're done.
                //are we starting a new row?
                //there are two spaces for the row start, four spaces otherwise
                if (values[0].charAt(2) != ' ') {
                    //if not empty then add the previously built up row to the dataGrid
                    if (!row.isEmpty())
                        dataGrid.add(row);
                    //reset the row
                    row = new ArrayList<>();
                }
                //parse the tokens and add to current row
                for (String value : values) {
                    if (!value.isBlank()) {
                        try {
                            //last line has terminator char
                            if (value.contains(";"))
                                value = value.replace(";", "");
                            row.add(Double.valueOf(value.trim()));
                        } catch (NumberFormatException ex) {
                            row.add(0.0);
                        }
                    }
                }
            }
        }
        System.out.println("Fire Area Text File parsing complete.");
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
