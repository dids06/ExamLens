package com.examlens.service;

import com.examlens.ai.AiAction;
import com.examlens.ai.AiOutcome;
import com.examlens.ai.AiServiceRunner;
import com.examlens.ai.dto.LearningDtos;
import com.examlens.ai.validate.AiValidators;
import com.examlens.dao.AiJobDao;
import com.examlens.dao.LearningDao;
import com.examlens.model.Exam;
import com.examlens.model.PaperAttempt;
import com.examlens.model.ReadinessAssessment;
import com.examlens.model.StudySession;
import com.examlens.model.StudyTask;
import com.examlens.model.Topic;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * P15 — readiness analysis. The canonical percentages (task completion, planned/actual minutes,
 * practice average) are computed here from persisted evidence; the AI interprets them and selects
 * weak topics with evidence. The AI can never introduce activity or a percentage that the backend
 * did not compute, because only the backend figures are stored and displayed.
 */
public class ReadinessService {

    private final LearningDao learning;
    private final SyllabusService syllabus;
    private final ExamService exams;
    private final PracticeService practice;
    private final StudyPlanService studyPlan;
    private final SessionService sessions;
    private final AiServiceRunner ai;
    private final SimulatedFlags flags;

    public ReadinessService(LearningDao learning, SyllabusService syllabus, ExamService exams,
                            PracticeService practice, StudyPlanService studyPlan, SessionService sessions,
                            AiServiceRunner ai, AiJobDao jobs) {
        this.learning = learning;
        this.syllabus = syllabus;
        this.exams = exams;
        this.practice = practice;
        this.studyPlan = studyPlan;
        this.sessions = sessions;
        this.ai = ai;
        this.flags = new SimulatedFlags(jobs);
    }

