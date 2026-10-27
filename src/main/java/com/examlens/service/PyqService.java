package com.examlens.service;

import com.examlens.ai.AiAction;
import com.examlens.ai.AiOutcome;
import com.examlens.ai.AiServiceRunner;
import com.examlens.ai.dto.DocDtos;
import com.examlens.ai.validate.AiValidators;
import com.examlens.dao.DocumentDao;
import com.examlens.dao.LearningDao;
import com.examlens.model.Document;
import com.examlens.model.DocumentBlock;
import com.examlens.model.Exam;
import com.examlens.model.PreviousPaper;
import com.examlens.model.PyqAnalysis;
import com.examlens.model.PyqQuestion;
import com.examlens.model.Topic;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * P05 — previous-year question analysis. Counts are derived only from the questions extracted from
 * the uploaded papers; historical frequency is never presented as a prediction.
 */
public class PyqService {

    private final DocumentDao documents;
    private final LearningDao learning;
    private final SyllabusService syllabus;
    private final ExamService exams;
    private final AiServiceRunner ai;

    public PyqService(DocumentDao documents, LearningDao learning, SyllabusService syllabus,
                      ExamService exams, AiServiceRunner ai) {
        this.documents = documents;
        this.learning = learning;
        this.syllabus = syllabus;
        this.exams = exams;
        this.ai = ai;
    }

    public long addPaper(long userId, long examId, long documentId, String label, String year, Integer totalMarks) {
        exams.require(userId, examId);
        Document document = findDocument(userId, documentId);
        if (document.getExamId() == null || document.getExamId() != examId) {
            throw new ServiceException("That document is not attached to this exam.");
        }
        if (label == null || label.isBlank()) {
            throw new ServiceException("Give the previous-year paper a label (for example \"Main paper\").");
        }
        if (totalMarks != null && (totalMarks < 0 || totalMarks > 1000)) {
            throw new ServiceException("Total marks must be between 0 and 1000.");
        }
        try {
            return learning.insertPaper(examId, userId, documentId, label.trim(),
                    year == null || year.isBlank() ? null : year.trim(), totalMarks);
        } catch (SQLException e) {
            throw new ServiceException("Could not save the previous-year paper.", e);
        }
    }

