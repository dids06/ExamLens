package com.examlens.service;

import com.examlens.ai.AiAction;
import com.examlens.ai.AiOutcome;
import com.examlens.ai.AiServiceRunner;
import com.examlens.ai.dto.PlanDtos;
import com.examlens.ai.validate.AiValidators;
import com.examlens.dao.LearningDao;
import com.examlens.dao.PlanDao;
import com.examlens.model.Exam;
import com.examlens.model.PlanChangeExplanation;
import com.examlens.model.PlanVersion;
import com.examlens.model.StudyPlan;
import com.examlens.model.StudyTask;
import com.examlens.model.Topic;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The educational decision engine. Gemini (P06/P08/P17) decides priorities, durations, ordering,
 * revision/practice allocation, feasibility and replanning. Java only orchestrates the calls,
 * validates the returned schedule against hard constraints (ownership, dates, capacity, prerequisites),
 * and persists the validated result. It never substitutes a deterministic plan when AI output is invalid.
 */
public class StudyPlanService {

    private final PlanDao plans;
    private final LearningDao learning;
    private final SyllabusService syllabus;
    private final ExamService exams;
    private final AiServiceRunner ai;

    public StudyPlanService(PlanDao plans, LearningDao learning, SyllabusService syllabus,
                            ExamService exams, AiServiceRunner ai) {
        this.plans = plans;
        this.learning = learning;
        this.syllabus = syllabus;
        this.exams = exams;
        this.ai = ai;
    }

    public record DayGroup(LocalDate date, List<StudyTask> tasks, int minutes) {
        public LocalDate getDate() { return date; }
        public List<StudyTask> getTasks() { return tasks; }
        public int getMinutes() { return minutes; }
    }

    public record PlanView(StudyPlan plan, PlanVersion version, List<StudyTask> tasks,
                           List<DayGroup> days, List<PlanVersion> versions,
                           int totalTasks, int completedTasks, int plannedMinutes,
                           int availableCapacityMinutes, Optional<PlanChangeExplanation> explanation) {
        public int completionPercent() {
            return totalTasks == 0 ? 0 : (int) Math.round(completedTasks * 100.0 / totalTasks);
        }

        public StudyPlan getPlan() { return plan; }
        public PlanVersion getVersion() { return version; }
        public List<StudyTask> getTasks() { return tasks; }
        public List<DayGroup> getDays() { return days; }
        public List<PlanVersion> getVersions() { return versions; }
        public int getTotalTasks() { return totalTasks; }
        public int getCompletedTasks() { return completedTasks; }
        public int getPlannedMinutes() { return plannedMinutes; }
        public int getAvailableCapacityMinutes() { return availableCapacityMinutes; }
        public Optional<PlanChangeExplanation> getExplanation() { return explanation; }
        public int getCompletionPercent() { return completionPercent(); }
    }

    // ------------------------------------------------------------------ feasibility (P07)

    public AiOutcome<PlanDtos.FeasibilityResult> assessFeasibility(long userId, long examId) {
        Exam exam = exams.require(userId, examId);
        List<Topic> topics = syllabus.confirmedTopics(userId, examId);
        if (topics.isEmpty()) {
            throw new ServiceException("Confirm your syllabus topics before assessing feasibility.");
        }
        List<AvailabilityDay> days = exams.availabilityDays(exam);
        if (days.isEmpty()) {
            throw new ServiceException("Add your available study time before assessing feasibility.");
        }
        ObjectNode input = AiInputs.object();
        input.put("subjectName", exam.getSubjectName());
        input.set("availability", AiInputs.availability(days));
        ArrayNode workload = Json.array();
        int total = 0;
        for (Topic t : topics) {
            int minutes = t.getEstimatedMinutes() == null ? 60 : t.getEstimatedMinutes();
            total += minutes;
            ObjectNode node = AiInputs.object();
            node.put("topicId", String.valueOf(t.getId()));
            node.put("title", t.getTitle());
            node.put("minutes", minutes);
            workload.add(node);
        }
        input.set("topicWorkloadMinutes", workload);
        input.put("revisionPracticeReserveMinutes", (int) Math.round(total * 0.2));
        return ai.run(AiAction.P07_FEASIBILITY_ASSESSMENT, userId, examId, input,
                AiValidators.feasibility(), null);
    }

