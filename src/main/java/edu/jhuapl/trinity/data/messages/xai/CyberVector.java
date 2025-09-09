package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.jhuapl.trinity.data.messages.MessageData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CyberVector extends MessageData {

    public static final String TYPESTRING = "cybervector";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
      "S(GT, A)": {
        "Image Count": 0.7391304347826086,
        "Image Distribution": 0.9869558477855777,
        "Pod Count": 0.20440251572327045,
        "Service Intersection": 1.0,
        "Namespace Intersection” : 0.5,
        "Namespaces Per Pod Distribution” : 0.5,
        "Namespace Count” : 0.5,
        "Role Count” : 0.5,
        "Role Name Intersection” : 0.5,
        "Resource Count” : 0.5,
        "Resource Name Intersection” : 0.5,
        "Role Permissions Intersection” : 0.5,
        "Role Resource Distribution” : 0.5,
        "Role Permissions Distribution” : 0.5,
        "Namespace Per User Distribution” : 0.5,
      },
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    @JsonProperty("Image Count")
    private double imageCount;

    @JsonProperty("Image Distribution")
    private double imageDistribution;

    @JsonProperty("Pod Count")
    private double podCount;

    @JsonProperty("Service Intersection")
    private double serviceIntersection;

    @JsonProperty("Namespace Intersection")
    private double namespaceIntersection;

    @JsonProperty("Namespaces Per Pod Distribution")
    private double namespacesPerPodDistribution;

    @JsonProperty("Namespace Count")
    private double namespaceCount;

    @JsonProperty("Role Count")
    private double roleCount;

    @JsonProperty("Role Name Intersection")
    private double roleNameIntersection;

    @JsonProperty("Resource Count")
    private double resourceCount;

    @JsonProperty("Resource Name Intersection")
    private double resourceNameIntersection;

    @JsonProperty("Role Permissions Intersection")
    private double rolePermissionsIntersection;

    @JsonProperty("Role Resource Distribution")
    private double roleResourceDistribution;

    @JsonProperty("Role Permissions Distribution")
    private double rolePermissionsDistribution;

    @JsonProperty("Namespace Per User Distribution")
    private double namespacePerUserDistribution;

    //</editor-fold>

    public CyberVector() {
        this.messageType = TYPESTRING;
//        entityId = UUID.randomUUID().toString();
//        metaData.put("uuid", entityId);
    }

//
//    public static Function<CyberVector, double[]> mapToStateArray = (state) -> {
//        double[] states = new double[state.data.size()];
//        for (int i = 0; i < states.length; i++) {
//            states[i] = state.data.get(i);
//        }
//        return states;
//    };

    //<editor-fold defaultstate="collapsed" desc="Properties">

    public double getImageCount() { return imageCount; }
    public void setImageCount(double imageCount) { this.imageCount = imageCount; }

    public double getImageDistribution() { return imageDistribution; }
    public void setImageDistribution(double imageDistribution) { this.imageDistribution = imageDistribution; }

    public double getPodCount() { return podCount; }
    public void setPodCount(double podCount) { this.podCount = podCount; }

    public double getServiceIntersection() { return serviceIntersection; }
    public void setServiceIntersection(double serviceIntersection) { this.serviceIntersection = serviceIntersection; }

    public double getNamespaceIntersection() { return namespaceIntersection; }
    public void setNamespaceIntersection(double namespaceIntersection) { this.namespaceIntersection = namespaceIntersection; }

    public double getNamespacesPerPodDistribution() { return namespacesPerPodDistribution; }
    public void setNamespacesPerPodDistribution(double namespacesPerPodDistribution) { this.namespacesPerPodDistribution = namespacesPerPodDistribution; }

    public double getNamespaceCount() { return namespaceCount; }
    public void setNamespaceCount(double namespaceCount) { this.namespaceCount = namespaceCount; }

    public double getRoleCount() { return roleCount; }
    public void setRoleCount(double roleCount) { this.roleCount = roleCount; }

    public double getRoleNameIntersection() { return roleNameIntersection; }
    public void setRoleNameIntersection(double roleNameIntersection) { this.roleNameIntersection = roleNameIntersection; }

    public double getResourceCount() { return resourceCount; }
    public void setResourceCount(double resourceCount) { this.resourceCount = resourceCount; }

    public double getResourceNameIntersection() { return resourceNameIntersection; }
    public void setResourceNameIntersection(double resourceNameIntersection) { this.resourceNameIntersection = resourceNameIntersection; }

    public double getRolePermissionsIntersection() { return rolePermissionsIntersection; }
    public void setRolePermissionsIntersection(double rolePermissionsIntersection) { this.rolePermissionsIntersection = rolePermissionsIntersection; }

    public double getRoleResourceDistribution() { return roleResourceDistribution; }
    public void setRoleResourceDistribution(double roleResourceDistribution) { this.roleResourceDistribution = roleResourceDistribution; }

    public double getRolePermissionsDistribution() { return rolePermissionsDistribution; }
    public void setRolePermissionsDistribution(double rolePermissionsDistribution) { this.rolePermissionsDistribution = rolePermissionsDistribution; }

    public double getNamespacePerUserDistribution() { return namespacePerUserDistribution; }
    public void setNamespacePerUserDistribution(double namespacePerUserDistribution) { this.namespacePerUserDistribution = namespacePerUserDistribution; }
    //</editor-fold>

}
