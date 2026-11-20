package edu.jhuapl.trinity.utils;

import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceUtilsSaveImageTest {

    @TempDir
    File tempDir;

    @Test
    public void saveImageFileCreatesMissingDirectoryAndUsesPrefix() throws IOException {
        File missingDir = new File(tempDir, "does/not/exist/imagery");
        assertTrue(!missingDir.exists());

        File saved = ResourceUtils.saveImageFile(new WritableImage(4, 3), missingDir, "Scan-");

        assertTrue(saved.isFile(), "saved file should exist");
        assertEquals(missingDir, saved.getParentFile());
        assertTrue(saved.getName().startsWith("Scan-"), "file name should use the given prefix: " + saved.getName());
        assertTrue(saved.getName().endsWith(".png"));

        BufferedImage readBack = ImageIO.read(saved);
        assertEquals(4, readBack.getWidth());
        assertEquals(3, readBack.getHeight());
    }

    @Test
    public void saveImageFileProducesUniqueNames() throws IOException {
        File first = ResourceUtils.saveImageFile(new WritableImage(2, 2), tempDir, "Trinity-scan-");
        File second = ResourceUtils.saveImageFile(new WritableImage(2, 2), tempDir, "Trinity-scan-");
        assertTrue(!first.getName().equals(second.getName()));
        assertTrue(Files.exists(first.toPath()) && Files.exists(second.toPath()));
    }
}
