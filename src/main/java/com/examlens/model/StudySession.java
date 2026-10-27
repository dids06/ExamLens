package com.examlens.model;

public class StudySession {
    private long id;
    private long userId;
    private long examId;
    private Long taskId;
    private Integer plannedMinutes;
    private int actualMinutes;
    private String status;
    private String difficultyFeedback;
    private String studentNotes;
    private java.time.LocalDate sessionDate;
    private Long aiJobId;
    private java.time.LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Integer getPlannedMinutes() { return plannedMinutes; }
    public void setPlannedMinutes(Integer plannedMinutes) { this.plannedMinutes = plannedMinutes; }
    public int getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(int actualMinutes) { this.actualMinutes = actualMinutes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDifficultyFeedback() { return difficultyFeedback; }
    public void setDifficultyFeedback(String difficultyFeedback) { this.difficultyFeedback = difficultyFeedback; }
    public String getStudentNotes() { return studentNotes; }
    public void setStudentNotes(String studentNotes) { this.studentNotes = studentNotes; }
    public java.time.LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(java.time.LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}
