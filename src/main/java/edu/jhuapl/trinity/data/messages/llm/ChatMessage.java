package edu.jhuapl.trinity.data.messages.llm;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Sean Phillips
 */
public class ChatMessage {
    private String role;
    private String content;
    private List<String> tool_calls;
    
    public ChatMessage(){
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
}
