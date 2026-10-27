package com.examlens.model;

import java.time.LocalDateTime;

public class SyllabusVersion {
    private long id;
    private long examId;
    private long userId;
    private int versionNumber;
    private String status = "DRAFT";
    private Long sourceDocumentId;
    private Long aiJobId;
    private String summary;
    private String warnings;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getSourceDocumentId() { return sourceDocumentId; }
    public void setSourceDocumentId(Long sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public boolean isConfirmed() { return "CONFIRMED".equals(status); }
    public boolean isDraft() { return "DRAFT".equals(status); }
}
