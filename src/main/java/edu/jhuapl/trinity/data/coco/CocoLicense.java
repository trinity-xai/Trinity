package edu.jhuapl.trinity.data.coco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author sean phillips
 * Based on https://cocodataset.org/#format-data 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CocoLicense {
    public static final String TYPESTRING = "CocoLicense";
    
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    //        {
    //            "url": "http://creativecommons.org/licenses/by/2.0/",
    //            "id": 4,
    //            "name": "Attribution License"
    //        }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">    
    private String url;
    private long id;
    private String name;
    //</editor-fold>    
    
    public CocoLicense() {
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">
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

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    //</editor-fold>    
}
