package com.examlens.model;

import java.time.LocalDateTime;
import java.util.List;

public class PreviousPaper {
    private long id;
    private long examId;
    private long userId;
    private long documentId;
    private String paperLabel;
    private String examYear;
    private Integer totalMarks;
    private LocalDateTime createdAt;
    private String documentName;
    private int questionCount;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getDocumentId() { return documentId; }
    public void setDocumentId(long documentId) { this.documentId = documentId; }
    public String getPaperLabel() { return paperLabel; }
    public void setPaperLabel(String paperLabel) { this.paperLabel = paperLabel; }
    public String getExamYear() { return examYear; }
    public void setExamYear(String examYear) { this.examYear = examYear; }
    public Integer getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Integer totalMarks) { this.totalMarks = totalMarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
}
