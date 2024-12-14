/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.files;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Sean Phillips
 */
public class GraphDirectedCollectionFile extends File implements Transferable {
    private static final Logger LOG = LoggerFactory.getLogger(GraphDirectedCollectionFile.class);
    public static String FILE_DESC1 = "\"type\": \"GraphDirectedCollection\"";
    public static String FILE_DESC2 = "\"type\":\"GraphDirectedCollection\"";
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(GraphDirectedCollectionFile.class, "GRAPHDIRECTEDCOLLECTION");
    public GraphDirectedCollection graphDirectedCollection = null;

    /**
     * Constructor that extends File super constructor
     *
     * @param pathname Full path string to the file
     */
    public GraphDirectedCollectionFile(String pathname) {
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
    public GraphDirectedCollectionFile(String pathname, Boolean parseExisting) throws IOException {
        super(pathname);
        if (parseExisting)
            parseContent();
    }

    /**
     * Tests whether a given File is a LayerableObject file or not
     *
     * @param file The File to be tested
     * @return True if the file being tested has header line matching FILE_DESC
     * @throws java.io.IOException
     */
    public static boolean isGraphDirectedCollectionFile(File file) throws IOException {
        String extension = file.getAbsolutePath().substring(
            file.getAbsolutePath().lastIndexOf("."));
        if (extension.equalsIgnoreCase(".json")) {
            String body = Files.readString(file.toPath());
            return GraphDirectedCollection.isGraphDirectedCollection(body);
        }
        return false;
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

    private GraphDirectedCollection parseContent() throws IOException {
        /** Provides deserialization support for JSON messages */
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String message = Files.readString(this.toPath());
        graphDirectedCollection = mapper.readValue(message, GraphDirectedCollection.class);
        return graphDirectedCollection;
    }

    /**
     * Writes out the content of this File at the location
     * specified via this object's constructor.
     * Assumes the file exists.
     *
     * @throws java.io.IOException
     */
    public void writeContent() throws IOException {
        if (null != graphDirectedCollection) {
            ObjectMapper mapper = new ObjectMapper();
            //mapper.configure(SerializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.writeValue(this, graphDirectedCollection);
            LOG.info("GraphDirectedCollection serialized to file.");
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
