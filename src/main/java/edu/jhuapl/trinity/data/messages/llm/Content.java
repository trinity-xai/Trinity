package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
          {
            "type": "text",
            "text": "What is in this image?"
          },
          {
            "type": "image_url",
            "image_url": {
              "url": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg"
            }
          }
     */
    //</editor-fold>
    public enum TYPE_ENUM {text, image_url}

    private String type; //required type field
    private String text; //optional prompt text
    private ImageUrl image_url; //optional image_url

    public Content() {
    }

    public void setTypeByEnum(TYPE_ENUM type_enum) {
        setType(type_enum.name());
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

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
     * @return the image_url
     */
    public ImageUrl getImage_url() {
        return image_url;
    }

    /**
     * @param image_url the image_url to set
     */
    public void setImage_url(ImageUrl image_url) {
        this.image_url = image_url;
    }
}