    // ------------------------------------------------------------------ generation (P06)

    public long generate(long userId, long examId) {
        Exam exam = exams.require(userId, examId);
        List<Topic> topics = syllabus.confirmedTopics(userId, examId);
        if (topics.isEmpty()) {
            throw new ServiceException("Confirm your syllabus topics before generating a plan.");
        }
        List<AvailabilityDay> days = exams.availabilityDays(exam);
        if (days.isEmpty()) {
            throw new ServiceException("Add your available study time first (Weekly availability or default study time).");
        }
        LocalDate today = LocalDate.now();
        LocalDate examDate = exam.getExamDate();
        Set<String> confirmedIds = new LinkedHashSet<>();
        for (Topic t : topics) {
            confirmedIds.add(String.valueOf(t.getId()));
        }
        Map<LocalDate, Integer> capacity = capacityByDate(days);

        ObjectNode input = AiInputs.object();
        input.put("subjectName", exam.getSubjectName());
        if (exam.getExamDatetime() != null) {
            input.put("examDateTime", exam.getExamDatetime().toString());
        }
        if (exam.getTargetMarks() != null) {
            input.put("targetMarks", exam.getTargetMarks());
        }
        input.set("topics", AiInputs.topics(topics));
        input.set("availability", AiInputs.availability(days));
        input.put("availableMinutesTotal", exams.totalAvailableMinutes(days));
        input.set("teacherHints", teacherHints(userId, examId));
        input.set("pyqPatterns", pyqPatterns(userId, examId));

        AiOutcome<PlanDtos.StudyPlanResult> outcome = ai.run(
                AiAction.P06_STUDY_PLAN_GENERATION, userId, examId, input,
                AiValidators.studyPlan(),
                data -> generationDomainCheck(data.tasks, confirmedIds, today, examDate, capacity));

        return persistNewVersion(userId, examId, exam, "INITIAL", outcome.getJobId(),
                outcome.getData().summary, outcome.getData().feasibility, outcome.getData().feasibilityNote,
                outcome.getData().assumptions, outcome.getData().warnings, outcome.getData().risks,
                outcome.getData().tradeoffs, outcome.getData().unscheduled,
                outcome.getData().totalEstimatedMinutes, exams.totalAvailableMinutes(days),
                null, outcome.getData().tasks);
    }

    // ------------------------------------------------------------------ replanning (P08)

