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
import com.examlens.model.TeacherGuidance;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * P02 — teacher instructions, hints and important topics. Extracted guidance stays a draft until
 * the student confirms it; doubtful items are surfaced as uncertainties rather than promoted to facts.
 */
public class TeacherGuidanceService {

    private final DocumentDao documents;
    private final LearningDao learning;
    private final ExamService exams;
    private final AiServiceRunner ai;

    public TeacherGuidanceService(DocumentDao documents, LearningDao learning, ExamService exams, AiServiceRunner ai) {
        this.documents = documents;
        this.learning = learning;
        this.exams = exams;
        this.ai = ai;
    }

    public long extract(long userId, long examId, long documentId) {
        Exam exam = exams.require(userId, examId);
        Document document = findDocument(userId, documentId);
        if (document.getExamId() == null || document.getExamId() != examId) {
            throw new ServiceException("That document is not attached to this exam.");
        }
        List<DocumentBlock> blocks = blocks(userId, documentId);
        if (blocks.isEmpty()) {
            throw new ServiceException("This document has no extracted text yet. Run extraction first.");
        }

        ObjectNode input = AiInputs.object();
        input.put("rawText", AiInputs.rawText(blocks));
        input.set("blocks", AiInputs.blocks(blocks));
        ObjectNode context = AiInputs.object();
        context.put("subjectName", exam.getSubjectName());
        if (exam.getExamDatetime() != null) {
            context.put("examDateTime", exam.getExamDatetime().toString());
        }
        input.set("examContext", context);

        AiOutcome<DocDtos.TeacherInstructionsResult> outcome = ai.run(
                AiAction.P02_TEACHER_INSTRUCTIONS, userId, examId, input,
                AiValidators.teacherInstructions(), null);

        DocDtos.TeacherInstructionsResult result = outcome.getData();
        try {
            TeacherGuidance guidance = new TeacherGuidance();
            guidance.setExamId(examId);
            guidance.setUserId(userId);
            guidance.setDocumentId(documentId);
            guidance.setAiJobId(outcome.getJobId());
            guidance.setHints(Json.write(result.hints));
            guidance.setImportantTopics(Json.write(result.importantTopics));
            guidance.setInstructions(Json.write(result.instructions));
            guidance.setExamFormat(Json.write(result.examFormatDetails));
            guidance.setUncertainties(Json.write(result.uncertainties));
            guidance.setConfirmed(false);
            return learning.insertTeacherGuidance(guidance);
        } catch (SQLException e) {
            throw new ServiceException("The extracted teacher guidance could not be saved.", e);
        }
    }

    public void confirm(long userId, long examId, long guidanceId) {
        exams.require(userId, examId);
        try {
            learning.confirmTeacherGuidance(guidanceId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not confirm the teacher guidance.", e);
        }
    }

    public Optional<TeacherGuidance> latest(long userId, long examId) {
        try {
            return learning.latestTeacherGuidance(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load teacher guidance.", e);
        }
    }

    public List<DocumentBlock> blocks(long userId, long documentId) {
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
