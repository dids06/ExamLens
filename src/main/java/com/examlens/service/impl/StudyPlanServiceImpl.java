package com.examlens.service.impl;

import com.examlens.dao.StudyPlanDao;
import com.examlens.dao.StudyTaskDao;
import com.examlens.dao.impl.StudyPlanDaoImpl;
import com.examlens.dao.impl.StudyTaskDaoImpl;
import com.examlens.exception.AuthorizationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ResourceNotFoundException;
import com.examlens.exception.ValidationException;
import com.examlens.model.Exam;
import com.examlens.model.StudyPlan;
import com.examlens.model.StudyTask;
import com.examlens.model.Topic;
import com.examlens.service.ExamService;
import com.examlens.service.StudyPlanService;
import com.examlens.service.TopicService;
import com.examlens.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of StudyPlanService for deterministic baseline study-plan generation,
 * task scheduling, ownership enforcement, and transactional persistence.
 */
public class StudyPlanServiceImpl implements StudyPlanService {

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    private static final int DEFAULT_SESSION_START_HOUR = 9; // 09:00 AM
    private static final int DEFAULT_SESSION_START_MINUTE = 0;

    private final StudyPlanDao studyPlanDao;
    private final StudyTaskDao studyTaskDao;
    private final ExamService examService;
    private final TopicService topicService;
    private final ConnectionProvider connectionProvider;

    public StudyPlanServiceImpl() {
        this(new StudyPlanDaoImpl(), new StudyTaskDaoImpl(), new ExamServiceImpl(), new TopicServiceImpl(), DatabaseUtil::getConnection);
    }

    public StudyPlanServiceImpl(StudyPlanDao studyPlanDao, StudyTaskDao studyTaskDao,
                                ExamService examService, TopicService topicService) {
        this(studyPlanDao, studyTaskDao, examService, topicService, DatabaseUtil::getConnection);
    }

