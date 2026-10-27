package com.examlens.model;

import java.util.ArrayList;
import java.util.List;

public class StudyTask {
    private long id;
    private long planVersionId;
    private String taskKey;
    private long examId;
    private long userId;
    private Long topicId;
    private String taskType;
    private String title;
    private String objective;
    private String studyMethod;
    private int estimatedMinutes;
    private java.time.LocalDate scheduledDate;
    private int taskOrder;
    private String priority;
    private String status = "PENDING";
    private String aiRationale;
    private String revisionOfTaskKey;
    private String changeType;
    private java.time.LocalDateTime completedAt;
    private List<String> prerequisites = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPlanVersionId() { return planVersionId; }
    public void setPlanVersionId(long planVersionId) { this.planVersionId = planVersionId; }
    public String getTaskKey() { return taskKey; }
    public void setTaskKey(String taskKey) { this.taskKey = taskKey; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public String getStudyMethod() { return studyMethod; }
    public void setStudyMethod(String studyMethod) { this.studyMethod = studyMethod; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public java.time.LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(java.time.LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public int getTaskOrder() { return taskOrder; }
    public void setTaskOrder(int taskOrder) { this.taskOrder = taskOrder; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAiRationale() { return aiRationale; }
    public void setAiRationale(String aiRationale) { this.aiRationale = aiRationale; }
    public String getRevisionOfTaskKey() { return revisionOfTaskKey; }
    public void setRevisionOfTaskKey(String revisionOfTaskKey) { this.revisionOfTaskKey = revisionOfTaskKey; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public java.time.LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(java.time.LocalDateTime completedAt) { this.completedAt = completedAt; }
    public List<String> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(List<String> prerequisites) { this.prerequisites = prerequisites; }

    public String getDurationDisplay() {
        int h = estimatedMinutes / 60;
        int m = estimatedMinutes % 60;
        if (h == 0) return m + " min";
        if (m == 0) return h + " h";
        return h + " h " + m + " min";
    }

    public String getTypeLabel() {
        if (taskType == null) return "Study";
        return switch (taskType) {
            case "LEARN" -> "Learn";
            case "PRACTICE" -> "Practice";
            case "REVISION" -> "Revision";
            case "MOCK_TEST" -> "Mock test";
            case "REVIEW" -> "Review";
            case "GENERAL" -> "General";
            default -> taskType;
        };
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isOverdue() {
        return scheduledDate != null && !isCompleted() && scheduledDate.isBefore(java.time.LocalDate.now());
    }
}
