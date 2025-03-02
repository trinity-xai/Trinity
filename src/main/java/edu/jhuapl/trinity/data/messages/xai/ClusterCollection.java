/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterCollection extends MessageData {

    public static final String TYPESTRING = "ClusterCollection";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {   "type": "ClusterCollection",
        "clusters": [
            {
                "messageType": "point_cluster",
                "clusterName": "some_optional name",
                "clusterId": 1,
                "map": [3, 7, 9, 14, 19, 27, 330, 3000, 9001 ],
                "data": [[-0.4232191175986961, -0.5031443592838056, 0.30497221256679125],
                    [0.4157550505277444, -0.46174460409303325, -0.12950797668733202],
                    [0.6323170694189965, 0.20112482321095512, -0.0770502704073328],
                    [-0.018055872253030292, 0.08990142832758276, 0.14425006537440124],
                    [-0.30635162612534406, -0.04588245408916634, 0.37569343542885386],
                    [-0.15806087484089912, 0.0673835604499377, 0.17998157972463474],
                    [-0.01611924502337739, -0.24604972532815875, -0.01560032825534631],
                    [0.3946917081600188, -0.12442754943149016, -0.18011471923351433],
                    [-0.04629288711207645, 0.5376945006956948, 0.18828509670172092]
                ]
            },
                {
                "messageType": "point_cluster",
                "clusterName": "some_optional name",
                "clusterId": 1,
                "map": [3, 7, 9, 14, 19, 27, 330, 3000, 9001 ],
                "data": [[-0.4232191175986961, -0.5031443592838056, 0.30497221256679125],
                    [0.4157550505277444, -0.46174460409303325, -0.12950797668733202],
                    [0.6323170694189965, 0.20112482321095512, -0.0770502704073328],
                    [-0.018055872253030292, 0.08990142832758276, 0.14425006537440124],
                    [-0.30635162612534406, -0.04588245408916634, 0.37569343542885386],
                    [-0.15806087484089912, 0.0673835604499377, 0.17998157972463474],
                    [-0.01611924502337739, -0.24604972532815875, -0.01560032825534631],
                    [0.3946917081600188, -0.12442754943149016, -0.18011471923351433],
                    [-0.04629288711207645, 0.5376945006956948, 0.18828509670172092]
                ]
            },
                {
                "messageType": "point_cluster",
                "clusterName": "some_optional name",
                "clusterId": 1,
                "map": [3, 7, 9, 14, 19, 27, 330, 3000, 9001 ],
                "data": [[-0.4232191175986961, -0.5031443592838056, 0.30497221256679125],
                    [0.4157550505277444, -0.46174460409303325, -0.12950797668733202],
                    [0.6323170694189965, 0.20112482321095512, -0.0770502704073328],
                    [-0.018055872253030292, 0.08990142832758276, 0.14425006537440124],
                    [-0.30635162612534406, -0.04588245408916634, 0.37569343542885386],
                    [-0.15806087484089912, 0.0673835604499377, 0.17998157972463474],
                    [-0.01611924502337739, -0.24604972532815875, -0.01560032825534631],
                    [0.3946917081600188, -0.12442754943149016, -0.18011471923351433],
                    [-0.04629288711207645, 0.5376945006956948, 0.18828509670172092]
                ]
            }
        ]
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String type;
    private List<PointCluster> clusters;

    //</editor-fold>

    public ClusterCollection() {
        type = TYPESTRING;
        this.clusters = new ArrayList<>();
    }

    public static boolean isClusterCollection(String messageBody) {
        return messageBody.contains("type")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("clusters");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

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
     * @return the clusters
     */
    public List<PointCluster> getClusters() {
        return clusters;
    }

    /**
     * @param clusters the clusters to set
     */
    public void setClusters(List<PointCluster> clusters) {
        this.clusters = clusters;
    }

    //</editor-fold>
}
