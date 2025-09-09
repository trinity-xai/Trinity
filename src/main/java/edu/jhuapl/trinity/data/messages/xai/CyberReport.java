package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CyberReport {
    public final static String GROUNDTRUTH = "Ground Truth";
    public final static String INFERENCES = "Inferences";
    public final static String ADJACENTNETWORK = "Adjacent Network";
    public final static String MOD = "Mod";
    public final static String SGTA = "S(GT, A)";
    public final static String SINTELGT = "S(intel, GT)";
    public final static String SINTELA = "S(intel, A)";
    public final static String SINFGT = "S(inf, GT)";
    public final static String DELTA = "delta";

    // ----- simple metadata -----
    @JsonProperty(GROUNDTRUTH)
    private String groundTruth;

    @JsonProperty(ADJACENTNETWORK)
    private String adjacentNetwork;

    @JsonProperty(INFERENCES)
    private List<String> inferences = new ArrayList<>();

    @JsonProperty(MOD)
    private List<String> mod = new ArrayList<>();

    // ----- score vectors (known names) -----
    @JsonProperty(SGTA)
    private CyberVector sGtA;

    @JsonProperty(SINTELGT)
    private CyberVector sIntelGt;

    @JsonProperty(SINTELA)
    private CyberVector sIntelA;

    @JsonProperty(SINFGT)
    private CyberVector sInfGt;

    // delta vector
    @JsonProperty(DELTA)
    private CyberVector delta;

    // ----- catch-all for any other S(... ) vectors so you can treat them like a list -----
    @JsonIgnore // avoid double-serializing; we expose via @JsonAnyGetter below
    private final Map<String, CyberVector> extraVectors = new LinkedHashMap<>();

    public static boolean isCyberReport(String body) {
        return body.contains("Ground Truth") && body.contains("Adjacent Network") 
            && body.contains("Inferences");        
    }
    /**
     * Any unrecognized property will land here. Jackson will deserialize the value
     * into a CyberVector because of the method signature.
     * This is handy if future files add more S(?, ?) blocks without changing this class.
     * @param name
     * @param vector
     */
    @JsonAnySetter
    public void putUnknown(String name, CyberVector vector) {
        // Only stash S(...) style keys that aren’t already bound to explicit fields
        if (name != null && name.startsWith("S(")) {
            extraVectors.put(name, vector);
        }
    }

    /**
     * When serializing back to JSON, include the extra S(...) vectors naturally.
     * @return 
     */
    @JsonAnyGetter
    public Map<String, CyberVector> getExtraVectors() {
        return extraVectors;
    }

    // ----- convenience: expose all vectors (known + extra) as a collection if you want "a list of CyberVector" -----
    @JsonIgnore
    public List<CyberVector> getAllVectors() {
        List<CyberVector> all = new ArrayList<>();
        if (sGtA != null) all.add(sGtA);
        if (sIntelGt != null) all.add(sIntelGt);
        if (sIntelA != null) all.add(sIntelA);
        if (sInfGt != null) all.add(sInfGt);
        all.addAll(extraVectors.values());
        return all;
    }

    // ----- getters/setters -----
    public String getGroundTruth() { return groundTruth; }
    public void setGroundTruth(String groundTruth) { this.groundTruth = groundTruth; }

    public String getAdjacentNetwork() { return adjacentNetwork; }
    public void setAdjacentNetwork(String adjacentNetwork) { this.adjacentNetwork = adjacentNetwork; }

    public List<String> getInferences() { return inferences; }
    public void setInferences(List<String> inferences) { this.inferences = inferences; }

    public List<String> getMod() { return mod; }
    public void setMod(List<String> mod) { this.mod = mod; }

    public CyberVector getsGtA() { return sGtA; }
    public void setsGtA(CyberVector sGtA) { this.sGtA = sGtA; }

    public CyberVector getsIntelGt() { return sIntelGt; }
    public void setsIntelGt(CyberVector sIntelGt) { this.sIntelGt = sIntelGt; }

    public CyberVector getsIntelA() { return sIntelA; }
    public void setsIntelA(CyberVector sIntelA) { this.sIntelA = sIntelA; }

    public CyberVector getsInfGt() { return sInfGt; }
    public void setsInfGt(CyberVector sInfGt) { this.sInfGt = sInfGt; }

    public CyberVector getDelta() { return delta; }
    public void setDelta(CyberVector delta) { this.delta = delta; }
}
