package edu.jhuapl.trinity.data.messages.llm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * @author Sean Phillips
 */
public enum Prompts {
    INSTANCE;
    public static String PROMPTS_DEFAULT_PATH = "services/"; //default to local relative path
    public static final String CAPTION_PROMPT_FILENAME = "captionPrompt.txt";
    public static final String AUTOCHOOSE_CAPTION_FILENAME = "autochooseCaptionPrompt.txt";
    public static final String AUTOCHOOSE_VARIABLE = "\\$CAPTION_CHOICES";
    public static final String DEFAULT_CAPTION_PROMPT_TEXT =
        """
            Generate a text caption for this image. The caption you generate should be in the English language. The caption should be a noun limited to a single word.
            Write your response as formatted json data. The formatted json response should have a field for "caption" which is where you put the word you are generating to describe the image.
            The formatted json response should have an "explanation" field and "description" field.
            The field called "explanation" is a single String type value where you provide any explanation of why you think the one word caption is appropriate. Please limit the "explanation" String value to four sentences or less.
            The field called "description" is a single String type value where you provide text to describe details of what you see in the image. Please limit the "description" String value to four sentences or less.
            """;

    public static final String DEFAULT_AUTOCHOOSECAPTION_PROMPT_TEXT =
        """
            Generate a text caption for this image. The caption you generate should be in the English language. The caption should be a noun limited to a single word. You must choose your caption from the following list of comma separated words: $CAPTION_CHOICES
            Write your response as formatted json data. The formatted json response should have a field for "caption" which is where you put the word you are generating to describe the image.
            The formatted json response should have an "explanation" field and "description" field.
            The field called "explanation" is a single String type value where you provide an explanation of why you chose the one word caption from the list of words provided above. Please limit the "explanation" String value to four sentences or less.
            The field called "description" is a single String type value where you provide text to describe details of what you see in the image. Please limit the "description" String value to four sentences or less.
            """;

    public static String loadDefaultCaptionPrompt() throws IOException {
        File file = new File(PROMPTS_DEFAULT_PATH + CAPTION_PROMPT_FILENAME);
        if (!file.exists() || !file.canRead()) {
            return DEFAULT_CAPTION_PROMPT_TEXT;
        }
        String message = Files.readString(file.toPath());
        return message;
    }

    public static String loadAutochooseCaptionPrompt() throws IOException {
        File file = new File(PROMPTS_DEFAULT_PATH + AUTOCHOOSE_CAPTION_FILENAME);
        if (!file.exists() || !file.canRead()) {
            return DEFAULT_AUTOCHOOSECAPTION_PROMPT_TEXT;
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
}
