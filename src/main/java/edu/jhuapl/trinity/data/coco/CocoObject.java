package edu.jhuapl.trinity.data.coco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sean phillips
 * Based on https://cocodataset.org/#format-data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CocoObject {
    public static final String TYPESTRING = "CocoObject";
    private CocoInfo info;
    private List<CocoLicense> licenses;
    private List<CocoImage> images;
    private List<CocoAnnotation> annotations;
    private List<CocoCategory> categories;

    public CocoObject() {
        licenses = new ArrayList<>();
        images = new ArrayList<>();
        annotations = new ArrayList<>();
        categories = new ArrayList<>();
    }

    public static boolean isCocoObject(String messageBody) {
        return messageBody.contains("categories")
            && messageBody.contains("images")
            && messageBody.contains("annotations");
    }

    /**
     * @return the info
     */
    public CocoInfo getInfo() {
        return info;
    }

    /**
     * @param info the info to set
     */
    public void setInfo(CocoInfo info) {
        this.info = info;
    }

    /**
     * @return the licenses
     */
    public List<CocoLicense> getLicenses() {
        return licenses;
    }

    /**
     * @param licenses the licenses to set
     */
    public void setLicenses(List<CocoLicense> licenses) {
        this.licenses = licenses;
    }

    /**
     * @return the images
     */
    public List<CocoImage> getImages() {
        return images;
    }

    /**
     * @param images the images to set
     */
    public void setImages(List<CocoImage> images) {
        this.images = images;
    }

    /**
     * @return the annotations
     */
    public List<CocoAnnotation> getAnnotations() {
        return annotations;
    }

    /**
     * @param annotations the annotations to set
     */
    public void setAnnotations(List<CocoAnnotation> annotations) {
        this.annotations = annotations;
    }

    /**
     * @return the categories
     */
    public List<CocoCategory> getCategories() {
        return categories;
    }

    /**
     * @param categories the categories to set
     */
    public void setCategories(List<CocoCategory> categories) {
        this.categories = categories;
    }
}
