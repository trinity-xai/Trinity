package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseChatMessage {
    private String role;
    private String content;
    private String reasoning_content;
    private List<String> tool_calls;

    public String getReasoning_content() {
        return reasoning_content;
    }

    public void setReasoning_content(String reasoning_content) {
        this.reasoning_content = reasoning_content;
    }

    public ResponseChatMessage() {
        tool_calls = new ArrayList<>();
    }

    /**
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * @param role the role to set
     */
    public void setRole(String role) {
        this.role = role;
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

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }
}