    public StudyPlanServiceImpl(StudyPlanDao studyPlanDao, StudyTaskDao studyTaskDao,
                                ExamService examService, TopicService topicService,
                                ConnectionProvider connectionProvider) {
        if (studyPlanDao == null) throw new IllegalArgumentException("StudyPlanDao cannot be null.");
        if (studyTaskDao == null) throw new IllegalArgumentException("StudyTaskDao cannot be null.");
        if (examService == null) throw new IllegalArgumentException("ExamService cannot be null.");
        if (topicService == null) throw new IllegalArgumentException("TopicService cannot be null.");
        if (connectionProvider == null) throw new IllegalArgumentException("ConnectionProvider cannot be null.");

        this.studyPlanDao = studyPlanDao;
        this.studyTaskDao = studyTaskDao;
        this.examService = examService;
        this.topicService = topicService;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public StudyPlan generateInitialPlan(Long examId, Long userId)
            throws ValidationException, ResourceNotFoundException, AuthorizationException, DataAccessException {

        if (examId == null) {
            throw new ValidationException("Exam ID is required to generate a study plan.");
        }
        if (userId == null) {
            throw new AuthorizationException("Authentication required to generate a study plan.");
        }

        // 1. Verify exam existence and student ownership
        Exam exam = examService.getExam(examId, userId);

        // 2. Validate exam timing and daily study limits
        LocalDateTime now = LocalDateTime.now();
        if (exam.getExamDateTime() == null || !exam.getExamDateTime().isAfter(now)) {
            throw new ValidationException("Cannot generate a study plan for an exam that is not set in the future.");
        }

        if (exam.getDailyStudyMinutes() == null || exam.getDailyStudyMinutes() <= 0) {
            throw new ValidationException("Daily study minutes must be positive to generate a study plan.");
        }

        // 3. Prevent duplicate initial plans: if plan already exists for this exam, return current plan
        Optional<StudyPlan> existingPlanOpt = studyPlanDao.findLatestByExamId(examId);
        if (existingPlanOpt.isPresent()) {
            StudyPlan existingPlan = existingPlanOpt.get();
            List<StudyTask> existingTasks = studyTaskDao.findAllByPlanId(existingPlan.getId());
            existingPlan.setTasks(existingTasks);
            return existingPlan;
        }

        // 4. Validate syllabus topics exist
        List<Topic> topics = topicService.getTopicsForExam(examId, userId);
        if (topics == null || topics.isEmpty()) {
            throw new ValidationException("Cannot generate a study plan without syllabus topics. Please add syllabus topics first.");
        }

        // 5. Generate deterministic baseline schedule
        List<Topic> orderedTopics = sortTopicsByPriorityAndHierarchy(topics);
        PlanScheduleResult scheduleResult = buildSchedule(exam, orderedTopics, now);

        // 6. Create StudyPlan entity
        StudyPlan plan = new StudyPlan();
        plan.setExamId(examId);
        plan.setVersion(1);
        plan.setCreatedAt(now);
        plan.setChangeReason(scheduleResult.getChangeReason());

        // 7. Transactional persistence: Plan and all Tasks committed atomically
        return persistPlanAndTasksTransactional(plan, scheduleResult.getTasks());
    }

    @Override
    public StudyPlan getCurrentPlan(Long examId, Long userId)
            throws ResourceNotFoundException, AuthorizationException, DataAccessException {

        if (examId == null) {
            throw new ResourceNotFoundException("Exam ID cannot be null.");
        }
        if (userId == null) {
            throw new AuthorizationException("Authentication required to view study plan.");
        }

        // Verify exam existence and student ownership
        examService.getExam(examId, userId);

        Optional<StudyPlan> planOpt = studyPlanDao.findLatestByExamId(examId);
        if (planOpt.isEmpty()) {
            return null;
        }

        StudyPlan plan = planOpt.get();
        List<StudyTask> tasks = studyTaskDao.findAllByPlanId(plan.getId());
        plan.setTasks(tasks);
        return plan;
    }

    @Override
    public StudyPlan reschedule(Long examId, Long userId, String reason)
            throws ValidationException, ResourceNotFoundException, AuthorizationException, DataAccessException {

        if (examId == null) {
            throw new ValidationException("Exam ID is required to reschedule a plan.");
        }
        if (userId == null) {
            throw new AuthorizationException("Authentication required to reschedule a plan.");
        }

        // Verify exam existence and student ownership
        Exam exam = examService.getExam(examId, userId);

        LocalDateTime now = LocalDateTime.now();
        if (exam.getExamDateTime() == null || !exam.getExamDateTime().isAfter(now)) {
            throw new ValidationException("Cannot reschedule a study plan for an exam that is not set in the future.");
        }

        List<Topic> topics = topicService.getTopicsForExam(examId, userId);
        if (topics == null || topics.isEmpty()) {
            throw new ValidationException("Cannot reschedule without syllabus topics.");
        }

        List<Topic> orderedTopics = sortTopicsByPriorityAndHierarchy(topics);
        PlanScheduleResult scheduleResult = buildSchedule(exam, orderedTopics, now);

        int nextVersion = studyPlanDao.getNextVersion(null, examId);
        String changeReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Adaptive rescheduling";
        if (scheduleResult.getShortfallMinutes() > 0) {
            changeReason += " (" + scheduleResult.getShortfallMinutes() + " mins shortfall)";
        }

        StudyPlan plan = new StudyPlan();
        plan.setExamId(examId);
        plan.setVersion(nextVersion);
        plan.setCreatedAt(now);
        plan.setChangeReason(changeReason);

        return persistPlanAndTasksTransactional(plan, scheduleResult.getTasks());
    }

    /**
     * Orders topics deterministically respecting parent-child prerequisite hierarchy
     * and study priority (TEACHER_IMPORTANT > HIGH > MEDIUM > LOW).
     */
    public List<Topic> sortTopicsByPriorityAndHierarchy(List<Topic> topics) {
        if (topics == null || topics.isEmpty()) {
            return List.of();
        }

        Map<Long, Topic> topicById = new HashMap<>();
        Map<Long, List<Topic>> childrenByParentId = new HashMap<>();
        List<Topic> topLevelTopics = new ArrayList<>();

        for (Topic t : topics) {
            topicById.put(t.getId(), t);
            if (t.isSubtopic() && t.getParentTopicId() != null) {
                childrenByParentId.computeIfAbsent(t.getParentTopicId(), k -> new ArrayList<>()).add(t);
            } else {
                topLevelTopics.add(t);
            }
        }

        // Comparator for priority ranking
        Comparator<Topic> priorityComparator = (t1, t2) -> {
            int r1 = getPriorityRank(t1.getPriority());
            int r2 = getPriorityRank(t2.getPriority());
            if (r1 != r2) {
                return Integer.compare(r2, r1); // Higher rank first
            }
            if (t1.getId() != null && t2.getId() != null) {
                return t1.getId().compareTo(t2.getId());
            }
            return t1.getName().compareToIgnoreCase(t2.getName());
        };

        topLevelTopics.sort(priorityComparator);

        List<Topic> ordered = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        // Add parent first, then recursively its children sorted by priority
        for (Topic parent : topLevelTopics) {
            scheduleTopicBranch(parent, childrenByParentId, priorityComparator, ordered, visited);
        }

        // Catch any orphaned subtopics whose parent was missing from the list
        for (Topic t : topics) {
            if (!visited.contains(t.getId())) {
                ordered.add(t);
                visited.add(t.getId());
            }
        }

        return ordered;
    }

    private void scheduleTopicBranch(Topic topic, Map<Long, List<Topic>> childrenByParentId,
                                      Comparator<Topic> comparator, List<Topic> ordered, Set<Long> visited) {
        if (topic == null || visited.contains(topic.getId())) {
            return;
        }

        ordered.add(topic);
        visited.add(topic.getId());

        List<Topic> children = childrenByParentId.get(topic.getId());
        if (children != null && !children.isEmpty()) {
            List<Topic> sortedChildren = new ArrayList<>(children);
            sortedChildren.sort(comparator);
            for (Topic child : sortedChildren) {
                scheduleTopicBranch(child, childrenByParentId, comparator, ordered, visited);
            }
        }
    }

    private int getPriorityRank(String priority) {
        if (priority == null) return 2;
        return switch (priority.trim().toUpperCase()) {
            case Topic.PRIORITY_TEACHER_IMPORTANT -> 4;
            case Topic.PRIORITY_HIGH -> 3;
            case Topic.PRIORITY_MEDIUM -> 2;
            case Topic.PRIORITY_LOW -> 1;
            default -> 2;
        };
    }

    /**
     * Builds deterministic task schedule respecting daily study minute limits,
     * available study window before examDateTime, and splitting topic effort when necessary.
     */
    private PlanScheduleResult buildSchedule(Exam exam, List<Topic> orderedTopics, LocalDateTime now) {
        LocalDateTime examDeadline = exam.getExamDateTime();
        int dailyStudyMinutes = exam.getDailyStudyMinutes();

        // Determine starting date for study
        LocalDate currentDate = now.toLocalDate();
        // If current time is late evening (>= 21:00), start tomorrow
        if (now.getHour() >= 21) {
            currentDate = currentDate.plusDays(1);
        }

        // Calculate available days and total available minutes
        LocalDate examDate = examDeadline.toLocalDate();
        int totalRequiredMinutes = 0;
        for (Topic t : orderedTopics) {
            int est = (t.getEstimatedMinutes() != null && t.getEstimatedMinutes() > 0) ? t.getEstimatedMinutes() : 60;
            totalRequiredMinutes += est;
        }

        List<StudyTask> scheduledTasks = new ArrayList<>();
        int scheduledMinutes = 0;

        // Queue of topics with remaining minutes to schedule
        record PendingTopic(Topic topic, int totalMinutes) {}
        List<PendingTopic> pendingList = new ArrayList<>();
        for (Topic t : orderedTopics) {
            int est = (t.getEstimatedMinutes() != null && t.getEstimatedMinutes() > 0) ? t.getEstimatedMinutes() : 60;
            pendingList.add(new PendingTopic(t, est));
        }

        int topicIndex = 0;
        int remainingTopicMinutes = (pendingList.isEmpty()) ? 0 : pendingList.get(0).totalMinutes();

        // Schedule day by day
        while (topicIndex < pendingList.size() && !currentDate.isAfter(examDate)) {
            // Determine day session start and max end
            LocalTime sessionStartTime = LocalTime.of(DEFAULT_SESSION_START_HOUR, DEFAULT_SESSION_START_MINUTE);
            LocalDateTime daySlotStart = currentDate.atTime(sessionStartTime);

            // If day is today and session start is already past, start now + 15 mins
            if (currentDate.isEqual(now.toLocalDate()) && daySlotStart.isBefore(now)) {
                daySlotStart = now.plusMinutes(15).withSecond(0).withNano(0);
            }

            // Max possible end time for this day
            LocalDateTime dayMaxEnd = currentDate.atTime(23, 0);
            if (currentDate.isEqual(examDate)) {
                // On exam day, study must finish at least 30 minutes before examDateTime
                dayMaxEnd = examDeadline.minusMinutes(30);
            }

            // If no study time left on this day, advance to next day
            if (!daySlotStart.isBefore(dayMaxEnd)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            int minutesUntilDayEnd = (int) java.time.Duration.between(daySlotStart, dayMaxEnd).toMinutes();
            int dayAvailableMinutes = Math.min(dailyStudyMinutes, minutesUntilDayEnd);

            if (dayAvailableMinutes <= 0) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            int dayMinutesLeft = dayAvailableMinutes;
            LocalDateTime currentTaskStart = daySlotStart;

            while (dayMinutesLeft > 0 && topicIndex < pendingList.size()) {
                Topic currentTopic = pendingList.get(topicIndex).topic();
                int taskMinutes = Math.min(remainingTopicMinutes, dayMinutesLeft);

                LocalDateTime currentTaskEnd = currentTaskStart.plusMinutes(taskMinutes);

                StudyTask task = new StudyTask();
                task.setTopicId(currentTopic.getId());
                task.setPlannedStart(currentTaskStart);
                task.setPlannedEnd(currentTaskEnd);
                task.setPlannedMinutes(taskMinutes);
                task.setActualMinutes(0);
                task.setStatus(StudyTask.STATUS_PLANNED);
                task.setCreatedAt(now);
                task.setTopicName(currentTopic.getName());
                task.setTopicPriority(currentTopic.getPriority());
                task.setTopicIsSubtopic(currentTopic.isSubtopic());

                scheduledTasks.add(task);
                scheduledMinutes += taskMinutes;

                dayMinutesLeft -= taskMinutes;
                remainingTopicMinutes -= taskMinutes;
                currentTaskStart = currentTaskEnd.plusMinutes(5); // 5-minute break between tasks

                if (remainingTopicMinutes <= 0) {
                    topicIndex++;
                    if (topicIndex < pendingList.size()) {
                        remainingTopicMinutes = pendingList.get(topicIndex).totalMinutes();
                    }
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        int shortfallMinutes = Math.max(0, totalRequiredMinutes - scheduledMinutes);
        String reason = StudyPlan.DEFAULT_INITIAL_REASON;
        if (shortfallMinutes > 0) {
            reason = "Initial baseline plan (Workload shortfall: " + totalRequiredMinutes + " mins required, " +
                    scheduledMinutes + " mins scheduled; shortfall: " + shortfallMinutes + " mins)";
        }

        return new PlanScheduleResult(scheduledTasks, totalRequiredMinutes, scheduledMinutes, shortfallMinutes, reason);
    }

    /**
     * Persists a StudyPlan and all its StudyTasks within an explicit JDBC transaction.
     * Rolls back on failure and restores original connection auto-commit settings.
     */
    private StudyPlan persistPlanAndTasksTransactional(StudyPlan plan, List<StudyTask> tasks)
            throws DataAccessException {

        Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            conn = connectionProvider.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // 1. Save StudyPlan
            studyPlanDao.save(conn, plan);

            // 2. Associate planId with all tasks
            for (StudyTask task : tasks) {
                task.setPlanId(plan.getId());
            }

            // 3. Save all tasks in batch
            studyTaskDao.saveAll(conn, tasks);

            conn.commit();

            plan.setTasks(tasks);
            return plan;

        } catch (SQLException | DataAccessException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    // Suppressed
                }
            }
            throw new DataAccessException("Transaction failed while saving study plan and tasks: " + e.getMessage(), e);

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                }
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private static class PlanScheduleResult {
        private final List<StudyTask> tasks;
        private final int totalRequiredMinutes;
        private final int scheduledMinutes;
        private final int shortfallMinutes;
        private final String changeReason;

        public PlanScheduleResult(List<StudyTask> tasks, int totalRequiredMinutes,
                                  int scheduledMinutes, int shortfallMinutes, String changeReason) {
            this.tasks = tasks;
            this.totalRequiredMinutes = totalRequiredMinutes;
            this.scheduledMinutes = scheduledMinutes;
            this.shortfallMinutes = shortfallMinutes;
            this.changeReason = changeReason;
        }

        public List<StudyTask> getTasks() {
            return tasks;
        }

        public int getTotalRequiredMinutes() {
            return totalRequiredMinutes;
        }

        public int getScheduledMinutes() {
            return scheduledMinutes;
        }

        public int getShortfallMinutes() {
            return shortfallMinutes;
        }

        public String getChangeReason() {
            return changeReason;
        }
    }
}