    public long replan(long userId, long examId) {
        Exam exam = exams.require(userId, examId);
        Optional<PlanVersion> activeOpt = activeVersion(userId, examId);
        PlanVersion active = activeOpt.orElseThrow(() ->
                new ServiceException("Generate a plan before replanning."));
        List<StudyTask> current = tasksForVersion(userId, active.getId());
        if (current.isEmpty()) {
            throw new ServiceException("The active plan has no tasks to replan.");
        }
        List<Topic> topics = syllabus.confirmedTopics(userId, examId);
        Set<String> confirmedIds = new LinkedHashSet<>();
        for (Topic t : topics) {
            confirmedIds.add(String.valueOf(t.getId()));
        }
        List<AvailabilityDay> days = exams.availabilityDays(exam);
        Map<LocalDate, Integer> capacity = capacityByDate(days);

        Set<String> existingKeys = new LinkedHashSet<>();
        Set<String> frozenKeys = new LinkedHashSet<>();
        ArrayNode currentTasks = Json.array();
        for (StudyTask t : current) {
            existingKeys.add(t.getTaskKey());
            if (t.isCompleted() || "SKIPPED".equals(t.getStatus())) {
                frozenKeys.add(t.getTaskKey());
            }
            ObjectNode node = AiInputs.object();
            node.put("taskKey", t.getTaskKey());
            node.put("existingTaskKey", t.getTaskKey());
            if (t.getTopicId() != null) {
                node.put("topicId", String.valueOf(t.getTopicId()));
            }
            node.put("topicTitle", t.getTitle());
            node.put("taskType", t.getTaskType());
            node.put("title", t.getTitle());
            node.put("objective", t.getObjective() == null ? "" : t.getObjective());
            node.put("studyMethod", t.getStudyMethod() == null ? "" : t.getStudyMethod());
            node.put("estimatedMinutes", t.getEstimatedMinutes());
            if (t.getScheduledDate() != null) {
                node.put("date", t.getScheduledDate().toString());
            }
            node.put("order", t.getTaskOrder());
            if (t.getPriority() != null) {
                node.put("priority", t.getPriority());
            }
            node.put("status", t.getStatus());
            currentTasks.add(node);
        }

        ObjectNode currentPlan = AiInputs.object();
        currentPlan.put("versionNumber", active.getVersionNumber());
        currentPlan.set("tasks", currentTasks);

        ObjectNode input = AiInputs.object();
        input.put("subjectName", exam.getSubjectName());
        if (exam.getExamDatetime() != null) {
            input.put("examDateTime", exam.getExamDatetime().toString());
        }
        input.set("currentPlan", currentPlan);
        input.set("remainingAvailability", AiInputs.availability(days));
        input.set("topics", AiInputs.topics(topics));

        LocalDate today = LocalDate.now();
        LocalDate examDate = exam.getExamDate();
        AiOutcome<PlanDtos.ReplanResult> outcome = ai.run(
                AiAction.P08_ADAPTIVE_REPLANNING, userId, examId, input,
                AiValidators.replan(),
                data -> replanDomainCheck(data, confirmedIds, today, examDate, capacity, existingKeys, frozenKeys));

        PlanDtos.ReplanResult result = outcome.getData();
        long versionId = persistNewVersion(userId, examId, exam, "REPLAN", outcome.getJobId(),
                result.summary, result.feasibility, result.changeSummary,
                result.assumptions, result.warnings, List.of(), List.of(), List.of(),
                result.totalEstimatedMinutes, exams.totalAvailableMinutes(days),
                result.changeSummary, result.tasks);

        explainChanges(userId, examId, active, versionId, current, result.tasks);
        return versionId;
    }

    private void explainChanges(long userId, long examId, PlanVersion previous, long newVersionId,
                                List<StudyTask> previousTasks, List<PlanDtos.StudyPlanResult.PlannedTask> newTasks) {
        Map<String, StudyTask> oldByKey = new LinkedHashMap<>();
        for (StudyTask t : previousTasks) {
            oldByKey.put(t.getTaskKey(), t);
        }
        ArrayNode changes = Json.array();
        Set<String> seenNew = new LinkedHashSet<>();
        for (PlanDtos.StudyPlanResult.PlannedTask t : newTasks) {
            String key = t.existingTaskKey != null && !t.existingTaskKey.isBlank() ? t.existingTaskKey : t.taskKey;
            seenNew.add(key);
            StudyTask old = oldByKey.get(key);
            if (old == null) {
                changes.add(change("ADDED", t.title, "A new task was added to the remaining schedule."));
            } else if (old.getScheduledDate() != null && t.date != null && !old.getScheduledDate().toString().equals(t.date)) {
                changes.add(change("MOVED", t.title, "Moved from " + old.getScheduledDate() + " to " + t.date + "."));
            } else if (t.estimatedMinutes != null && old.getEstimatedMinutes() != t.estimatedMinutes) {
                changes.add(change("UPDATED", t.title, "Duration changed from " + old.getEstimatedMinutes()
                        + " to " + t.estimatedMinutes + " minutes."));
            }
        }
        for (StudyTask old : previousTasks) {
            if (!seenNew.contains(old.getTaskKey()) && !old.isCompleted()) {
                changes.add(change("REMOVED", old.getTitle(), "This pending task is no longer in the revised plan."));
            }
        }
        if (changes.isEmpty()) {
            return;
        }
        ObjectNode input = AiInputs.object();
        input.set("changes", changes);
        input.put("previousVersion", previous.getVersionNumber());
        try {
            AiOutcome<PlanDtos.PlanChangeExplanationResult> outcome = ai.run(
                    AiAction.P18_PLAN_CHANGE_EXPLANATIONS, userId, examId, input,
                    AiValidators.planChangeExplanation(), null);
            PlanDtos.PlanChangeExplanationResult result = outcome.getData();
            PlanChangeExplanation explanation = new PlanChangeExplanation();
            explanation.setPlanVersionId(newVersionId);
            explanation.setAiJobId(outcome.getJobId());
            explanation.setSummary(result.summary);
            explanation.setChangesJson(Json.write(result.changes));
            explanation.setUnchangedPriorities(Json.write(result.unchangedPriorities));
            explanation.setActionItems(Json.write(result.studentActionItems));
            plans.insertChangeExplanation(explanation);
        } catch (SQLException e) {
            throw new ServiceException("The plan was saved, but its change explanation could not be stored.", e);
        }
    }

