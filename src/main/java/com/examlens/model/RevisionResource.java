package com.examlens.model;

import java.time.LocalDateTime;
import java.util.List;

public class RevisionResource {
    private long id;
    private long examId;
    private long userId;
    private Long aiJobId;
    private String topicTitle;
    private Long topicId;
    private String keyPoints;
    private String flashcardsJson;
    private String recallJson;
    private String sourceReferences;
    private LocalDateTime createdAt;
    private boolean simulated;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public String getTopicTitle() { return topicTitle; }
    public void setTopicTitle(String topicTitle) { this.topicTitle = topicTitle; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getKeyPoints() { return keyPoints; }
    public void setKeyPoints(String keyPoints) { this.keyPoints = keyPoints; }
    public String getFlashcardsJson() { return flashcardsJson; }
    public void setFlashcardsJson(String flashcardsJson) { this.flashcardsJson = flashcardsJson; }
    public String getRecallJson() { return recallJson; }
    public void setRecallJson(String recallJson) { this.recallJson = recallJson; }
    public String getSourceReferences() { return sourceReferences; }
    public void setSourceReferences(String sourceReferences) { this.sourceReferences = sourceReferences; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public List<String> getKeyPointList() {
        if (keyPoints == null || keyPoints.isBlank()) return List.of();
        return keyPoints.lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public List<Flashcard> getFlashcardList() {
        if (flashcardsJson == null || flashcardsJson.isBlank()) return List.of();
        try {
            return Json().readValue(flashcardsJson, new com.fasterxml.jackson.core.type.TypeReference<List<Flashcard>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<RecallItem> getRecallList() {
        if (recallJson == null || recallJson.isBlank()) return List.of();
        try {
            return Json().readValue(recallJson, new com.fasterxml.jackson.core.type.TypeReference<List<RecallItem>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private static com.fasterxml.jackson.databind.ObjectMapper Json() {
        return com.examlens.util.Json.mapper();
    }

    public static class Flashcard {
        private String front;
        private String back;

        public Flashcard() {
        }

        public Flashcard(String front, String back) {
            this.front = front;
            this.back = back;
        }

        public String getFront() { return front; }
        public void setFront(String front) { this.front = front; }
        public String getBack() { return back; }
        public void setBack(String back) { this.back = back; }
    }

    public static class RecallItem {
        private String question;
        private String answer;

        public RecallItem() {
        }

        public RecallItem(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
    }
}
