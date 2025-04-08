package edu.jhuapl.trinity.data.files;

import edu.jhuapl.trinity.data.messages.xai.ShapleyCollection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sean phillips
 */
public class ShapleyCollectionFileTest {
    private static final Logger LOG = LoggerFactory.getLogger(ShapleyCollectionFileTest.class);

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
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testWriteContent() throws Exception {
        LOG.info("writeContent");
        ShapleyCollectionFile instance = new ShapleyCollectionFile("./ShapleyFile.json");
        ShapleyCollection sc = ShapleyCollection.fakeCollection(512, 512);
        sc.setSourceInput("OnyxHappyFace.png");
        instance.shapleyCollection = sc;
        instance.writeContent();
    }

}
