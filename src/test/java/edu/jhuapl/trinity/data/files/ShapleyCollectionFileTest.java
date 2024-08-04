package edu.jhuapl.trinity.data.files;

import edu.jhuapl.trinity.data.messages.ShapleyCollection;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author sean phillips
 */
public class ShapleyCollectionFileTest {
    
    public ShapleyCollectionFileTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of writeContent method, of class ShapleyCollectionFile.
     * @throws java.lang.Exception
     */
    @Test
    public void testWriteContent() throws Exception {
        System.out.println("writeContent");
        ShapleyCollectionFile instance = new ShapleyCollectionFile("./ShapleyFile.json");
        ShapleyCollection sc = ShapleyCollection.fakeCollection(512, 512);
        sc.setSourceInput("OnyxHappyFace.png");
        instance.shapleyCollection = sc;
        instance.writeContent();
    }
    
}
