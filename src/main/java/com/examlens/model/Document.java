package com.examlens.model;

import java.time.LocalDateTime;

public class Document {
    private long id;
    private long userId;
    private Long examId;
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private long fileSize;
    private String sha256;
    private String documentType;
    private Integer pageCount;
    private String extractionStatus = "PENDING";
    private String extractionMethod;
    private String extractionWarnings;
    private int extractedChars;
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public String getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(String extractionStatus) { this.extractionStatus = extractionStatus; }
    public String getExtractionMethod() { return extractionMethod; }
    public void setExtractionMethod(String extractionMethod) { this.extractionMethod = extractionMethod; }
    public String getExtractionWarnings() { return extractionWarnings; }
    public void setExtractionWarnings(String extractionWarnings) { this.extractionWarnings = extractionWarnings; }
    public int getExtractedChars() { return extractedChars; }
    public void setExtractedChars(int extractedChars) { this.extractedChars = extractedChars; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSizeDisplay() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    public String getTypeLabel() {
        if (documentType == null) return "Document";
        return switch (documentType) {
            case "SYLLABUS" -> "Syllabus";
            case "TEACHER_NOTES" -> "Teacher notes";
            case "STUDY_MATERIAL" -> "Study material";
            case "PREVIOUS_PAPER" -> "Previous-year paper";
            case "QUESTION_BANK" -> "Question bank";
            default -> documentType.replace('_', ' ');
        };
    }
}
