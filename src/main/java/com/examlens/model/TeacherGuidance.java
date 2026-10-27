package com.examlens.model;

import java.time.LocalDateTime;
import java.util.List;

public class TeacherGuidance {
    private long id;
    private long examId;
    private long userId;
    private Long documentId;
    private Long aiJobId;
    private String hints;
    private String importantTopics;
    private String instructions;
    private String examFormat;
    private String uncertainties;
    private boolean confirmed;
    private LocalDateTime createdAt;
    private boolean simulated;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public String getHints() { return hints; }
    public void setHints(String hints) { this.hints = hints; }
    public String getImportantTopics() { return importantTopics; }
    public void setImportantTopics(String importantTopics) { this.importantTopics = importantTopics; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getExamFormat() { return examFormat; }
    public void setExamFormat(String examFormat) { this.examFormat = examFormat; }
    public String getUncertainties() { return uncertainties; }
    public void setUncertainties(String uncertainties) { this.uncertainties = uncertainties; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public List<String> getHintLines() { return getHintList().stream().map(h -> String.valueOf(h.get("text"))).toList(); }
    public List<String> getInstructionLines() { return com.examlens.util.Json.readStringList(instructions); }
    public List<String> getFormatLines() { return com.examlens.util.Json.readStringList(examFormat); }
    public List<String> getUncertaintyLines() { return com.examlens.util.Json.readStringList(uncertainties); }

    /** Hint objects (text, evidenceQuote, sourceReference, confidence) as maps for JSP EL. */
    public List<java.util.Map<String, Object>> getHintList() {
        return rows(hints);
    }

    /** Important-topic objects (topic, priority, evidenceQuote, sourceReference) as maps for JSP EL. */
    public List<java.util.Map<String, Object>> getImportantTopicList() {
        return rows(importantTopics);
    }

    private static List<java.util.Map<String, Object>> rows(String json) {
        return com.examlens.util.Json.readMapList(json);
    }
}
