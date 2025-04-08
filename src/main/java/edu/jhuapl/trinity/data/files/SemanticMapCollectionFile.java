/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data.files;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.bci.SemanticMapCollection;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Sean Phillips
 */
public class SemanticMapCollectionFile extends File implements Transferable {
    public static String SEMANTIC_SPACE_MARKER = "semantic_space";
    public static String FILE_DESC1 = "\"type\": \"SemanticMapCollection\"";
    public static String FILE_DESC2 = "\"type\":\"SemanticMapCollection\"";
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(SemanticMapCollectionFile.class, "SEMANTICMAPCOLLECTION");
    public SemanticMapCollection semanticMapCollection = null;

    /**
     * Constructor that extends File super constructor
     *
     * @param pathname Full path string to the file
     */
    public SemanticMapCollectionFile(String pathname) {
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
    public SemanticMapCollectionFile(String pathname, Boolean parseExisting) throws IOException {
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
    public static boolean isSemanticMapCollectionFile(File file) throws IOException {
        return Files.lines(file.toPath()).limit(2).anyMatch(t -> {
            int lim = t.length() > 256 ? 256 : t.length();
            return t.substring(0, lim).contains(FILE_DESC1)
                || t.substring(0, lim).contains(FILE_DESC2);
        });
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

    private SemanticMapCollection parseContent() throws IOException {
        /** Provides deserialization support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String message = Files.readString(this.toPath());
        semanticMapCollection = mapper.readValue(message, SemanticMapCollection.class);
        return semanticMapCollection;
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
