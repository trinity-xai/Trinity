package edu.jhuapl.trinity.data.coco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sean phillips
 * Based on https://cocodataset.org/#format-data 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CocoAnnotation {
    
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    //    {
    //         "id": 125686,
    //         "category_id": 2,
    //         "iscrowd": 0,
    //         "segmentation": [[164.81, 417.51, 164.81, 417.51, 164.81, 417.51, 159.31, 
    //            447.73, 241.72, 438.11, 226.61, 425.75, 226.61, 420.26, 210.13, 413.39, 
    //            206.01, 413.39, 197.77, 414.76, 167.55, 410.64]],
    //         "image_id": 242287,
    //         "area": 42061.80340000001,
    //         "bbox": [19.23, 383.18, 314.5, 244.46]
    //    }
    //</editor-fold>
    
    public static final String TYPESTRING = "CocoAnnotation";
    
    //<editor-fold defaultstate="collapsed" desc="Payload Fields">    
    private long id;
    private int image_id;
    private int category_id;
    private List<List<Double>> segmentation;
    private double area;
    private List<Double> bbox; //[0.0, 0, 16.0, 0]
    private boolean iscrowd;
    //</editor-fold>
    
    public CocoAnnotation() {
        segmentation = new ArrayList<>();
        bbox = new ArrayList<>();
    }

    public String bboxToString() {
        NumberFormat format = new DecimalFormat("0.00");
        StringBuilder sb = new StringBuilder("[ ");
        for (Double d : getBbox()) {
            sb.append(format.format(d));
            sb.append(" ");
        }
        sb.append("]");
        String bboxStr = sb.toString();
        return bboxStr;
    }    
    public boolean isBBoxValid() {
        return null != getBbox() && !getBbox().isEmpty() && getBbox().size() > 3
            && getBbox().get(2) > 0.0 && getBbox().get(3) > 0.0;
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
     * @return the image_id
     */
    public int getImage_id() {
        return image_id;
    }

    /**
     * @param image_id the image_id to set
     */
    public void setImage_id(int image_id) {
        this.image_id = image_id;
    }

    /**
     * @return the category_id
     */
    public int getCategory_id() {
        return category_id;
    }

    /**
     * @param category_id the category_id to set
     */
    public void setCategory_id(int category_id) {
        this.category_id = category_id;
    }

    /**
     * @return the segmentation
     */
    public List<List<Double>> getSegmentation() {
        return segmentation;
    }

    /**
     * @param segmentation the segmentation to set
     */
    public void setSegmentation(List<List<Double>> segmentation) {
        this.segmentation = segmentation;
    }

    /**
     * @return the area
     */
    public double getArea() {
        return area;
    }

    /**
     * @param area the area to set
     */
    public void setArea(double area) {
        this.area = area;
    }

    /**
     * @return the bbox
     */
    public List<Double> getBbox() {
        return bbox;
    }

    /**
     * @param bbox the bbox to set
     */
    public void setBbox(List<Double> bbox) {
        this.bbox = bbox;
    }

    /**
     * @return the iscrowd
     */
    public boolean isIscrowd() {
        return iscrowd;
    }

    /**
     * @param iscrowd the iscrowd to set
     */
    public void setIscrowd(boolean iscrowd) {
        this.iscrowd = iscrowd;
    }
    //</editor-fold>
}
