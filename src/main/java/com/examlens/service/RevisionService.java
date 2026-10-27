package com.examlens.service;

import com.examlens.ai.AiAction;
import com.examlens.ai.AiOutcome;
import com.examlens.ai.AiServiceRunner;
import com.examlens.ai.dto.LearningDtos;
import com.examlens.ai.validate.AiValidators;
import com.examlens.dao.AiJobDao;
import com.examlens.dao.LearningDao;
import com.examlens.model.Exam;
import com.examlens.model.RevisionResource;
import com.examlens.model.Topic;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * P14 — AI-generated revision resources. The AI decides which topics deserve flashcards, key
 * points and active-recall questions and how deep they go; Java only validates that referenced
 * topic IDs exist, persists the output and keeps the simulated label accurate.
 */
public class RevisionService {

    private final LearningDao learning;
    private final SyllabusService syllabus;
    private final ExamService exams;
    private final AiServiceRunner ai;
    private final SimulatedFlags flags;

    public RevisionService(LearningDao learning, SyllabusService syllabus, ExamService exams,
                           AiServiceRunner ai, AiJobDao jobs) {
        this.learning = learning;
        this.syllabus = syllabus;
        this.exams = exams;
        this.ai = ai;
        this.flags = new SimulatedFlags(jobs);
    }

    public record RevisionBatch(List<RevisionResource> resources, String summary, List<String> warnings,
                                boolean simulated, long aiJobId) {
    }

    public RevisionBatch generate(long userId, long examId, List<Long> topicIds, String depth, Integer availableMinutes) {
        Exam exam = exams.require(userId, examId);
        List<Topic> confirmed = syllabus.confirmedTopics(userId, examId);
        if (confirmed.isEmpty()) {
            throw new ServiceException("Confirm your syllabus topics before creating revision resources.");
        }
        List<Topic> selected = select(confirmed, topicIds);
        if (selected.isEmpty()) {
            throw new ServiceException("Select at least one confirmed topic.");
        }
        String safeDepth = normaliseDepth(depth);

        Set<String> allowed = new LinkedHashSet<>();
        for (Topic t : confirmed) {
            allowed.add(String.valueOf(t.getId()));
        }

        ObjectNode input = AiInputs.object();
        input.put("subjectName", exam.getSubjectName());
        input.set("topics", AiInputs.topics(selected));
        input.put("depth", safeDepth);
        if (availableMinutes != null && availableMinutes > 0) {
            input.put("availableMinutes", availableMinutes);
        }
        if (exam.getExamDatetime() != null) {
            input.put("examDateTime", exam.getExamDatetime().toString());
        }

        AiOutcome<LearningDtos.RevisionResourcesResult> outcome = ai.run(
                AiAction.P14_REVISION_RESOURCES, userId, examId, input,
                AiValidators.revisionResources(),
                data -> domainCheck(data, allowed));

        LearningDtos.RevisionResourcesResult result = outcome.getData();
        try {
            List<RevisionResource> saved = new ArrayList<>();
            for (LearningDtos.RevisionResourcesResult.TopicResource tr : result.topicResources) {
                RevisionResource r = new RevisionResource();
                r.setExamId(examId);
                r.setUserId(userId);
                r.setAiJobId(outcome.getJobId());
                r.setTopicTitle(tr.topic);
                r.setTopicId(StudyPlanService.parseLong(tr.topicId));
                r.setKeyPoints(join(tr.keyPoints));
                r.setFlashcardsJson(Json.write(tr.flashcards));
                r.setRecallJson(Json.write(tr.activeRecallQuestions));
                r.setSourceReferences(join(tr.sourceReferences));
                r.setSimulated(outcome.isSimulated());
                r.setId(learning.insertRevisionResource(r));
                saved.add(r);
            }
            return new RevisionBatch(saved, result.summary, result.warnings, outcome.isSimulated(), outcome.getJobId());
        } catch (SQLException e) {
            throw new ServiceException("The revision resources could not be saved.", e);
        }
    }

    public List<RevisionResource> resources(long userId, long examId) {
        exams.require(userId, examId);
        try {
            List<RevisionResource> list = learning.revisionResources(examId, userId);
            var simulatedById = flags.forExam(userId, examId);
            for (RevisionResource r : list) {
                r.setSimulated(Boolean.TRUE.equals(simulatedById.get(r.getAiJobId())));
            }
            return list;
        } catch (SQLException e) {
            throw new ServiceException("Could not load revision resources.", e);
        }
    }

    public int count(long userId, long examId) {
        try {
            return learning.countRevisionResources(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not count revision resources.", e);
        }
    }

    private List<String> domainCheck(LearningDtos.RevisionResourcesResult data, Set<String> allowedTopicIds) {
        List<String> errors = new ArrayList<>();
        for (LearningDtos.RevisionResourcesResult.TopicResource r : data.topicResources) {
            if (r.topicId != null && !r.topicId.isBlank() && !allowedTopicIds.contains(r.topicId.trim())) {
                errors.add("revision resource for \"" + r.topic + "\" references topicId \"" + r.topicId
                        + "\" which is not a confirmed topic for this exam.");
            }
        }
        return errors;
    }

    private List<Topic> select(List<Topic> confirmed, List<Long> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return confirmed;
        }
        Set<Long> wanted = new LinkedHashSet<>(topicIds);
        List<Topic> selected = new ArrayList<>();
        for (Topic t : confirmed) {
            if (wanted.contains(t.getId())) {
                selected.add(t);
            }
        }
        return selected;
    }

    private static String normaliseDepth(String depth) {
        if (depth == null || depth.isBlank()) {
            return "STANDARD";
        }
        String upper = depth.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (upper) {
            case "QUICK", "STANDARD", "DEEP" -> upper;
            default -> "STANDARD";
        };
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join("\n", values);
    }
}
