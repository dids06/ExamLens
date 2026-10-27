package com.examlens.service;

import com.examlens.ai.AiAction;
import com.examlens.ai.AiOutcome;
import com.examlens.ai.AiServiceRunner;
import com.examlens.ai.dto.LearningDtos;
import com.examlens.ai.validate.AiValidators;
import com.examlens.dao.PlanDao;
import com.examlens.model.StudySession;
import com.examlens.model.StudyTask;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Study-session logging plus P16 feedback. The student records what actually happened; the AI
 * turns those recorded facts into constructive feedback. Java never invents session data and
 * never infers motivation or personal circumstances.
 */
public class SessionService {

    private final PlanDao plans;
    private final ExamService exams;
    private final StudyPlanService studyPlan;
    private final AiServiceRunner ai;

    public SessionService(PlanDao plans, ExamService exams, StudyPlanService studyPlan, AiServiceRunner ai) {
        this.plans = plans;
        this.exams = exams;
        this.studyPlan = studyPlan;
        this.ai = ai;
    }

    public record SessionResult(StudySession session, LearningDtos.SessionFeedbackResult feedback, boolean simulated) {
    }

    public SessionResult log(long userId, long examId, Long taskId, Integer plannedMinutes, int actualMinutes,
                             String difficultyFeedback, String notes, boolean markComplete) {
        exams.require(userId, examId);
        if (actualMinutes < 0 || actualMinutes > 24 * 60) {
            throw new ServiceException("Actual minutes must be between 0 and 1440.");
        }
        StudyTask task = null;
        if (taskId != null && taskId > 0) {
            task = studyPlan.findTask(userId, taskId)
                    .orElseThrow(() -> new ServiceException("That study task was not found."));
            if (task.getExamId() != examId) {
                throw new ServiceException("That study task belongs to a different exam.");
            }
        }

        StudySession session = new StudySession();
        session.setUserId(userId);
        session.setExamId(examId);
        session.setTaskId(task == null ? null : task.getId());
        Integer planned = task != null ? task.getEstimatedMinutes() : plannedMinutes;
        session.setPlannedMinutes(planned);
        session.setActualMinutes(actualMinutes);
        session.setStatus(markComplete ? "COMPLETED" : "LOGGED");
        session.setDifficultyFeedback(safeEnum(difficultyFeedback));
        session.setStudentNotes(trim(notes));
        session.setSessionDate(LocalDate.now());

        try {
            long sessionId = plans.insertSession(session);
            session.setId(sessionId);
            if (markComplete && task != null) {
                studyPlan.updateTaskStatus(userId, task.getId(), "COMPLETED");
            }

            ObjectNode input = AiInputs.object();
            input.put("plannedMinutes", planned == null ? 0 : planned);
            input.put("actualMinutes", actualMinutes);
            input.put("completed", markComplete);
            if (task != null) {
                input.put("taskTitle", task.getTitle());
                input.put("taskType", task.getTaskType());
                if (task.getObjective() != null) {
                    input.put("objective", task.getObjective());
                }
            }
            if (session.getDifficultyFeedback() != null) {
                input.put("difficultyFeedback", session.getDifficultyFeedback());
            }
            if (session.getStudentNotes() != null) {
                input.put("studentNotes", session.getStudentNotes());
            }

            AiOutcome<LearningDtos.SessionFeedbackResult> outcome = ai.run(
                    AiAction.P16_STUDY_SESSION_FEEDBACK, userId, examId, input,
                    AiValidators.sessionFeedback(), null);

            session.setAiJobId(outcome.getJobId());
            plans.attachSessionJob(sessionId, outcome.getJobId());
            return new SessionResult(session, outcome.getData(), outcome.isSimulated());
        } catch (SQLException e) {
            throw new ServiceException("The study session could not be saved.", e);
        }
    }

    public List<StudySession> sessions(long userId, long examId, int limit) {
        exams.require(userId, examId);
        try {
            return plans.sessionsForExam(examId, userId, limit);
        } catch (SQLException e) {
            throw new ServiceException("Could not load study sessions.", e);
        }
    }

    public List<StudySession> sessionsForTask(long userId, long taskId) {
        try {
            return plans.sessionsForTask(taskId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load sessions for this task.", e);
        }
    }

    public int actualMinutes(long userId, long examId) {
        try {
            return plans.actualMinutes(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not compute study minutes.", e);
        }
    }

    private static String safeEnum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String upper = value.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (upper) {
            case "EASIER", "AS_EXPECTED", "HARDER" -> upper;
            default -> null;
        };
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > 2000 ? t.substring(0, 2000) : t;
    }
}
