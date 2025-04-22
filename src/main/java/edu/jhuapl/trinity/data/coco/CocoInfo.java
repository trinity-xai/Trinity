package edu.jhuapl.trinity.data.coco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author sean phillips
 * Based on https://cocodataset.org/#format-data 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CocoInfo {
    public static final String TYPESTRING = "CocoInfo";
    
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    //    "info": {
    //            "description": "COCO 2017 Dataset",
    //            "url": "http://cocodataset.org",
    //            "version": "1.0",
    //            "year": 2017,
    //            "contributor": "COCO Consortium",
    //            "date_created": "2017/09/01"
    //        }    
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">    
    private String description;
    private String url;
    private String version;
    private int year;
    private String contributor;
    private String date_created;
    //</editor-fold>    
    
    public CocoInfo() {
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
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

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the year
     */
    public int getYear() {
        return year;
    }

    /**
     * @param year the year to set
     */
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * @return the contributor
     */
    public String getContributor() {
        return contributor;
    }

    /**
     * @param contributor the contributor to set
     */
    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    /**
     * @return the date_created
     */
    public String getDate_created() {
        return date_created;
    }

    /**
     * @param date_created the date_created to set
     */
    public void setDate_created(String date_created) {
        this.date_created = date_created;
    }
//</editor-fold>
}