    private static ObjectNode change(String type, String title, String detail) {
        ObjectNode node = AiInputs.object();
        node.put("type", type);
        node.put("title", title == null ? "" : title);
        node.put("detail", detail);
        return node;
    }

    // ------------------------------------------------------------------ last-minute (P17)

    public long lastMinute(long userId, long examId) {
        Exam exam = exams.require(userId, examId);
        List<Topic> topics = syllabus.confirmedTopics(userId, examId);
        if (topics.isEmpty()) {
            throw new ServiceException("Confirm your syllabus topics before requesting last-minute revision.");
        }
        List<AvailabilityDay> days = exams.availabilityDays(exam);
        if (days.isEmpty()) {
            throw new ServiceException("There are no remaining study days before the exam.");
        }
        Set<Long> completedTopicIds = completedTopicIds(userId, examId);

        ObjectNode input = AiInputs.object();
        input.put("subjectName", exam.getSubjectName());
        if (exam.getExamDatetime() != null) {
            input.put("examDeadline", exam.getExamDatetime().toString());
        }
        input.set("availability", AiInputs.availability(days));
        ArrayNode incomplete = Json.array();
        for (Topic t : topics) {
            if (!completedTopicIds.contains(t.getId())) {
                ObjectNode node = AiInputs.object();
                node.put("topicId", String.valueOf(t.getId()));
                node.put("title", t.getTitle());
                if (t.getImportance() != null) {
                    node.put("importance", t.getImportance());
                }
                incomplete.add(node);
            }
        }
        input.set("incompleteTopics", incomplete);
        input.put("availableMinutesTotal", exams.totalAvailableMinutes(days));

        AiOutcome<PlanDtos.LastMinuteRevisionResult> outcome = ai.run(
                AiAction.P17_LAST_MINUTE_REVISION, userId, examId, input,
                AiValidators.lastMinute(),
                data -> lastMinuteDomainCheck(data, topicIds(topics)));

        PlanDtos.LastMinuteRevisionResult result = outcome.getData();
        List<PlanDtos.StudyPlanResult.PlannedTask> tasks = packPriorityBlocks(result.priorityBlocks, days);
        List<String> warnings = new ArrayList<>(result.warnings);
        warnings.addAll(result.tradeoffs);
        return persistNewVersion(userId, examId, exam, "LAST_MINUTE", outcome.getJobId(),
                result.summary, result.feasibility, null, result.assumptions, warnings,
                List.of(), result.tradeoffs, result.deferredItems,
                tasks.stream().mapToInt(t -> t.estimatedMinutes == null ? 0 : t.estimatedMinutes).sum(),
                exams.totalAvailableMinutes(days), null, tasks);
    }

