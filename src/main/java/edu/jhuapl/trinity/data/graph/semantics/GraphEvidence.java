package edu.jhuapl.trinity.data.graph.semantics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * An independently addressable reference to supporting source material.
 * Offsets, when present, are [startOffset, endOffset) in the explicitly named
 * spanCoordinateSystem and the stated document revision. Never infer them from
 * a PDF or another source when extraction did not retain the relevant spans.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphEvidence {
    private String evidenceId;
    private String documentId;
    private String documentRevision;
    private String passageId;
    private Integer pageNumber; // 1-based, optional
    private String sectionId;
    private Long startOffset; // 0-based, inclusive
    private Long endOffset;   // exclusive
    private String spanCoordinateSystem;
    private String quote;
    private String extractionRunId;
    private String extractionMethod;
    private Double extractionScore;
    private String reviewStatus;

    public GraphEvidence() { }

    public String getEvidenceId() { return evidenceId; }
    public void setEvidenceId(String evidenceId) { this.evidenceId = evidenceId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getDocumentRevision() { return documentRevision; }
    public void setDocumentRevision(String documentRevision) { this.documentRevision = documentRevision; }
    public String getPassageId() { return passageId; }
    public void setPassageId(String passageId) { this.passageId = passageId; }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }
    public Long getStartOffset() { return startOffset; }
    public void setStartOffset(Long startOffset) { this.startOffset = startOffset; }
    public Long getEndOffset() { return endOffset; }
    public void setEndOffset(Long endOffset) { this.endOffset = endOffset; }
    public String getSpanCoordinateSystem() { return spanCoordinateSystem; }
    public void setSpanCoordinateSystem(String spanCoordinateSystem) { this.spanCoordinateSystem = spanCoordinateSystem; }
    public String getQuote() { return quote; }
    public void setQuote(String quote) { this.quote = quote; }
    public String getExtractionRunId() { return extractionRunId; }
    public void setExtractionRunId(String extractionRunId) { this.extractionRunId = extractionRunId; }
    public String getExtractionMethod() { return extractionMethod; }
    public void setExtractionMethod(String extractionMethod) { this.extractionMethod = extractionMethod; }
    public Double getExtractionScore() { return extractionScore; }
    public void setExtractionScore(Double extractionScore) { this.extractionScore = extractionScore; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
}
