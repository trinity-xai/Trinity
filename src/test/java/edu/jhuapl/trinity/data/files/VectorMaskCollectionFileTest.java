package edu.jhuapl.trinity.data.files;

import edu.jhuapl.trinity.data.messages.VectorMaskCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sean phillips
 */
public class VectorMaskCollectionFileTest {
    
    public VectorMaskCollectionFileTest() {
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
        VectorMaskCollectionFile instance = new VectorMaskCollectionFile("./VectorMask.json");
        VectorMaskCollection sc = VectorMaskCollection.fakeCollection(512, 512, 1.0);
        instance.vectorMaskCollection = sc;
        instance.writeContent();
    }
    
}
