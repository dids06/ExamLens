package com.examlens.model;

public class SyllabusClassification {
    private long id;
    private long syllabusVersionId;
    private String blockKey;
    private Integer pageNumber;
    private String originalText;
    private String classification;
    private Double confidence;
    private String normalizedTopic;
    private String parentUnit;
    private String reason;
    private boolean included;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getSyllabusVersionId() { return syllabusVersionId; }
    public void setSyllabusVersionId(long syllabusVersionId) { this.syllabusVersionId = syllabusVersionId; }
    public String getBlockKey() { return blockKey; }
    public void setBlockKey(String blockKey) { this.blockKey = blockKey; }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getNormalizedTopic() { return normalizedTopic; }
    public void setNormalizedTopic(String normalizedTopic) { this.normalizedTopic = normalizedTopic; }
    public String getParentUnit() { return parentUnit; }
    public void setParentUnit(String parentUnit) { this.parentUnit = parentUnit; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isIncluded() { return included; }
    public void setIncluded(boolean included) { this.included = included; }

    public String getClassificationLabel() {
        if (classification == null) return "Unclassified";
        return switch (classification) {
            case "SYLLABUS_TOPIC" -> "Syllabus topic";
            case "SYLLABUS_SUBTOPIC" -> "Subtopic";
            case "COURSE_OUTCOME" -> "Course outcome";
            case "TEACHER_INSTRUCTION" -> "Teacher instruction";
            case "EXAM_PATTERN" -> "Exam pattern";
            case "MARKS_DISTRIBUTION" -> "Marks distribution";
            case "REFERENCE_MATERIAL" -> "Reference material";
            case "ADMINISTRATIVE_INFORMATION" -> "Administrative";
            case "PAGE_HEADER_OR_FOOTER" -> "Header/footer";
            case "PAGE_NUMBER" -> "Page number";
            case "WATERMARK" -> "Watermark";
            case "OCR_NOISE" -> "OCR noise";
            case "UNRELATED_CONTENT" -> "Unrelated content";
            case "UNCERTAIN" -> "Uncertain";
            default -> classification.replace('_', ' ').toLowerCase();
        };
    }

    public String getConfidenceLabel() {
        if (confidence == null) return "No confidence estimate";
        if (confidence >= 0.85) return "High confidence";
        if (confidence >= 0.6) return "Medium confidence";
        return "Low confidence — review";
    }
}
