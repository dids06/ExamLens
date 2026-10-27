package com.examlens.model;

import com.examlens.util.Json;

import java.time.LocalDateTime;
import java.util.List;

public class ConceptExplanation {
    private long id;
    private Long examId;
    private long userId;
    private Long aiJobId;
    private String topicTitle;
    private String question;
    private String directAnswer;
    private String explanation;
    private String examplesJson;
    private String checkJson;
    private String sourceReferences;
    private String limitations;
    private LocalDateTime createdAt;
    private boolean simulated;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public String getTopicTitle() { return topicTitle; }
    public void setTopicTitle(String topicTitle) { this.topicTitle = topicTitle; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getDirectAnswer() { return directAnswer; }
    public void setDirectAnswer(String directAnswer) { this.directAnswer = directAnswer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getExamplesJson() { return examplesJson; }
    public void setExamplesJson(String examplesJson) { this.examplesJson = examplesJson; }
    public String getCheckJson() { return checkJson; }
    public void setCheckJson(String checkJson) { this.checkJson = checkJson; }
    public String getSourceReferences() { return sourceReferences; }
    public void setSourceReferences(String sourceReferences) { this.sourceReferences = sourceReferences; }
    public String getLimitations() { return limitations; }
    public void setLimitations(String limitations) { this.limitations = limitations; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public List<String> getExampleList() { return list(examplesJson); }
    public List<String> getCheckList() { return list(checkJson); }
    public List<String> getSourceList() { return list(sourceReferences); }
    public List<String> getLimitationList() { return list(limitations); }

    private List<String> list(String v) {
        return Json.readStringList(v);
    }
}
