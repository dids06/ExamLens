package com.examlens.model;

import com.examlens.util.Text;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Exam {
    private long id;
    private long userId;
    private String subjectName;
    private LocalDateTime examDatetime;
    private String timezone = "UTC";
    private Integer targetMarks;
    private Integer totalMarks;
    private String notes;
    private String status = "ACTIVE";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public LocalDateTime getExamDatetime() { return examDatetime; }
    public void setExamDatetime(LocalDateTime examDatetime) { this.examDatetime = examDatetime; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Integer getTargetMarks() { return targetMarks; }
    public void setTargetMarks(Integer targetMarks) { this.targetMarks = targetMarks; }
    public Integer getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Integer totalMarks) { this.totalMarks = totalMarks; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDate getExamDate() {
        return examDatetime == null ? null : examDatetime.toLocalDate();
    }

    public String getExamDateInput() {
        return examDatetime == null ? "" : examDatetime.toLocalDate().toString();
    }

    public String getExamTimeInput() {
        return examDatetime == null ? "" : examDatetime.toLocalTime().toString().substring(0, Math.min(5, examDatetime.toLocalTime().toString().length()));
    }

    public Long getDaysUntil() {
        if (examDatetime == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), examDatetime.toLocalDate());
    }

    public String getExamDatetimeDisplay() {
        return Text.formatDateTime(examDatetime);
    }
}
