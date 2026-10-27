package com.examlens.model;

public class PyqQuestion {
    private long id;
    private long paperId;
    private String questionKey;
    private String questionText;
    private Integer marks;
    private Integer pageNumber;
    private String matchedTopic;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPaperId() { return paperId; }
    public void setPaperId(long paperId) { this.paperId = paperId; }
    public String getQuestionKey() { return questionKey; }
    public void setQuestionKey(String questionKey) { this.questionKey = questionKey; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public Integer getMarks() { return marks; }
    public void setMarks(Integer marks) { this.marks = marks; }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public String getMatchedTopic() { return matchedTopic; }
    public void setMatchedTopic(String matchedTopic) { this.matchedTopic = matchedTopic; }
}
