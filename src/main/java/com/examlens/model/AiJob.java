package com.examlens.model;

import java.time.LocalDateTime;

public class AiJob {
    private long id;
    private long userId;
    private Long examId;
    private String actionKey;
    private String promptVersion;
    private String provider;
    private String model;
    private String status;
    private int attemptCount;
    private int correctionAttempts;
    private Long durationMs;
    private Integer inputChars;
    private String errorCode;
    private String errorMessage;
    private String rawResponse;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public int getCorrectionAttempts() { return correctionAttempts; }
    public void setCorrectionAttempts(int correctionAttempts) { this.correctionAttempts = correctionAttempts; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public Integer getInputChars() { return inputChars; }
    public void setInputChars(Integer inputChars) { this.inputChars = inputChars; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public boolean isSimulatedProvider() {
        return provider != null && provider.toLowerCase().contains("simulated");
    }
}
