package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CyberReport {
    public static final String GROUNDTRUTH    = "Ground Truth";
    public static final String INFERENCES     = "Inferences";
    public static final String ADJACENTNETWORK= "Adjacent Network";
    public static final String MOD            = "Mod";
    public static final String SGTA           = "S(GT, A)";
    public static final String SINTELGT       = "S(intel, GT)";
    public static final String SINTELA        = "S(intel, A)";
    public static final String SINFGT         = "S(inf, GT)";
    public static final String DELTA          = "delta";

    // ---- small type for "Mod": [[10,"pod"], ...] ----
    @JsonFormat(shape = JsonFormat.Shape.ARRAY) // maps [10,"pod"] -> new ModEntry(10,"pod")
    public static record ModEntry(int value, String label) {}

    // ----- simple metadata -----
    @JsonProperty(GROUNDTRUTH)
    private String groundTruth;

    @JsonProperty(ADJACENTNETWORK)
    private String adjacentNetwork;

    @JsonProperty(INFERENCES)
    private List<String> inferences = new ArrayList<>();

    @JsonProperty(MOD)
    private List<ModEntry> mod = new ArrayList<>();

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

    // ----- catch-all for any other S(... ) vectors -----
    @JsonIgnore // avoid double-serializing; we expose via @JsonAnyGetter below
    private final Map<String, CyberVector> extraVectors = new LinkedHashMap<>();

    public static boolean isCyberReport(String body) {
        return body != null
            && body.contains(GROUNDTRUTH)
            && body.contains(ADJACENTNETWORK)
            && body.contains(INFERENCES);
    }

    /**
     * Any unrecognized property will land here. Jackson will deserialize the value
     * into a CyberVector because of the method signature.
     */
    @JsonAnySetter
    public void putUnknown(String name, CyberVector vector) {
        // Only stash S(...) style keys that aren’t already bound to explicit fields
        if (name != null && name.startsWith("S(")) {
            extraVectors.put(name, vector);
        }
    }

    /** When serializing back to JSON, include the extra S(...) vectors naturally. */
    @JsonAnyGetter
    public Map<String, CyberVector> getExtraVectors() {
        return extraVectors;
    }

    // ----- convenience: expose all vectors (known + extra) as a collection -----
    @JsonIgnore
    public List<CyberVector> getAllVectors() {
        List<CyberVector> all = new ArrayList<>();
        if (sGtA != null)     all.add(sGtA);
        if (sIntelGt != null) all.add(sIntelGt);
        if (sIntelA != null)  all.add(sIntelA);
        if (sInfGt != null)   all.add(sInfGt);
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

    public List<ModEntry> getMod() { return mod; }
    public void setMod(List<ModEntry> mod) { this.mod = mod; }

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
