package edu.jhuapl.trinity.data.messages.llm;

import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * @author Sean Phillips
 */
public enum Prompts {
    INSTANCE;
    public static final String PROMPTS_DEFAULT_PATH = "services/"; //default to local relative path
    public static final String DEFAULT_CAPTION_PROMPT = "captionPrompt.txt";
    public static final String AUTOCHOOSE_CAPTION_PROMPT = "autochooseCaptionPrompt.txt";
    public static final String AUTOCHOOSE_VARIABLE = "\\$CAPTION_CHOICES";

    public static String loadDefaultCaptionPrompt() throws IOException {
        File file = new File(PROMPTS_DEFAULT_PATH + DEFAULT_CAPTION_PROMPT);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        String message = Files.readString(file.toPath());
        return message;
    }

    public static String loadAutochooseCaptionPrompt() throws IOException {
        File file = new File(PROMPTS_DEFAULT_PATH + AUTOCHOOSE_CAPTION_PROMPT);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        String message = Files.readString(file.toPath());
        return message;
    }

    public static String insertAutochooseChoices(String captionPrompt, List<String> choices) {
        StringBuilder choicesCSV = new StringBuilder("");
        for (int i = 0; i < choices.size(); i++) {
            choicesCSV.append(choices.get(i));
            if (i < choices.size() - 1)
                choicesCSV.append(", ");
            else
                choicesCSV.append(" ");
        }
        return captionPrompt.replaceAll(AUTOCHOOSE_VARIABLE, choicesCSV.toString());
    }

    private static void notifyTerminalError(String message, Scene scene) {
        Platform.runLater(() -> {
            CommandTerminalEvent cte = new CommandTerminalEvent(
                message, new Font("Consolas", 20), Color.RED);
            scene.getRoot().fireEvent(cte);
        });
    }
}
