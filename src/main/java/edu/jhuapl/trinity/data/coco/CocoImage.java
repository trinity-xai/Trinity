package edu.jhuapl.trinity.data.coco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author sean phillips
 * Based on https://cocodataset.org/#format-data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CocoImage {
    public static final String TYPESTRING = "CocoImage";

    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    //    {
    //        "id": 242287,
    //        "license": 4,
    //        "coco_url": "http://images.cocodataset.org/val2017/000000242287.jpg",
    //        "flickr_url": "http://farm3.staticflickr.com/2626/4072194513_edb6acfb2b_z.jpg",
    //        "path" : "/datasets/coco/000000242287.jpg"
    //        "width": 426,
    //        "height": 640,
    //        "file_name": "000000242287.jpg",
    //        "date_captured": "2013-11-15 02:41:42"
    //    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private long id;
    private int license;
    private String coco_url;
    private String flickr_url;
    private int width;
    private int height;
    private String path;
    private String file_name;
    private String date_captured;
    //</editor-fold>

    public CocoImage() {
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

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
     * @return the license
     */
    public int getLicense() {
        return license;
    }

    /**
     * @param license the license to set
     */
    public void setLicense(int license) {
        this.license = license;
    }

    /**
     * @return the coco_url
     */
    public String getCoco_url() {
        return coco_url;
    }

    /**
     * @param coco_url the coco_url to set
     */
    public void setCoco_url(String coco_url) {
        this.coco_url = coco_url;
    }

    /**
     * @return the flickr_url
     */
    public String getFlickr_url() {
        return flickr_url;
    }

    /**
     * @param flickr_url the flickr_url to set
     */
    public void setFlickr_url(String flickr_url) {
        this.flickr_url = flickr_url;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the file_name
     */
    public String getFile_name() {
        return file_name;
    }

    /**
     * @param file_name the file_name to set
     */
    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    /**
     * @return the date_captured
     */
    public String getDate_captured() {
        return date_captured;
    }

    /**
     * @param date_captured the date_captured to set
     */
    public void setDate_captured(String date_captured) {
        this.date_captured = date_captured;
    }
    //</editor-fold>
}
