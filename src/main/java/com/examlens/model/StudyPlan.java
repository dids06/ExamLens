package com.examlens.model;

public class StudyPlan {
    private long id;
    private long examId;
    private long userId;
    private Long activeVersionId;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public Long getActiveVersionId() { return activeVersionId; }
    public void setActiveVersionId(Long activeVersionId) { this.activeVersionId = activeVersionId; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
