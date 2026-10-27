package com.examlens.model;

import java.time.LocalDateTime;
import java.util.List;

public class PlanChangeExplanation {
    private long id;
    private long planVersionId;
    private Long aiJobId;
    private String summary;
    private String changesJson;
    private String unchangedPriorities;
    private String actionItems;
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPlanVersionId() { return planVersionId; }
    public void setPlanVersionId(long planVersionId) { this.planVersionId = planVersionId; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getChangesJson() { return changesJson; }
    public void setChangesJson(String changesJson) { this.changesJson = changesJson; }
    public String getUnchangedPriorities() { return unchangedPriorities; }
    public void setUnchangedPriorities(String unchangedPriorities) { this.unchangedPriorities = unchangedPriorities; }
    public String getActionItems() { return actionItems; }
    public void setActionItems(String actionItems) { this.actionItems = actionItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Change rows as maps so JSP EL can read fields without getters on the DTO. */
    public List<java.util.Map<String, Object>> getChangeList() {
        return readMapList(changesJson);
    }

    public List<String> getUnchangedList() { return lines(unchangedPriorities); }
    public List<String> getActionList() { return lines(actionItems); }

    private static List<java.util.Map<String, Object>> readMapList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Object> raw = com.examlens.util.Json.mapper().readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {
                    });
            return com.examlens.util.Json.toMaps(raw);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<String> lines(String v) {
        return com.examlens.util.Json.readStringList(v);
    }
}