    /** Simple Java packing of the AI-ordered priority blocks into the remaining available days. */
    private List<PlanDtos.StudyPlanResult.PlannedTask> packPriorityBlocks(
            List<PlanDtos.LastMinuteRevisionResult.PriorityBlock> blocks, List<AvailabilityDay> days) {
        List<PlanDtos.StudyPlanResult.PlannedTask> tasks = new ArrayList<>();
        int dayIndex = 0;
        int remainingToday = days.isEmpty() ? 0 : days.get(0).minutes();
        int order = 0;
        for (PlanDtos.LastMinuteRevisionResult.PriorityBlock block : blocks) {
            int minutes = block.recommendedMinutes == null ? 30 : block.recommendedMinutes;
            int remaining = minutes;
            boolean scheduled = false;
            while (remaining > 0 && dayIndex < days.size()) {
                if (remainingToday <= 0) {
                    dayIndex++;
                    if (dayIndex >= days.size()) {
                        break;
                    }
                    remainingToday = days.get(dayIndex).minutes();
                    continue;
                }
                int chunk = Math.min(remaining, remainingToday);
                PlanDtos.StudyPlanResult.PlannedTask task = new PlanDtos.StudyPlanResult.PlannedTask();
                task.taskKey = "lm" + (++order);
                task.topicId = block.topicId;
                task.topicTitle = block.topic;
                task.taskType = "REVISION";
                task.title = "Revision: " + block.topic;
                task.objective = block.objective;
                task.studyMethod = block.activity;
                task.estimatedMinutes = chunk;
                task.date = days.get(dayIndex).date().toString();
                task.order = order;
                task.priority = "HIGH";
                task.rationale = "Last-minute priority block selected by the AI.";
                tasks.add(task);
                remaining -= chunk;
                remainingToday -= chunk;
                scheduled = true;
            }
            if (!scheduled) {
                break;
            }
        }
        return tasks;
    }

    // ------------------------------------------------------------------ persistence

    private long persistNewVersion(long userId, long examId, Exam exam, String kind, long aiJobId,
                                   String summary, String feasibility, String feasibilityNote,
                                   List<String> assumptions, List<String> warnings, List<String> risks,
                                   List<String> tradeoffs, List<String> unscheduled,
                                   Integer totalEstimatedMinutes, int availableCapacity, String changeSummary,
                                   List<PlanDtos.StudyPlanResult.PlannedTask> tasks) {
        try {
            long planId = plans.ensurePlan(examId, userId);
            int versionNumber = plans.nextVersionNumber(planId);
            PlanVersion version = new PlanVersion();
            version.setPlanId(planId);
            version.setVersionNumber(versionNumber);
            version.setKind(kind);
            version.setStatus("ACTIVE");
            version.setAiJobId(aiJobId);
            version.setSummary(summary);
            version.setFeasibility(feasibility);
            version.setFeasibilityNote(feasibilityNote);
            version.setAssumptions(Json.write(assumptions));
            version.setWarnings(Json.write(warnings));
            version.setRisks(Json.write(risks));
            version.setTradeoffs(Json.write(tradeoffs));
            version.setUnscheduledWork(Json.write(unscheduled));
            version.setTotalEstimatedMinutes(totalEstimatedMinutes);
            version.setAvailableCapacityMinutes(availableCapacity);
            version.setChangeSummary(changeSummary);

            plans.archiveVersions(planId);
            long versionId = plans.insertPlanVersion(version);
            persistTasks(versionId, examId, userId, tasks);
            plans.setActiveVersion(planId, versionId);
            return versionId;
        } catch (SQLException e) {
            throw new ServiceException("The plan could not be saved.", e);
        }
    }

    private void persistTasks(long versionId, long examId, long userId,
                              List<PlanDtos.StudyPlanResult.PlannedTask> tasks) throws SQLException {
        Map<String, Long> keyToId = new HashMap<>();
        int fallbackOrder = 0;
        for (PlanDtos.StudyPlanResult.PlannedTask t : tasks) {
            StudyTask task = new StudyTask();
            task.setPlanVersionId(versionId);
            task.setTaskKey(t.taskKey);
            task.setExamId(examId);
            task.setUserId(userId);
            task.setTopicId(parseLong(t.topicId));
            task.setTaskType(t.taskType);
            task.setTitle(t.title);
            task.setObjective(t.objective);
            task.setStudyMethod(t.studyMethod);
            task.setEstimatedMinutes(t.estimatedMinutes == null ? 30 : t.estimatedMinutes);
            task.setScheduledDate(parseDate(t.date));
            task.setTaskOrder(t.order == null ? ++fallbackOrder : t.order);
            task.setPriority(t.priority);
            task.setStatus("PENDING");
            task.setAiRationale(t.rationale);
            task.setRevisionOfTaskKey(t.revisionOfTaskKey);
            task.setChangeType(t.existingTaskKey != null && !t.existingTaskKey.isBlank()
                    && !t.existingTaskKey.equals(t.taskKey) ? "MOVED" : null);
            long taskId = plans.insertTask(task);
            keyToId.put(t.taskKey, taskId);
        }
        for (PlanDtos.StudyPlanResult.PlannedTask t : tasks) {
            Long id = keyToId.get(t.taskKey);
            if (id == null || t.prerequisites == null) {
                continue;
            }
            for (String prerequisite : t.prerequisites) {
                if (keyToId.containsKey(prerequisite)) {
                    plans.insertPrerequisite(id, prerequisite);
                }
            }
        }
    }

