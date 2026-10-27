package com.examlens.model;

import java.time.LocalDateTime;
import java.util.List;

public class SourceComparison {
    private long id;
    private long examId;
    private long userId;
    private Long aiJobId;
    private String recommendationsJson;
    private String uncoveredTopics;
    private String warnings;
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
    public String getRecommendationsJson() { return recommendationsJson; }
    public void setRecommendationsJson(String recommendationsJson) { this.recommendationsJson = recommendationsJson; }
    public String getUncoveredTopics() { return uncoveredTopics; }
    public void setUncoveredTopics(String uncoveredTopics) { this.uncoveredTopics = uncoveredTopics; }
    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public List<String> getUncoveredList() { return lines(uncoveredTopics); }
    public List<String> getWarningList() { return lines(warnings); }
    public List<java.util.Map<String, Object>> getRecommendationList() { return com.examlens.util.Json.readMapList(recommendationsJson); }

    private List<String> lines(String v) {
        return com.examlens.util.Json.readStringList(v);
    }
}