    public ReadinessAssessment analyse(long userId, long examId) {
        Exam exam = exams.require(userId, examId);
        List<Topic> topics = syllabus.confirmedTopics(userId, examId);

        int[] stats = studyPlan.taskStats(userId, examId);
        int totalTasks = stats[0];
        int completedTasks = stats[1];
        Double completionPercent = totalTasks == 0 ? null
                : Math.round(completedTasks * 10000.0 / totalTasks) / 100.0;
        int plannedMinutes = studyPlan.plannedMinutes(userId, examId);
        int actualMinutes = sessions.actualMinutes(userId, examId);
        Double practiceAverage = practice.practiceAverage(userId, examId);
        List<PaperAttempt> attempts = practice.attempts(userId, examId, 100);
        List<StudySession> recentSessions = sessions.sessions(userId, examId, 50);

        List<StudyTask> tasks = studyPlan.loadPlan(userId, examId).map(StudyPlanService.PlanView::tasks).orElse(List.of());
        Map<Long, int[]> tasksByTopic = new LinkedHashMap<>();
        for (StudyTask t : tasks) {
            if (t.getTopicId() == null) {
                continue;
            }
            int[] counts = tasksByTopic.computeIfAbsent(t.getTopicId(), k -> new int[2]);
            counts[0]++;
            if (t.isCompleted()) {
                counts[1]++;
            }
        }

        Set<String> allowedTopicIds = new LinkedHashSet<>();
        ArrayNode topicsNode = Json.array();
        for (Topic t : topics) {
            allowedTopicIds.add(String.valueOf(t.getId()));
            ObjectNode node = AiInputs.object();
            node.put("topicId", String.valueOf(t.getId()));
            node.put("title", t.getTitle());
            if (t.getImportance() != null) {
                node.put("importance", t.getImportance());
            }
            if (t.getDifficulty() != null) {
                node.put("difficulty", t.getDifficulty());
            }
            int[] counts = tasksByTopic.get(t.getId());
            int topicTotal = counts == null ? 0 : counts[0];
            int topicDone = counts == null ? 0 : counts[1];
            node.put("totalTasks", topicTotal);
            node.put("completedTasks", topicDone);
            node.put("status", topicTotal == 0 ? "NOT_SCHEDULED" : topicDone == topicTotal ? "COMPLETED"
                    : topicDone == 0 ? "NOT_STARTED" : "IN_PROGRESS");
            Double score = practice.topicScoreRatio(userId, examId, t.getId());
            if (score != null) {
                node.put("practiceScoreRatio", round(score));
            }
            topicsNode.add(node);
        }

        ObjectNode metrics = AiInputs.object();
        metrics.put("completionPercent", completionPercent == null ? 0 : completionPercent);
        metrics.put("hasTaskData", totalTasks > 0);
        metrics.put("totalTasks", totalTasks);
        metrics.put("completedTasks", completedTasks);
        metrics.put("plannedMinutes", plannedMinutes);
        metrics.put("actualMinutes", actualMinutes);
        metrics.put("sessionsLogged", recentSessions.size());
        metrics.put("papersAttempted", attempts.size());
        if (practiceAverage == null) {
            metrics.put("practiceAverage", -1);
            metrics.putNull("practiceAveragePercent");
        } else {
            metrics.put("practiceAverage", round(practiceAverage));
            metrics.put("practiceAveragePercent", round(practiceAverage));
        }

        ObjectNode input = AiInputs.object();
        input.put("subjectName", exam.getSubjectName());
        if (exam.getExamDatetime() != null) {
            input.put("examDateTime", exam.getExamDatetime().toString());
        }
        input.set("metrics", metrics);
        input.set("topics", topicsNode);
        input.set("recentSessions", recentSessions(recentSessions));

        AiOutcome<LearningDtos.ReadinessResult> outcome = ai.run(
                AiAction.P15_READINESS_ANALYSIS, userId, examId, input,
                AiValidators.readiness(),
                data -> domainCheck(data, allowedTopicIds));

        LearningDtos.ReadinessResult result = outcome.getData();
        try {
            ReadinessAssessment assessment = new ReadinessAssessment();
            assessment.setExamId(examId);
            assessment.setUserId(userId);
            assessment.setAiJobId(outcome.getJobId());
            assessment.setWeakTopicsJson(Json.write(result.weakTopics));
            assessment.setStrengthsJson(Json.write(result.strengths));
            assessment.setCoverageGaps(Json.write(result.coverageGaps));
            assessment.setReadinessSummary(result.readinessSummary);
            assessment.setDataLimitations(Json.write(result.dataLimitations));
            assessment.setCompletionPercent(completionPercent);
            assessment.setPlannedMinutes(plannedMinutes);
            assessment.setActualMinutes(actualMinutes);
            assessment.setPracticeAverage(practiceAverage);
            assessment.setSimulated(outcome.isSimulated());
            assessment.setId(learning.insertReadiness(assessment));
            return assessment;
        } catch (SQLException e) {
            throw new ServiceException("The readiness assessment could not be saved.", e);
        }
    }

    public Optional<ReadinessAssessment> latest(long userId, long examId) {
        exams.require(userId, examId);
        try {
            Optional<ReadinessAssessment> found = learning.latestReadiness(examId, userId);
            found.ifPresent(a -> a.setSimulated(flags.forJob(userId, a.getAiJobId())));
            return found;
        } catch (SQLException e) {
            throw new ServiceException("Could not load the readiness assessment.", e);
        }
    }

    private ArrayNode recentSessions(List<StudySession> latest) {
        ArrayNode array = Json.array();
        for (StudySession s : latest) {
            ObjectNode node = AiInputs.object();
            node.put("date", s.getSessionDate() == null ? null : s.getSessionDate().toString());
            node.put("plannedMinutes", s.getPlannedMinutes() == null ? 0 : s.getPlannedMinutes());
            node.put("actualMinutes", s.getActualMinutes());
            node.put("status", s.getStatus());
            if (s.getDifficultyFeedback() != null) {
                node.put("difficultyFeedback", s.getDifficultyFeedback());
            }
            array.add(node);
        }
        return array;
    }

    private List<String> domainCheck(LearningDtos.ReadinessResult data, Set<String> allowedTopicIds) {
        List<String> errors = new ArrayList<>();
        for (LearningDtos.ReadinessResult.WeakTopic t : data.weakTopics) {
            if (t.topicId != null && !t.topicId.isBlank() && !allowedTopicIds.contains(t.topicId.trim())) {
                errors.add("weak topic \"" + t.topic + "\" references topicId \"" + t.topicId
                        + "\" which is not a confirmed topic for this exam.");
            }
        }
        return errors;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
