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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.ManifoldData;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Sean Phillips
 */
public class ManifoldDataFile extends File implements Transferable {
    public static String FILE_DESC1 = "\"messageType\": \"manifold_data\"";
    public static String FILE_DESC2 = "\"messageType\":\"manifold_data\"";
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(ManifoldDataFile.class, "ManifoldData");
    public ManifoldData manifoldData = null;

    /**
     * Constructor that extends File super constructor
     *
     * @param pathname Full path string to the file
     */
    public ManifoldDataFile(String pathname) {
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
    public ManifoldDataFile(String pathname, Boolean parseExisting) throws IOException {
        super(pathname);
        if (parseExisting)
            parseContent();
    }

    /**
     * Tests whether a given File is a ManifoldData file or not
     *
     * @param file The File to be tested
     * @return True if the file being tested has header line matching FILE_DESC
     * @throws java.io.IOException
     */
    public static boolean isManifoldDataFile(File file) throws IOException {
        String body = Files.readString(file.toPath());
        return ManifoldData.isManifoldData(body);
//        return firstLine.isPresent() &&
//        (firstLine.get().contains(FILE_DESC1) || firstLine.get().contains(FILE_DESC2));
    }

    /**
     * Parses the file specified via this object's constructor
     * Assumes the file exists and is readable and conforms to expected format.
     *
     * @throws java.io.IOException
     */
    public void parse() throws IOException {
        parseContent();
    }

    private ManifoldData parseContent() throws IOException {
        /** Provides deserialization support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String message = Files.readString(this.toPath());
        manifoldData = mapper.readValue(message, ManifoldData.class);
        return manifoldData;
    }

    /**
     * Writes out the content of this File at the location
     * specified via this object's constructor.
     * Assumes the file exists.
     *
     * @throws java.io.IOException
     */
    public void writeContent() throws IOException {
        if (null != manifoldData) {
            ObjectMapper mapper = new ObjectMapper();
            //mapper.configure(SerializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.writeValue(this, manifoldData);
            System.out.println("ManifoldData serialized to file.");
        }
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
