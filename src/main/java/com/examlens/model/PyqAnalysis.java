package com.examlens.model;

import java.time.LocalDateTime;
import java.util.List;

public class PyqAnalysis {
    private long id;
    private long examId;
    private long userId;
    private Long aiJobId;
    private String topicFrequencyJson;
    private String recurringPatterns;
    private String unmatchedQuestions;
    private String limitations;
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
    public String getTopicFrequencyJson() { return topicFrequencyJson; }
    public void setTopicFrequencyJson(String topicFrequencyJson) { this.topicFrequencyJson = topicFrequencyJson; }
    public String getRecurringPatterns() { return recurringPatterns; }
    public void setRecurringPatterns(String recurringPatterns) { this.recurringPatterns = recurringPatterns; }
    public String getUnmatchedQuestions() { return unmatchedQuestions; }
    public void setUnmatchedQuestions(String unmatchedQuestions) { this.unmatchedQuestions = unmatchedQuestions; }
    public String getLimitations() { return limitations; }
    public void setLimitations(String limitations) { this.limitations = limitations; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public List<String> getPatternList() { return lines(recurringPatterns); }
    public List<String> getUnmatchedList() { return lines(unmatchedQuestions); }
    public List<String> getLimitationList() { return lines(limitations); }
    public List<java.util.Map<String, Object>> getTopicFrequencyList() { return com.examlens.util.Json.readMapList(topicFrequencyJson); }

    private List<String> lines(String v) {
        return com.examlens.util.Json.readStringList(v);
    }
}
