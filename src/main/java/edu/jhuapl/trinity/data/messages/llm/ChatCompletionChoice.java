package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionChoice {
    private String text;
    private ResponseChatMessage message;
    private String raw;
    private Integer index;
    private Double logprobs;
    private String finish_reason;
    private List<String> tool_calls;
    
    public ChatCompletionChoice() {
        tool_calls = new ArrayList<>();
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the message
     */
    public ResponseChatMessage getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(ResponseChatMessage message) {
        this.message = message;
    }

    /**
     * @return the raw
     */
    public String getRaw() {
        return raw;
    }

    /**
     * @param raw the raw to set
     */
    public void setRaw(String raw) {
        this.raw = raw;
    }

    /**
     * @return the index
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(Integer index) {
        this.index = index;
    }

    /**
     * @return the logprobs
     */
    public Double getLogprobs() {
        return logprobs;
    }

    /**
     * @param logprobs the logprobs to set
     */
    public void setLogprobs(Double logprobs) {
        this.logprobs = logprobs;
    }

    /**
     * @return the finish_reason
     */
    public String getFinish_reason() {
        return finish_reason;
    }

    /**
     * @param finish_reason the finish_reason to set
     */
    public void setFinish_reason(String finish_reason) {
        this.finish_reason = finish_reason;
    }

    /**
     * @return the tool_calls
     */
    public List<String> getTool_calls() {
        return tool_calls;
    }

    /**
     * @param tool_calls the tool_calls to set
     */
    public void setTool_calls(List<String> tool_calls) {
        this.tool_calls = tool_calls;
    }
    //</editor-fold>
}
