package com.examlens.model;

import java.util.ArrayList;
import java.util.List;

public class PaperQuestion {
    private long id;
    private long paperId;
    private String clientKey;
    private String questionType;
    private String topic;
    private Long topicId;
    private String prompt;
    private List<String> options = new ArrayList<>();
    private String correctOption;
    private int marks;
    private String difficulty;
    private String modelAnswer;
    private List<RubricItem> rubric = new ArrayList<>();
    private int displayOrder;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPaperId() { return paperId; }
    public void setPaperId(long paperId) { this.paperId = paperId; }
    public String getClientKey() { return clientKey; }
    public void setClientKey(String clientKey) { this.clientKey = clientKey; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }
    public int getMarks() { return marks; }
    public void setMarks(int marks) { this.marks = marks; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getModelAnswer() { return modelAnswer; }
    public void setModelAnswer(String modelAnswer) { this.modelAnswer = modelAnswer; }
    public List<RubricItem> getRubric() { return rubric; }
    public void setRubric(List<RubricItem> rubric) { this.rubric = rubric; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public boolean isMultipleChoice() {
        return "MCQ".equals(questionType);
    }

    public String getTypeLabel() {
        if (questionType == null) return "Question";
        return switch (questionType) {
            case "MCQ" -> "Multiple choice";
            case "SHORT_ANSWER" -> "Short answer";
            case "THEORY" -> "Long answer";
            case "NUMERICAL" -> "Numerical";
            case "APPLICATION" -> "Application";
            default -> questionType.replace('_', ' ');
        };
    }

    /** Rubric rows use stable option indices A, B, C ... */
    public List<String> getOptionLabels() {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            labels.add(String.valueOf((char) ('A' + i)));
        }
        return labels;
    }

    public static class RubricItem {
        private String criterion;
        private int marks;

        public RubricItem() {
        }

        public RubricItem(String criterion, int marks) {
            this.criterion = criterion;
            this.marks = marks;
        }

        public String getCriterion() { return criterion; }
        public void setCriterion(String criterion) { this.criterion = criterion; }
        public int getMarks() { return marks; }
        public void setMarks(int marks) { this.marks = marks; }
    }
}
