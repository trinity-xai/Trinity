package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingsImageUrl {

    public static final String TYPESTRING = "embeddingImageInput";
    public static final String BASE64_PREFIX_PNG = "data:image/png;base64,";
    public static final String BASE64_PREFIX_JPEG = "data:image/jpegng;base64,";
    public static final String BASE64_PREFIX_WEBP = "data:image/webp;base64,";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {"type":"image_url","image_url": {"url": "data:image/png;base64,image1bytesencoded64"}},
    {"type":"image_url","image_url": {"url": "data:image/jpeg;base64,image2bytesencoded64"}},
    {"type":"image_url","image_url": {"url": "data:image/webp;base64,image3bytesencoded64"}}
     */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String type;
    private ImageUrl image_url;
    //</editor-fold>

    public EmbeddingsImageUrl() {
        type = "image_url";
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
