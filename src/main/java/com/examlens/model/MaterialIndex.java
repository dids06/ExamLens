package com.examlens.model;

import java.time.LocalDateTime;
import java.util.List;

public class MaterialIndex {
    private long id;
    private long examId;
    private long userId;
    private long documentId;
    private Long aiJobId;
    private String sectionsJson;
    private String unmappedTopics;
    private String warnings;
    private LocalDateTime createdAt;
    private String documentName;
    private boolean simulated;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getDocumentId() { return documentId; }
    public void setDocumentId(long documentId) { this.documentId = documentId; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public String getSectionsJson() { return sectionsJson; }
    public void setSectionsJson(String sectionsJson) { this.sectionsJson = sectionsJson; }
    public String getUnmappedTopics() { return unmappedTopics; }
    public void setUnmappedTopics(String unmappedTopics) { this.unmappedTopics = unmappedTopics; }
    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public List<String> getUnmappedList() { return lines(unmappedTopics); }
    public List<String> getWarningList() { return lines(warnings); }
    public List<java.util.Map<String, Object>> getSectionList() { return com.examlens.util.Json.readMapList(sectionsJson); }

    private List<String> lines(String v) {
        return com.examlens.util.Json.readStringList(v);
    }
}
