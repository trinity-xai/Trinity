/* Copyright (C) 2021 - 2024 Sean Phillips */
package edu.jhuapl.trinity.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 *
 * @author sean phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CocoAnnotation {
    public static final String TYPESTRING = "coco_annotation";
    private String content;
    
    public CocoAnnotation() { }
    
    public static boolean isCocoAnnotation(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING);
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
