package edu.jhuapl.trinity.data.graph.semantics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/** Optional record of graph construction. Values are supplied by the builder, never inferred. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphRunProvenance {
    private String runId;
    private String producer;
    private String method;
    private String sourceCollectionId;
    private List<String> sourceDocumentIds;
    private Map<String, Object> parameters;

    public GraphRunProvenance() { }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getProducer() { return producer; }
    public void setProducer(String producer) { this.producer = producer; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getSourceCollectionId() { return sourceCollectionId; }
    public void setSourceCollectionId(String sourceCollectionId) { this.sourceCollectionId = sourceCollectionId; }
    public List<String> getSourceDocumentIds() { return sourceDocumentIds; }
    public void setSourceDocumentIds(List<String> sourceDocumentIds) { this.sourceDocumentIds = sourceDocumentIds; }
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
}
