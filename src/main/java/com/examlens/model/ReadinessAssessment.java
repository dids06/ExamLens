package com.examlens.model;

import java.time.LocalDateTime;
import java.util.List;

public class ReadinessAssessment {
    private long id;
    private long examId;
    private long userId;
    private Long aiJobId;
    private String weakTopicsJson;
    private String strengthsJson;
    private String coverageGaps;
    private String readinessSummary;
    private String dataLimitations;
    private Double completionPercent;
    private Integer plannedMinutes;
    private Integer actualMinutes;
    private Double practiceAverage;
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
    public String getWeakTopicsJson() { return weakTopicsJson; }
    public void setWeakTopicsJson(String weakTopicsJson) { this.weakTopicsJson = weakTopicsJson; }
    public String getStrengthsJson() { return strengthsJson; }
    public void setStrengthsJson(String strengthsJson) { this.strengthsJson = strengthsJson; }
    public String getCoverageGaps() { return coverageGaps; }
    public void setCoverageGaps(String coverageGaps) { this.coverageGaps = coverageGaps; }
    public String getReadinessSummary() { return readinessSummary; }
    public void setReadinessSummary(String readinessSummary) { this.readinessSummary = readinessSummary; }
    public String getDataLimitations() { return dataLimitations; }
    public void setDataLimitations(String dataLimitations) { this.dataLimitations = dataLimitations; }
    public Double getCompletionPercent() { return completionPercent; }
    public void setCompletionPercent(Double completionPercent) { this.completionPercent = completionPercent; }
    public Integer getPlannedMinutes() { return plannedMinutes; }
    public void setPlannedMinutes(Integer plannedMinutes) { this.plannedMinutes = plannedMinutes; }
    public Integer getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(Integer actualMinutes) { this.actualMinutes = actualMinutes; }
    public Double getPracticeAverage() { return practiceAverage; }
    public void setPracticeAverage(Double practiceAverage) { this.practiceAverage = practiceAverage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public String getCompletionDisplay() {
        return completionPercent == null ? "—" : String.format("%.0f%%", completionPercent);
    }

    public String getPracticeAverageDisplay() {
        return practiceAverage == null ? "No attempts yet" : String.format("%.0f%%", practiceAverage);
    }

    public List<String> getStrengthList() { return jsonStrings(strengthsJson); }
    public List<String> getGapList() { return jsonStrings(coverageGaps); }
    public List<String> getLimitationList() { return jsonStrings(dataLimitations); }

    /** Weak topics as maps so JSP EL can read fields without getters on the DTO. */
    public List<java.util.Map<String, Object>> getWeakTopicList() {
        if (weakTopicsJson == null || weakTopicsJson.isBlank()) {
            return List.of();
        }
        try {
            List<Object> raw = com.examlens.util.Json.mapper().readValue(weakTopicsJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {
                    });
            return com.examlens.util.Json.toMaps(raw);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<String> jsonStrings(String v) {
        if (v == null || v.isBlank()) {
            return List.of();
        }
        try {
            return com.examlens.util.Json.mapper().readValue(v,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                    });
        } catch (Exception e) {
            // Older rows may have been stored as newline text; fall back to line splitting.
            return v.lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
    }
}
