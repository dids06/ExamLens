package com.examlens.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaperAttempt {
    private long id;
    private long paperId;
    private long userId;
    private String status = "IN_PROGRESS";
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private boolean timed;
    private Integer durationMinutes;
    private Double totalAwarded;
    private Integer totalPossible;
    private Long aiJobId;
    private PracticePaper paper;
    private List<AttemptAnswer> answers = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPaperId() { return paperId; }
    public void setPaperId(long paperId) { this.paperId = paperId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public boolean isTimed() { return timed; }
    public void setTimed(boolean timed) { this.timed = timed; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Double getTotalAwarded() { return totalAwarded; }
    public void setTotalAwarded(Double totalAwarded) { this.totalAwarded = totalAwarded; }
    public Integer getTotalPossible() { return totalPossible; }
    public void setTotalPossible(Integer totalPossible) { this.totalPossible = totalPossible; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public PracticePaper getPaper() { return paper; }
    public void setPaper(PracticePaper paper) { this.paper = paper; }
    public List<AttemptAnswer> getAnswers() { return answers; }
    public void setAnswers(List<AttemptAnswer> answers) { this.answers = answers; }

    public boolean isSubmitted() {
        return "SUBMITTED".equals(status) || "GRADED".equals(status);
    }

    public String getScoreDisplay() {
        if (totalAwarded == null || totalPossible == null) {
            return "—";
        }
        return String.format("%.1f / %d", totalAwarded, totalPossible);
    }

    public Integer getScorePercent() {
        if (totalAwarded == null || totalPossible == null || totalPossible == 0) {
            return null;
        }
        return (int) Math.round(totalAwarded * 100.0 / totalPossible);
    }
}
