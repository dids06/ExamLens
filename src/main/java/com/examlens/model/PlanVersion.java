package com.examlens.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlanVersion {
    private long id;
    private long planId;
    private int versionNumber;
    private String kind;
    private String status = "ACTIVE";
    private Long aiJobId;
    private String summary;
    private String feasibility;
    private String feasibilityNote;
    private String assumptions;
    private String warnings;
    private String risks;
    private String tradeoffs;
    private String unscheduledWork;
    private Integer totalEstimatedMinutes;
    private Integer availableCapacityMinutes;
    private String changeSummary;
    private String changeExplanation;
    private LocalDateTime createdAt;
    private List<StudyTask> tasks = new ArrayList<>();
    private boolean simulated;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPlanId() { return planId; }
    public void setPlanId(long planId) { this.planId = planId; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getFeasibility() { return feasibility; }
    public void setFeasibility(String feasibility) { this.feasibility = feasibility; }
    public String getFeasibilityNote() { return feasibilityNote; }
    public void setFeasibilityNote(String feasibilityNote) { this.feasibilityNote = feasibilityNote; }
    public String getAssumptions() { return assumptions; }
    public void setAssumptions(String assumptions) { this.assumptions = assumptions; }
    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }
    public String getRisks() { return risks; }
    public void setRisks(String risks) { this.risks = risks; }
    public String getTradeoffs() { return tradeoffs; }
    public void setTradeoffs(String tradeoffs) { this.tradeoffs = tradeoffs; }
    public String getUnscheduledWork() { return unscheduledWork; }
    public void setUnscheduledWork(String unscheduledWork) { this.unscheduledWork = unscheduledWork; }
    public Integer getTotalEstimatedMinutes() { return totalEstimatedMinutes; }
    public void setTotalEstimatedMinutes(Integer totalEstimatedMinutes) { this.totalEstimatedMinutes = totalEstimatedMinutes; }
    public Integer getAvailableCapacityMinutes() { return availableCapacityMinutes; }
    public void setAvailableCapacityMinutes(Integer availableCapacityMinutes) { this.availableCapacityMinutes = availableCapacityMinutes; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
    public String getChangeExplanation() { return changeExplanation; }
    public void setChangeExplanation(String changeExplanation) { this.changeExplanation = changeExplanation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<StudyTask> getTasks() { return tasks; }
    public void setTasks(List<StudyTask> tasks) { this.tasks = tasks; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public String getFeasibilityLabel() {
        if (feasibility == null) return "Not assessed";
        return switch (feasibility) {
            case "FEASIBLE" -> "Feasible";
            case "TIGHT" -> "Tight but workable";
            case "INFEASIBLE" -> "Not feasible as requested";
            default -> feasibility;
        };
    }

    public String getTotalHoursDisplay() {
        if (totalEstimatedMinutes == null) return "—";
        return String.format("%.1f h", totalEstimatedMinutes / 60.0);
    }

    public List<String> getWarningLines() {
        if (warnings == null || warnings.isBlank()) return List.of();
        return warnings.lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