    // ------------------------------------------------------------------ reads

    public Optional<PlanVersion> activeVersion(long userId, long examId) {
        try {
            return plans.activeVersion(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the active plan.", e);
        }
    }

    public Optional<PlanView> loadPlan(long userId, long examId) {
        Optional<PlanVersion> activeOpt = activeVersion(userId, examId);
        if (activeOpt.isEmpty()) {
            return Optional.empty();
        }
        PlanVersion active = activeOpt.get();
        List<StudyTask> tasks = tasksForVersion(userId, active.getId());
        Map<LocalDate, List<StudyTask>> grouped = new LinkedHashMap<>();
        for (StudyTask t : tasks) {
            if (t.getScheduledDate() != null) {
                grouped.computeIfAbsent(t.getScheduledDate(), k -> new ArrayList<>()).add(t);
            }
        }
        List<DayGroup> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<StudyTask>> e : grouped.entrySet()) {
            int minutes = e.getValue().stream().mapToInt(StudyTask::getEstimatedMinutes).sum();
            days.add(new DayGroup(e.getKey(), e.getValue(), minutes));
        }
        days.sort(java.util.Comparator.comparing(DayGroup::date));
        int completed = (int) tasks.stream().filter(StudyTask::isCompleted).count();
        int planned = tasks.stream().mapToInt(StudyTask::getEstimatedMinutes).sum();
        Optional<PlanChangeExplanation> explanation = Optional.empty();
        try {
            explanation = plans.changeExplanation(active.getId());
        } catch (SQLException ignored) {
            // explanation is supplementary
        }
        StudyPlan plan;
        try {
            plan = plans.findPlan(examId, userId).orElse(null);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the plan.", e);
        }
        return Optional.of(new PlanView(plan, active, tasks, days, versions(userId, examId),
                tasks.size(), completed, planned,
                active.getAvailableCapacityMinutes() == null ? 0 : active.getAvailableCapacityMinutes(),
                explanation));
    }

