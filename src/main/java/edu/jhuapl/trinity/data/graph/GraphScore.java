package edu.jhuapl.trinity.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.ArrayList;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphScore extends MessageData {

    public static final String TYPESTRING = "graphscore";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {
        "p": {
            "start": {
                "identity": 12525037,
                "labels": [
                      "Document"
                    ],
                "properties": {
                    "name": "7630a867296188c0566d0a73",
                    "numberHashes": 276
                }
            },
            "end": {
                "identity": 12487335,
                "labels": [
                      "Document"
                ],
                "properties": {
                    "name": "c735cd9a7620c80fc53de855",
                    "numberHashes": 276
                }
            },
            "segments": [{
                "start": {
                    "identity": 12525037,
                    "labels": [
                        "Document"
                    ],
                    "properties": {
                        "name": "7630a867296188c0566d0a73",
                        "numberHashes": 276
                    }
                },
                "relationship": {
                    "identity": 32684,
                    "start": 12525037,
                    "end": 12487335,
                    "type": "SIMILAR",
                    "properties": {
                        "jaccard": 1.0
                    }
                },
                "end": {
                    "identity": 12487335,
                    "labels": [
                        "Document"
                    ],
                    "properties": {
                        "name": "c735cd9a7620c80fc53de855",
                        "numberHashes": 276
                    }
                }
            }],
            "length": 1.0
        }
      }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private GraphNode start;
    private GraphNode end;
    private ArrayList<GraphSegment> segments;
    private double length;
    //</editor-fold>

    public GraphScore() {
        this.messageType = TYPESTRING;
    }

    public static boolean isGraphScore(String messageBody) {
        return messageBody.contains("start")
            && messageBody.contains("identity")
            && messageBody.contains("segments")
            && messageBody.contains("length");
    }
}
