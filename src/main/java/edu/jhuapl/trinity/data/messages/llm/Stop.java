package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Stop sequences returned in chat generation parameters.
 *
 * The observed response shape is a nested array, such as [[""]],
 * rather than a JSON object. This value object preserves that shape
 * with standard Jackson binding annotations.
 */
public class Stop {
    private List<List<String>> sequences;

    public Stop() {
        this.sequences = new ArrayList<>();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Stop(List<List<String>> sequences) {
        this.sequences = sequences;
    }

    @JsonValue
    public List<List<String>> getSequences() {
        return sequences;
    }

    public void setSequences(List<List<String>> sequences) {
        this.sequences = sequences;
    }
}
