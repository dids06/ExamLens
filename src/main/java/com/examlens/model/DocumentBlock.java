package com.examlens.model;

import java.time.LocalDateTime;

public class DocumentBlock {
    private long id;
    private long documentId;
    private String blockKey;
    private Integer pageNumber;
    private String heading;
    private String blockText;
    private String extractionMethod;
    private Double confidence;
    private boolean uncertain;
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getDocumentId() { return documentId; }
    public void setDocumentId(long documentId) { this.documentId = documentId; }
    public String getBlockKey() { return blockKey; }
    public void setBlockKey(String blockKey) { this.blockKey = blockKey; }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }
    public String getBlockText() { return blockText; }
    public void setBlockText(String blockText) { this.blockText = blockText; }
    public String getExtractionMethod() { return extractionMethod; }
    public void setExtractionMethod(String extractionMethod) { this.extractionMethod = extractionMethod; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public boolean isUncertain() { return uncertain; }
    public void setUncertain(boolean uncertain) { this.uncertain = uncertain; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPageLabel() {
        return pageNumber == null ? "no page" : "page " + pageNumber;
    }
}
