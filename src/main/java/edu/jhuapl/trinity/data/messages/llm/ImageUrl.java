package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageUrl {
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        "image_url": {"url": "data:image/png;base64,image1bytesencoded64"}},
     */
    //</editor-fold>

    private String url; //base64 image to get embeddings for

    public ImageUrl() {
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

}
