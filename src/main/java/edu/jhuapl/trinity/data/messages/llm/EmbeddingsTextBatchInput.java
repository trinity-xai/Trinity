package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** OpenAI-compatible batch of text inputs (one embedding per input string). */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingsTextBatchInput {
    private Object input;
    private String model;
    private String encoding_format = "float";
    private String embedding_type = "all";
    private int dimensions = 512;
    private String user = "string";

    public Object getInput() { return input; }
    public void setInput(List<String> input) { this.input = input; }
    public void setInput(String input) { this.input = input; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getEncoding_format() { return encoding_format; }
    public void setEncoding_format(String value) { encoding_format = value; }
    public String getEmbedding_type() { return embedding_type; }
    public void setEmbedding_type(String value) { embedding_type = value; }
    public int getDimensions() { return dimensions; }
    public void setDimensions(int value) { dimensions = value; }
    public String getUser() { return user; }
    public void setUser(String value) { user = value; }
}
