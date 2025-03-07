package edu.jhuapl.trinity.data.messages.llm;

import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 *
 * @author Sean Phillips
 */
public enum Prompts {
    INSTANCE;
    public static final String PROMPTS_DEFAULT_PATH = "services/"; //default to local relative path
    public static final String DEFAULT_CAPTION_PROMPT = "captionPrompt.txt";
      

    public static String loadDefaultCaptionPrompt() throws IOException {
        File file = new File(PROMPTS_DEFAULT_PATH + DEFAULT_CAPTION_PROMPT);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        String message = Files.readString(file.toPath());
        return message;
    }    
  
    private static void notifyTerminalError(String message, Scene scene) {
        Platform.runLater(() -> {
            CommandTerminalEvent cte = new CommandTerminalEvent(
                message, new Font("Consolas", 20), Color.RED);
            scene.getRoot().fireEvent(cte);
        });            
    }
}
