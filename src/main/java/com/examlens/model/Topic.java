package com.examlens.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Topic {
    private long id;
    private long syllabusVersionId;
    private long examId;
    private Long parentTopicId;
    private String unitName;
    private String title;
    private String subtopics;
    private String learningOutcomes;
    private String importance;
    private String difficulty;
    private Integer estimatedMinutes;
    private String sourcePages;
    private String confidence;
    private boolean needsReview;
    private String aiReason;
    private int displayOrder;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getSyllabusVersionId() { return syllabusVersionId; }
    public void setSyllabusVersionId(long syllabusVersionId) { this.syllabusVersionId = syllabusVersionId; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public Long getParentTopicId() { return parentTopicId; }
    public void setParentTopicId(Long parentTopicId) { this.parentTopicId = parentTopicId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtopics() { return subtopics; }
    public void setSubtopics(String subtopics) { this.subtopics = subtopics; }
    public String getLearningOutcomes() { return learningOutcomes; }
    public void setLearningOutcomes(String learningOutcomes) { this.learningOutcomes = learningOutcomes; }
    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public String getSourcePages() { return sourcePages; }
    public void setSourcePages(String sourcePages) { this.sourcePages = sourcePages; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public boolean isNeedsReview() { return needsReview; }
    public void setNeedsReview(boolean needsReview) { this.needsReview = needsReview; }
    public String getAiReason() { return aiReason; }
    public void setAiReason(String aiReason) { this.aiReason = aiReason; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public List<String> getSubtopicList() {
        if (subtopics == null || subtopics.isBlank()) {
            return List.of();
        }
        return Arrays.stream(subtopics.split("\\r?\\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public List<String> getOutcomeList() {
        if (learningOutcomes == null || learningOutcomes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(learningOutcomes.split("\\r?\\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public List<String> getSourcePageList() {
        List<String> out = new ArrayList<>();
        if (sourcePages == null || sourcePages.isBlank()) {
            return out;
        }
        for (String part : sourcePages.split(",")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
    }

    public String getImportanceLabel() {
        if (importance == null) return "Unrated";
        return switch (importance) {
            case "HIGH" -> "High priority";
            case "MEDIUM" -> "Medium priority";
            case "LOW" -> "Low priority";
            default -> importance;
        };
    }

    public String getDifficultyLabel() {
        if (difficulty == null) return "Unknown difficulty";
        return switch (difficulty) {
            case "HARD" -> "Demanding";
            case "MEDIUM" -> "Moderate";
            case "EASY" -> "Approachable";
            default -> difficulty;
        };
    }
}
