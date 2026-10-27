package com.examlens.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PracticePaper {
    private long id;
    private long examId;
    private long userId;
    private Long aiJobId;
    private String title;
    private String instructions;
    private int totalMarks;
    private Integer durationMinutes;
    private String difficulty;
    private String status = "READY";
    private LocalDateTime createdAt;
    private List<PaperQuestion> questions = new ArrayList<>();
    private boolean simulated;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public int getTotalMarks() { return totalMarks; }
    public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<PaperQuestion> getQuestions() { return questions; }
    public void setQuestions(List<PaperQuestion> questions) { this.questions = questions; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public String getDurationDisplay() {
        if (durationMinutes == null) return "Untimed";
        int h = durationMinutes / 60;
        int m = durationMinutes % 60;
        if (h == 0) return m + " min";
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }
}