    public List<StudyTask> tasksForVersion(long userId, long versionId) {
        try {
            return plans.tasksForVersion(versionId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load plan tasks.", e);
        }
    }

    public List<PlanVersion> versions(long userId, long examId) {
        try {
            return plans.versionsForExam(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load plan versions.", e);
        }
    }

    public List<StudyTask> upcoming(long userId, long examId, int limit) {
        try {
            return plans.upcomingTasks(examId, userId, limit);
        } catch (SQLException e) {
            throw new ServiceException("Could not load upcoming tasks.", e);
        }
    }

    public Optional<StudyTask> findTask(long userId, long taskId) {
        if (taskId <= 0) {
            return Optional.empty();
        }
        try {
            return plans.findTask(taskId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the task.", e);
        }
    }

    public void updateTaskStatus(long userId, long taskId, String status) {
        String safeStatus = switch (status == null ? "" : status) {
            case "COMPLETED", "PENDING", "IN_PROGRESS", "SKIPPED" -> status;
            default -> null;
        };
        if (safeStatus == null) {
            throw new ServiceException("Unsupported task status.");
        }
        findTask(userId, taskId).orElseThrow(() -> new ServiceException("That task was not found."));
        try {
            plans.updateTaskStatus(taskId, userId, safeStatus);
        } catch (SQLException e) {
            throw new ServiceException("Could not update the task.", e);
        }
    }

    public int[] taskStats(long userId, long examId) {
        try {
            return plans.taskStats(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load plan statistics.", e);
        }
    }

    public int plannedMinutes(long userId, long examId) {
        try {
            return plans.plannedMinutes(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load planned minutes.", e);
        }
    }

    // ------------------------------------------------------------------ domain checks

    private List<String> generationDomainCheck(List<PlanDtos.StudyPlanResult.PlannedTask> tasks,
                                               Set<String> confirmedIds, LocalDate today, LocalDate examDate,
                                               Map<LocalDate, Integer> capacity) {
        List<String> errors = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        Map<LocalDate, Integer> used = new HashMap<>();
        for (PlanDtos.StudyPlanResult.PlannedTask t : tasks) {
            if (t.taskKey != null) {
                keys.add(t.taskKey);
            }
            checkTopic(t.topicId, confirmedIds, t.taskKey, errors);
            checkDate(t.date, today, examDate, t.taskKey, errors);
            if (t.date != null && t.estimatedMinutes != null) {
                LocalDate date = safeDate(t.date);
                if (date != null) {
                    used.merge(date, t.estimatedMinutes, Integer::sum);
                }
            }
        }
        for (PlanDtos.StudyPlanResult.PlannedTask t : tasks) {
            if (t.prerequisites != null) {
                for (String prerequisite : t.prerequisites) {
                    if (!keys.contains(prerequisite)) {
                        errors.add("task \"" + t.taskKey + "\" lists prerequisite \"" + prerequisite
                                + "\" which is not part of the returned schedule.");
                    }
                }
            }
        }
        for (Map.Entry<LocalDate, Integer> e : used.entrySet()) {
            Integer cap = capacity.get(e.getKey());
            if (cap == null) {
                errors.add("the schedule places " + e.getValue() + " minutes on " + e.getKey()
                        + ", which is not one of the available study days.");
            } else if (e.getValue() > cap) {
                errors.add("the schedule places " + e.getValue() + " minutes on " + e.getKey()
                        + ", exceeding the " + cap + " available minutes for that day.");
            }
        }
        if (used.isEmpty()) {
            errors.add("no dated tasks were produced; distribute the work across the available days.");
        }
        return errors;
    }

    private List<String> replanDomainCheck(PlanDtos.ReplanResult data, Set<String> confirmedIds, LocalDate today,
                                           LocalDate examDate, Map<LocalDate, Integer> capacity,
                                           Set<String> existingKeys, Set<String> frozenKeys) {
        List<String> errors = new ArrayList<>();
        Map<LocalDate, Integer> used = new HashMap<>();
        Set<String> keys = new LinkedHashSet<>();
        for (PlanDtos.StudyPlanResult.PlannedTask t : data.tasks) {
            if (t.taskKey != null) {
                keys.add(t.taskKey);
            }
        }
        for (PlanDtos.StudyPlanResult.PlannedTask t : data.tasks) {
            checkTopic(t.topicId, confirmedIds, t.taskKey, errors);
            String originKey = t.existingTaskKey != null && !t.existingTaskKey.isBlank() ? t.existingTaskKey : t.taskKey;
            boolean frozen = frozenKeys.contains(originKey);
            if (!frozen) {
                checkDate(t.date, today, examDate, t.taskKey, errors);
                if (t.date != null && t.estimatedMinutes != null) {
                    LocalDate date = safeDate(t.date);
                    if (date != null) {
                        used.merge(date, t.estimatedMinutes, Integer::sum);
                    }
                }
            }
            if (t.prerequisites != null) {
                for (String prerequisite : t.prerequisites) {
                    if (!keys.contains(prerequisite)) {
                        errors.add("task \"" + t.taskKey + "\" lists prerequisite \"" + prerequisite
                                + "\" which is not part of the revised plan.");
                    }
                }
            }
        }
        if (data.preservedTaskKeys != null) {
            for (String key : data.preservedTaskKeys) {
                if (!existingKeys.contains(key)) {
                    errors.add("preservedTaskKeys references \"" + key + "\" which is not an existing task.");
                }
            }
        }
        if (data.removedTaskKeys != null) {
            for (String key : data.removedTaskKeys) {
                if (!existingKeys.contains(key)) {
                    errors.add("removedTaskKeys references \"" + key + "\" which is not an existing task.");
                }
            }
        }
        for (Map.Entry<LocalDate, Integer> e : used.entrySet()) {
            Integer cap = capacity.get(e.getKey());
            if (cap == null) {
                errors.add("the revised schedule places " + e.getValue() + " minutes on " + e.getKey()
                        + ", which is not one of the available study days.");
            } else if (e.getValue() > cap) {
                errors.add("the revised schedule places " + e.getValue() + " minutes on " + e.getKey()
                        + ", exceeding the " + cap + " available minutes.");
            }
        }
        return errors;
    }

    private List<String> lastMinuteDomainCheck(PlanDtos.LastMinuteRevisionResult data, Set<String> confirmedIds) {
        List<String> errors = new ArrayList<>();
        for (PlanDtos.LastMinuteRevisionResult.PriorityBlock b : data.priorityBlocks) {
            if (b.topicId != null && !b.topicId.isBlank() && !confirmedIds.contains(b.topicId.trim())) {
                errors.add("priority block for \"" + b.topic + "\" references topicId \"" + b.topicId
                        + "\" which is not a confirmed topic.");
            }
        }
        return errors;
    }

    private void checkTopic(String topicId, Set<String> confirmedIds, String taskKey, List<String> errors) {
        if (topicId != null && !topicId.isBlank() && !confirmedIds.contains(topicId.trim())) {
            errors.add("task \"" + taskKey + "\" references topicId \"" + topicId
                    + "\" which is not a confirmed topic for this exam.");
        }
    }

    private void checkDate(String date, LocalDate today, LocalDate examDate, String taskKey, List<String> errors) {
        if (date == null || date.isBlank()) {
            return;
        }
        LocalDate parsed = safeDate(date);
        if (parsed == null) {
            return; // the structural validator already reports malformed dates
        }
        if (today != null && parsed.isBefore(today)) {
            errors.add("task \"" + taskKey + "\" is scheduled before today (" + date + ").");
        }
        if (examDate != null && parsed.isAfter(examDate)) {
            errors.add("task \"" + taskKey + "\" is scheduled after the exam date (" + date + ").");
        }
    }

    // ------------------------------------------------------------------ helpers

    private Map<LocalDate, Integer> capacityByDate(List<AvailabilityDay> days) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (AvailabilityDay d : days) {
            map.put(d.date(), d.minutes());
        }
        return map;
    }

    private Set<String> topicIds(List<Topic> topics) {
        Set<String> ids = new LinkedHashSet<>();
        for (Topic t : topics) {
            ids.add(String.valueOf(t.getId()));
        }
        return ids;
    }

    private Set<Long> completedTopicIds(long userId, long examId) {
        Set<Long> ids = new LinkedHashSet<>();
        Optional<PlanVersion> active = activeVersion(userId, examId);
        if (active.isEmpty()) {
            return ids;
        }
        for (StudyTask t : tasksForVersion(userId, active.get().getId())) {
            if (t.isCompleted() && t.getTopicId() != null) {
                ids.add(t.getTopicId());
            }
        }
        return ids;
    }

    private ArrayNode teacherHints(long userId, long examId) {
        try {
            return learning.latestTeacherGuidance(examId, userId)
                    .map(g -> {
                        try {
                            return (ArrayNode) Json.read(g.getHints());
                        } catch (RuntimeException e) {
                            return Json.array();
                        }
                    })
                    .orElseGet(Json::array);
        } catch (SQLException e) {
            return Json.array();
        }
    }

    private ArrayNode pyqPatterns(long userId, long examId) {
        try {
            return learning.latestPyqAnalysis(examId, userId)
                    .map(a -> {
                        try {
                            return (ArrayNode) Json.read(a.getTopicFrequencyJson());
                        } catch (RuntimeException e) {
                            return Json.array();
                        }
                    })
                    .orElseGet(Json::array);
        } catch (SQLException e) {
            return Json.array();
        }
    }

    static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate safeDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        return safeDate(value);
    }
}