    public List<PreviousPaper> papers(long userId, long examId) {
        try {
            return learning.previousPapers(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load previous-year papers.", e);
        }
    }

    public long analyse(long userId, long examId) {
        exams.require(userId, examId);
        List<PreviousPaper> papers = papers(userId, examId);
        if (papers.isEmpty()) {
            throw new ServiceException("Add at least one previous-year paper before running the analysis.");
        }
        List<Topic> topics = syllabus.confirmedTopics(userId, examId);

        ArrayNode papersNode = Json.array();
        Set<String> suppliedPaperIds = new LinkedHashSet<>();
        int totalQuestions = 0;
        for (PreviousPaper paper : papers) {
            String paperId = String.valueOf(paper.getId());
            suppliedPaperIds.add(paperId);
            ObjectNode node = AiInputs.object();
            node.put("paperId", paperId);
            node.put("label", paper.getPaperLabel());
            if (paper.getExamYear() != null) {
                node.put("year", paper.getExamYear());
            }
            ArrayNode questions = Json.array();
            List<DocumentBlock> blocks = blocks(userId, paper.getDocumentId());
            for (DocumentBlock block : blocks) {
                ObjectNode q = AiInputs.object();
                // Prefix with the paper id so question keys stay unique across papers.
                q.put("questionKey", "p" + paperId + "-" + block.getBlockKey());
                q.put("text", block.getBlockText());
                if (block.getPageNumber() != null) {
                    q.put("page", block.getPageNumber());
                }
                questions.add(q);
                totalQuestions++;
            }
            node.set("questions", questions);
            papersNode.add(node);
        }
        if (totalQuestions == 0) {
            throw new ServiceException("The previous-year papers have no extracted text to analyse.");
        }

        ObjectNode input = AiInputs.object();
        input.put("subjectName", exams.require(userId, examId).getSubjectName());
        input.set("papers", papersNode);
        input.set("syllabusTopics", AiInputs.topics(topics));

        AiOutcome<DocDtos.PyqAnalysisResult> outcome = ai.run(
                AiAction.P05_PYQ_ANALYSIS, userId, examId, input,
                AiValidators.pyqAnalysis(),
                data -> domainCheck(data, suppliedPaperIds));

        DocDtos.PyqAnalysisResult result = outcome.getData();
        try {
            for (PreviousPaper paper : papers) {
                String paperId = String.valueOf(paper.getId());
                List<PyqQuestion> rows = new ArrayList<>();
                for (DocDtos.PyqAnalysisResult.Question q : result.questions) {
                    if (q.paperId != null && q.paperId.equals(paperId)) {
                        PyqQuestion row = new PyqQuestion();
                        row.setQuestionKey(q.questionKey);
                        row.setQuestionText(q.text);
                        row.setMarks(q.marks);
                        row.setPageNumber(q.page);
                        row.setMatchedTopic(q.topic);
                        rows.add(row);
                    }
                }
                if (!rows.isEmpty()) {
                    learning.replacePyqQuestions(paper.getId(), rows);
                }
            }
            PyqAnalysis analysis = new PyqAnalysis();
            analysis.setExamId(examId);
            analysis.setUserId(userId);
            analysis.setAiJobId(outcome.getJobId());
            analysis.setTopicFrequencyJson(Json.write(result.topicFrequency));
            analysis.setRecurringPatterns(Json.write(result.recurringPatterns));
            analysis.setUnmatchedQuestions(Json.write(result.unmatchedQuestions));
            analysis.setLimitations(Json.write(result.limitations));
            analysis.setSimulated(outcome.isSimulated());
            return learning.insertPyqAnalysis(analysis);
        } catch (SQLException e) {
            throw new ServiceException("The previous-year analysis could not be saved.", e);
        }
    }

    private List<String> domainCheck(DocDtos.PyqAnalysisResult data, Set<String> suppliedPaperIds) {
        List<String> errors = new ArrayList<>();
        for (DocDtos.PyqAnalysisResult.TopicFrequency f : data.topicFrequency) {
            if (f.paperIds != null) {
                for (String id : f.paperIds) {
                    if (!suppliedPaperIds.contains(id)) {
                        errors.add("topicFrequency for \"" + f.topic + "\" cites paperId \"" + id
                                + "\" which is not one of the supplied papers.");
                    }
                }
            }
        }
        for (DocDtos.PyqAnalysisResult.Question q : data.questions) {
            if (q.paperId != null && !q.paperId.isBlank() && !suppliedPaperIds.contains(q.paperId)) {
                errors.add("extracted question \"" + q.questionKey + "\" cites paperId \"" + q.paperId
                        + "\" which is not one of the supplied papers.");
            }
        }
        return errors;
    }

    public Optional<PyqAnalysis> latestAnalysis(long userId, long examId) {
        try {
            return learning.latestPyqAnalysis(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the previous-year analysis.", e);
        }
    }

    public List<PyqQuestion> questions(long userId, long paperId) {
        try {
            Optional<PreviousPaper> paper = learning.findPaper(paperId, userId);
            if (paper.isEmpty()) {
                return List.of();
            }
            return learning.pyqQuestions(paperId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load previous-year questions.", e);
        }
    }

    private List<DocumentBlock> blocks(long userId, long documentId) {
        try {
            return documents.blocks(documentId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load extracted text.", e);
        }
    }

    private Document findDocument(long userId, long documentId) {
        try {
            return documents.find(documentId, userId)
                    .orElseThrow(() -> new ServiceException("That document was not found in your account."));
        } catch (SQLException e) {
            throw new ServiceException("Could not load that document.", e);
        }
    }
}
