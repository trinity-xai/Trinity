package edu.jhuapl.trinity.data.graph.semantics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Optional typed meaning of an existing GraphRelationship. The edge still owns direction. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphRelationshipSemantics {
    private String predicateId;
    private String hierarchyScopeId;
    private GraphAssertionOrigin origin;
    private List<String> evidenceIds;

    public GraphRelationshipSemantics() { }

    public String getPredicateId() { return predicateId; }
    public void setPredicateId(String predicateId) { this.predicateId = predicateId; }
    public String getHierarchyScopeId() { return hierarchyScopeId; }
    public void setHierarchyScopeId(String hierarchyScopeId) { this.hierarchyScopeId = hierarchyScopeId; }
    public GraphAssertionOrigin getOrigin() { return origin; }
    public void setOrigin(GraphAssertionOrigin origin) { this.origin = origin; }
    public List<String> getEvidenceIds() { return evidenceIds; }
    public void setEvidenceIds(List<String> evidenceIds) { this.evidenceIds = evidenceIds; }
}
