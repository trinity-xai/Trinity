package edu.jhuapl.trinity.data.files;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sean Phillips
 */
public abstract class DroppableFile extends File implements Transferable {
    public static final Logger LOG = LoggerFactory.getLogger(DroppableFile.class);

    /**
     * Constructor that extends File super constructor
     *
     * @param pathname Full path string to the file
     */    
    public DroppableFile(String pathname) {
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
    public DroppableFile(String pathname, Boolean parseExisting) throws IOException {
        super(pathname);
        if (parseExisting)
            parseContent();
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
    public abstract DataFlavor getDataFlavor();
    public abstract void parseContent() throws IOException;
    /**
     * Writes out the content of this File at the location
     * specified via this object's constructor.
     * Assumes the file exists.
     *
     * @throws java.io.IOException
     */    
    public abstract void writeContent() throws IOException;

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{getDataFlavor()};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor df) {
        return df == getDataFlavor();
    }

    @Override
    public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException {
        if (df == getDataFlavor()) {
            return this;
        } else {
            throw new UnsupportedFlavorException(df);
        }
    }    
}