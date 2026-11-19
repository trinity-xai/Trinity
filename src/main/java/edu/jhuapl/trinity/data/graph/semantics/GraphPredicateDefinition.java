package edu.jhuapl.trinity.data.graph.semantics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Predicate definition in subject (GraphEdge.startID) -> object (GraphEdge.endID)
 * order. Optional constraints are metadata for semantic validation and future
 * scoped hierarchy indexing; they do not modify the source edge set.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphPredicateDefinition {
    private String predicateId;
    private GraphPredicateFamily family;
    private String subjectRole;
    private String objectRole;
    private Boolean symmetric;
    private Boolean hierarchy;
    // Optional in 2.3A JSON; required for newly defined indexed hierarchies.
    private GraphHierarchyOrientation hierarchyOrientation;
    private Boolean acyclic;
    private Boolean allowsMultipleParents;
    private Boolean transitive;
    private List<String> allowedSubjectNodeKinds;
    private List<String> allowedObjectNodeKinds;

    public GraphPredicateDefinition() { }

    public String getPredicateId() { return predicateId; }
    public void setPredicateId(String predicateId) { this.predicateId = predicateId; }
    public GraphPredicateFamily getFamily() { return family; }
    public void setFamily(GraphPredicateFamily family) { this.family = family; }
    public String getSubjectRole() { return subjectRole; }
    public void setSubjectRole(String subjectRole) { this.subjectRole = subjectRole; }
    public String getObjectRole() { return objectRole; }
    public void setObjectRole(String objectRole) { this.objectRole = objectRole; }
    public Boolean getSymmetric() { return symmetric; }
    public void setSymmetric(Boolean symmetric) { this.symmetric = symmetric; }
    public Boolean getHierarchy() { return hierarchy; }
    public void setHierarchy(Boolean hierarchy) { this.hierarchy = hierarchy; }
    public GraphHierarchyOrientation getHierarchyOrientation() { return hierarchyOrientation; }
    public void setHierarchyOrientation(GraphHierarchyOrientation hierarchyOrientation) { this.hierarchyOrientation = hierarchyOrientation; }
    public Boolean getAcyclic() { return acyclic; }
    public void setAcyclic(Boolean acyclic) { this.acyclic = acyclic; }
    public Boolean getAllowsMultipleParents() { return allowsMultipleParents; }
    public void setAllowsMultipleParents(Boolean allowsMultipleParents) { this.allowsMultipleParents = allowsMultipleParents; }
    public Boolean getTransitive() { return transitive; }
    public void setTransitive(Boolean transitive) { this.transitive = transitive; }
    public List<String> getAllowedSubjectNodeKinds() { return allowedSubjectNodeKinds; }
    public void setAllowedSubjectNodeKinds(List<String> allowedSubjectNodeKinds) { this.allowedSubjectNodeKinds = allowedSubjectNodeKinds; }
    public List<String> getAllowedObjectNodeKinds() { return allowedObjectNodeKinds; }
    public void setAllowedObjectNodeKinds(List<String> allowedObjectNodeKinds) { this.allowedObjectNodeKinds = allowedObjectNodeKinds; }
}
