package edu.jhuapl.trinity.data.coco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author sean phillips
 * Based on https://cocodataset.org/#format-data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CocoCategory {

    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    //    {
    //        "supercategory": "vehicle",
    //        "id": 2,
    //        "name": "bicycle"
    //    }
    //</editor-fold>

    public static final String TYPESTRING = "CocoAnnotation";

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String supercategory;
    private long id;
    private String name;
    private String color; //non standard but common
    //</editor-fold>

    public CocoCategory() {
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the supercategory
     */
    public String getSupercategory() {
        return supercategory;
    }

    /**
     * @param supercategory the supercategory to set
     */
    public void setSupercategory(String supercategory) {
        this.supercategory = supercategory;
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

    /**
     * @return the color
     */
    public String getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(String color) {
        this.color = color;
    }
    //</editor-fold>
}
