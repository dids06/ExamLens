package com.examlens.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AttemptAnswer {
    private long id;
    private long attemptId;
    private long questionId;
    private String answerText;
    private String selectedOption;
    private Double awardedMarks;
    private String gradingMethod;
    private String feedback;
    private List<CriterionResult> criteria = new ArrayList<>();
    private String confidence;
    private boolean requiresManualReview;
    private Long aiJobId;
    private LocalDateTime updatedAt;
    private PaperQuestion question;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getAttemptId() { return attemptId; }
    public void setAttemptId(long attemptId) { this.attemptId = attemptId; }
    public long getQuestionId() { return questionId; }
    public void setQuestionId(long questionId) { this.questionId = questionId; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public String getSelectedOption() { return selectedOption; }
    public void setSelectedOption(String selectedOption) { this.selectedOption = selectedOption; }
    public Double getAwardedMarks() { return awardedMarks; }
    public void setAwardedMarks(Double awardedMarks) { this.awardedMarks = awardedMarks; }
    public String getGradingMethod() { return gradingMethod; }
    public void setGradingMethod(String gradingMethod) { this.gradingMethod = gradingMethod; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public List<CriterionResult> getCriteria() { return criteria; }
    public void setCriteria(List<CriterionResult> criteria) { this.criteria = criteria; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public boolean isRequiresManualReview() { return requiresManualReview; }
    public void setRequiresManualReview(boolean requiresManualReview) { this.requiresManualReview = requiresManualReview; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public PaperQuestion getQuestion() { return question; }
    public void setQuestion(PaperQuestion question) { this.question = question; }

    public String getAwardedDisplay() {
        if (awardedMarks == null) return "Not graded";
        return (awardedMarks == Math.floor(awardedMarks) ? String.valueOf((int) (double) awardedMarks) : String.format("%.1f", awardedMarks));
    }

    public boolean isAiGraded() {
        return "AI_RUBRIC".equals(gradingMethod) || "AI_ESTIMATE".equals(gradingMethod);
    }

    public static class CriterionResult {
        private String criterion;
        private double awardedMarks;
        private double maximumMarks;
        private String reason;

        public CriterionResult() {
        }

        public CriterionResult(String criterion, double awardedMarks, double maximumMarks, String reason) {
            this.criterion = criterion;
            this.awardedMarks = awardedMarks;
            this.maximumMarks = maximumMarks;
            this.reason = reason;
        }

        public String getCriterion() { return criterion; }
        public void setCriterion(String criterion) { this.criterion = criterion; }
        public double getAwardedMarks() { return awardedMarks; }
        public void setAwardedMarks(double awardedMarks) { this.awardedMarks = awardedMarks; }
        public double getMaximumMarks() { return maximumMarks; }
        public void setMaximumMarks(double maximumMarks) { this.maximumMarks = maximumMarks; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
