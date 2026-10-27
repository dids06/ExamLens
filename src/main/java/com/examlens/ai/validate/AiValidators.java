package com.examlens.ai.validate;

import com.examlens.ai.AiValidationException;
import com.examlens.ai.AiValidator;
import com.examlens.ai.dto.DocDtos;
import com.examlens.ai.dto.LearningDtos;
import com.examlens.ai.dto.PlanDtos;
import com.examlens.ai.dto.PracticeDtos;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.examlens.ai.dto.DocDtos.*;
import static com.examlens.ai.dto.LearningDtos.*;
import static com.examlens.ai.dto.PlanDtos.*;
import static com.examlens.ai.dto.PracticeDtos.*;

/** Structural validation for every AI action. Domain (database-aware) checks live in services. */
public final class AiValidators {

    private static final Set<String> IMPORTANCE = Set.of("HIGH", "MEDIUM", "LOW");
    private static final Set<String> DIFFICULTY = Set.of("HARD", "MEDIUM", "EASY");
    private static final Set<String> CONFIDENCE = Set.of("HIGH", "MEDIUM", "LOW");
    private static final Set<String> TASK_TYPES = Set.of("LEARN", "PRACTICE", "REVISION", "MOCK_TEST", "REVIEW", "GENERAL");
    private static final Set<String> QUESTION_TYPES = Set.of("MCQ", "SHORT_ANSWER", "THEORY", "NUMERICAL", "APPLICATION");
    private static final Set<String> FEASIBILITY = Set.of("FEASIBLE", "TIGHT", "INFEASIBLE");
    private static final Set<String> CLASSIFICATIONS = Set.of(
            "SYLLABUS_TOPIC", "SYLLABUS_SUBTOPIC", "COURSE_OUTCOME", "TEACHER_INSTRUCTION", "EXAM_PATTERN",
            "MARKS_DISTRIBUTION", "REFERENCE_MATERIAL", "ADMINISTRATIVE_INFORMATION", "PAGE_HEADER_OR_FOOTER",
            "PAGE_NUMBER", "WATERMARK", "OCR_NOISE", "UNRELATED_CONTENT", "UNCERTAIN");
    private static final Set<String> COVERAGE = Set.of("EXPLICIT", "PARTIAL", "POSSIBLE");

    private AiValidators() {
    }

    // ------------------------------------------------------------------ P01

    public static AiValidator<SyllabusExtractionResult> syllabusExtraction() {
        return root -> {
            List<String> errors = new ArrayList<>();
            SyllabusExtractionResult result = convert(root, SyllabusExtractionResult.class, errors);
            if (result.units.isEmpty()) {
                errors.add("units must contain at least one unit");
            }
            for (SyllabusExtractionResult.Unit unit : result.units) {
                if (blank(unit.unitName)) {
                    errors.add("every unit requires a unitName");
                }
                for (SyllabusExtractionResult.TopicDraft topic : unit.topics) {
                    if (blank(topic.title)) {
                        errors.add("every topic requires a title");
                    }
                    if (topic.importance != null && !IMPORTANCE.contains(topic.importance)) {
                        errors.add("topic importance must be HIGH, MEDIUM or LOW (found " + topic.importance + ")");
                    }
                    if (topic.difficulty != null && !DIFFICULTY.contains(topic.difficulty)) {
                        errors.add("topic difficulty must be HARD, MEDIUM or EASY (found " + topic.difficulty + ")");
                    }
                    if (topic.confidence != null && !CONFIDENCE.contains(topic.confidence)) {
                        errors.add("topic confidence must be HIGH, MEDIUM or LOW");
                    }
                    if (topic.estimatedMinutes != null && (topic.estimatedMinutes < 5 || topic.estimatedMinutes > 1440)) {
                        errors.add("estimatedMinutes must be between 5 and 1440");
                    }
                }
            }
            for (SyllabusExtractionResult.Classification c : result.classifications) {
                if (blank(c.originalText)) {
                    errors.add("each classification requires originalText");
                }
                if (c.classification != null && !CLASSIFICATIONS.contains(c.classification)) {
                    errors.add("unsupported classification value: " + c.classification);
                }
                if (c.confidence != null && (c.confidence < 0 || c.confidence > 1)) {
                    errors.add("classification confidence must be between 0 and 1");
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    // ------------------------------------------------------------------ P02

    public static AiValidator<TeacherInstructionsResult> teacherInstructions() {
        return root -> {
            List<String> errors = new ArrayList<>();
            TeacherInstructionsResult result = convert(root, TeacherInstructionsResult.class, errors);
            for (TeacherInstructionsResult.Hint hint : result.hints) {
                if (blank(hint.text)) {
                    errors.add("every hint requires text");
                }
                if (hint.confidence != null && !CONFIDENCE.contains(hint.confidence)) {
                    errors.add("hint confidence must be HIGH, MEDIUM or LOW");
                }
            }
            for (TeacherInstructionsResult.ImportantTopic t : result.importantTopics) {
                if (blank(t.topic)) {
                    errors.add("every important topic requires a topic name");
                }
                if (t.priority != null && !IMPORTANCE.contains(t.priority)) {
                    errors.add("important topic priority must be HIGH, MEDIUM or LOW");
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    // ------------------------------------------------------------------ P03

    public static AiValidator<MaterialIndexResult> materialIndex() {
        return root -> {
            List<String> errors = new ArrayList<>();
            MaterialIndexResult result = convert(root, MaterialIndexResult.class, errors);
            for (MaterialIndexResult.Section s : result.sections) {
                if (blank(s.title)) {
                    errors.add("every section requires a title");
                }
                if (s.startPage != null && s.startPage < 1) {
                    errors.add("section startPage must be a positive page number");
                }
                if (s.endPage != null && s.startPage != null && s.endPage < s.startPage) {
                    errors.add("section endPage must not be before startPage");
                }
                for (MaterialIndexResult.TopicCoverage t : s.topics) {
                    if (blank(t.topic)) {
                        errors.add("every indexed topic requires a topic name");
                    }
                    if (t.coverage != null && !COVERAGE.contains(t.coverage)) {
                        errors.add("coverage must be EXPLICIT, PARTIAL or POSSIBLE");
                    }
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    // ------------------------------------------------------------------ P04

    public static AiValidator<SourceComparisonResult> sourceComparison() {
        return root -> {
            List<String> errors = new ArrayList<>();
            SourceComparisonResult result = convert(root, SourceComparisonResult.class, errors);
            for (SourceComparisonResult.Recommendation r : result.recommendations) {
                if (blank(r.topic)) {
                    errors.add("every recommendation requires a topic");
                }
                if (blank(r.primarySourceId)) {
                    errors.add("every recommendation requires a primarySourceId referencing a supplied source");
                }
                if (r.confidence != null && !CONFIDENCE.contains(r.confidence)) {
                    errors.add("recommendation confidence must be HIGH, MEDIUM or LOW");
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    // ------------------------------------------------------------------ P05

    public static AiValidator<PyqAnalysisResult> pyqAnalysis() {
        return root -> {
            List<String> errors = new ArrayList<>();
            PyqAnalysisResult result = convert(root, PyqAnalysisResult.class, errors);
            Set<String> questionKeys = new HashSet<>();
            for (PyqAnalysisResult.Question q : result.questions) {
                if (blank(q.questionKey)) {
                    errors.add("every extracted question requires a questionKey");
                } else if (!questionKeys.add(q.questionKey)) {
                    errors.add("duplicate questionKey: " + q.questionKey);
                }
                if (blank(q.text)) {
                    errors.add("every extracted question requires text");
                }
                if (q.marks != null && (q.marks < 0 || q.marks > 200)) {
                    errors.add("question marks must be between 0 and 200");
                }
            }
            for (PyqAnalysisResult.TopicFrequency f : result.topicFrequency) {
                if (blank(f.topic)) {
                    errors.add("every topicFrequency entry requires a topic");
                }
                if (f.observedCount == null) {
                    errors.add("topicFrequency for '" + f.topic + "' requires observedCount");
                }
                if (f.evidenceQuestionIds != null && f.observedCount != null
                        && !f.evidenceQuestionIds.isEmpty() && f.evidenceQuestionIds.size() != f.observedCount) {
                    errors.add("observedCount (" + f.observedCount + ") for '" + f.topic
                            + "' must equal the number of evidenceQuestionIds ("
                            + f.evidenceQuestionIds.size() + ")");
                }
                if (f.evidenceQuestionIds != null) {
                    for (String id : f.evidenceQuestionIds) {
                        if (!questionKeys.contains(id)) {
                            errors.add("evidenceQuestionId '" + id + "' does not match any extracted question");
                        }
                    }
                }
                if (f.confidence != null && !CONFIDENCE.contains(f.confidence)) {
                    errors.add("topicFrequency confidence must be HIGH, MEDIUM or LOW");
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    // ------------------------------------------------------------------ P06 / P08 / P17

    private static void validateTask(JsonNode task, List<String> errors, LocalDate minDate, LocalDate maxDate) {
        String taskKey = task.path("taskKey").asText("");
        if (taskKey.isBlank()) {
            errors.add("every task requires a taskKey");
        }
        String type = task.path("taskType").asText("");
        if (!TASK_TYPES.contains(type)) {
            errors.add("task '" + taskKey + "' has unsupported taskType '" + type + "'");
        }
        if (task.path("title").asText("").isBlank()) {
            errors.add("task '" + taskKey + "' requires a title");
        }
        JsonNode minutes = task.get("estimatedMinutes");
        if (minutes == null || !minutes.isInt() || minutes.asInt() < 5 || minutes.asInt() > 720) {
            errors.add("task '" + taskKey + "' estimatedMinutes must be an integer between 5 and 720");
        }
        String date = task.path("date").asText("");
        if (date.isBlank()) {
            errors.add("task '" + taskKey + "' requires a date");
        } else {
            try {
                LocalDate parsed = LocalDate.parse(date);
                if (minDate != null && parsed.isBefore(minDate)) {
                    errors.add("task '" + taskKey + "' is scheduled before today (" + date + ")");
                }
                if (maxDate != null && parsed.isAfter(maxDate)) {
                    errors.add("task '" + taskKey + "' is scheduled after the exam date (" + date + ")");
                }
            } catch (DateTimeParseException e) {
                errors.add("task '" + taskKey + "' has an invalid date format (expected YYYY-MM-DD): " + date);
            }
        }
        if (task.hasNonNull("priority")) {
            String priority = task.get("priority").asText();
            if (!IMPORTANCE.contains(priority)) {
                errors.add("task '" + taskKey + "' priority must be HIGH, MEDIUM or LOW");
            }
        }
        JsonNode prereqs = task.get("prerequisites");
        if (prereqs != null && !prereqs.isNull() && !prereqs.isArray()) {
            errors.add("task '" + taskKey + "' prerequisites must be an array");
        }
    }

    public static AiValidator<StudyPlanResult> studyPlan() {
        return root -> {
            List<String> errors = new ArrayList<>();
            StudyPlanResult result = convert(root, StudyPlanResult.class, errors);
            if (blank(result.summary)) {
                errors.add("summary is required");
            }
            if (result.feasibility != null && !FEASIBILITY.contains(result.feasibility)) {
                errors.add("feasibility must be FEASIBLE, TIGHT or INFEASIBLE");
            }
            if (result.tasks.isEmpty()) {
                errors.add("tasks must contain at least one scheduled task");
            }
            LocalDate today = LocalDate.now();
            Set<String> keys = new HashSet<>();
            for (JsonNode task : root.path("tasks")) {
                validateTask(task, errors, today, null);
                String key = task.path("taskKey").asText("");
                if (!key.isBlank() && !keys.add(key)) {
                    errors.add("duplicate taskKey: " + key);
                }
            }
            Set<String> allKeys = new HashSet<>(keys);
            for (JsonNode task : root.path("tasks")) {
                for (JsonNode pre : task.path("prerequisites")) {
                    if (!allKeys.contains(pre.asText())) {
                        errors.add("task '" + task.path("taskKey").asText()
                                + "' references unknown prerequisite '" + pre.asText() + "'");
                    }
                }
            }
            for (StudyPlanResult.TopicPriority p : result.topics) {
                if (blank(p.topicId)) {
                    errors.add("every topic priority requires a topicId");
                }
                if (p.priority != null && !IMPORTANCE.contains(p.priority)) {
                    errors.add("topic priority value must be HIGH, MEDIUM or LOW");
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<ReplanResult> replan() {
        return root -> {
            List<String> errors = new ArrayList<>();
            ReplanResult result = convert(root, ReplanResult.class, errors);
            if (blank(result.summary)) {
                errors.add("summary is required");
            }
            if (result.feasibility != null && !FEASIBILITY.contains(result.feasibility)) {
                errors.add("feasibility must be FEASIBLE, TIGHT or INFEASIBLE");
            }
            if (result.capacityRisk != null && !Set.of("LOW", "MEDIUM", "HIGH").contains(result.capacityRisk)) {
                errors.add("capacityRisk must be LOW, MEDIUM or HIGH");
            }
            LocalDate today = LocalDate.now();
            Set<String> keys = new HashSet<>();
            for (JsonNode task : root.path("tasks")) {
                // Preserved completed tasks may legitimately carry past dates; the service validates
                // dates for non-preserved tasks against today and the exam date.
                validateTask(task, errors, null, null);
                String key = task.path("taskKey").asText("");
                if (!key.isBlank() && !keys.add(key)) {
                    errors.add("duplicate taskKey in replan: " + key);
                }
                if (task.hasNonNull("changeType")) {
                    String change = task.get("changeType").asText();
                    if (!Set.of("ADDED", "MOVED", "UPDATED", "UNCHANGED", "REMOVED").contains(change)) {
                        errors.add("changeType must be ADDED, MOVED, UPDATED, UNCHANGED or REMOVED");
                    }
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<FeasibilityResult> feasibility() {
        return root -> {
            List<String> errors = new ArrayList<>();
            FeasibilityResult result = convert(root, FeasibilityResult.class, errors);
            if (result.availableMinutes == null || result.availableMinutes < 0) {
                errors.add("availableMinutes must be a non-negative integer");
            }
            if (result.estimatedRequiredMinutes == null || result.estimatedRequiredMinutes < 0) {
                errors.add("estimatedRequiredMinutes must be a non-negative integer");
            }
            if (result.feasibility == null || !FEASIBILITY.contains(result.feasibility)) {
                errors.add("feasibility must be FEASIBLE, TIGHT or INFEASIBLE");
            }
            if (result.availableMinutes != null && result.estimatedRequiredMinutes != null && result.bufferMinutes != null) {
                int expected = result.availableMinutes - result.estimatedRequiredMinutes;
                if (Math.abs(expected - result.bufferMinutes) > 5) {
                    errors.add("bufferMinutes (" + result.bufferMinutes + ") does not match availableMinutes - estimatedRequiredMinutes ("
                            + expected + ")");
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<LastMinuteRevisionResult> lastMinute() {
        return root -> {
            List<String> errors = new ArrayList<>();
            LastMinuteRevisionResult result = convert(root, LastMinuteRevisionResult.class, errors);
            if (result.feasibility != null && !FEASIBILITY.contains(result.feasibility)) {
                errors.add("feasibility must be FEASIBLE, TIGHT or INFEASIBLE");
            }
            for (LastMinuteRevisionResult.PriorityBlock b : result.priorityBlocks) {
                if (blank(b.topic)) {
                    errors.add("every priority block requires a topic");
                }
                if (b.recommendedMinutes == null || b.recommendedMinutes < 5 || b.recommendedMinutes > 480) {
                    errors.add("priority block recommendedMinutes must be between 5 and 480");
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<PlanChangeExplanationResult> planChangeExplanation() {
        return root -> {
            List<String> errors = new ArrayList<>();
            PlanChangeExplanationResult result = convert(root, PlanChangeExplanationResult.class, errors);
            if (blank(result.summary)) {
                errors.add("summary is required");
            }
            for (PlanChangeExplanationResult.Change c : result.changes) {
                if (blank(c.change)) {
                    errors.add("every change requires a description");
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    // ------------------------------------------------------------------ P09 / P10 / P11

    private static void validateQuestion(JsonNode q, List<String> errors, boolean requireMarks) {
        String key = q.path("clientKey").asText("");
        String type = q.path("type").asText("");
        if (!QUESTION_TYPES.contains(type)) {
            errors.add("question '" + key + "' has unsupported type '" + type + "'");
        }
        if (q.path("prompt").asText("").isBlank()) {
            errors.add("question '" + key + "' requires a prompt");
        }
        if (requireMarks) {
            JsonNode marks = q.get("marks");
            if (marks == null || !marks.isInt() || marks.asInt() < 1 || marks.asInt() > 100) {
                errors.add("question '" + key + "' marks must be an integer between 1 and 100");
            }
        }
        if ("MCQ".equals(type)) {
            JsonNode options = q.get("options");
            if (options == null || !options.isArray() || options.size() < 2) {
                errors.add("MCQ '" + key + "' must provide at least two options");
            } else {
                String correct = q.path("correctOption").asText("");
                if (correct.isBlank()) {
                    errors.add("MCQ '" + key + "' must provide correctOption");
                } else {
                    int index = correct.toUpperCase().charAt(0) - 'A';
                    if (index < 0 || index >= options.size()) {
                        errors.add("MCQ '" + key + "' correctOption '" + correct + "' is outside the option range");
                    }
                }
            }
        }
        JsonNode rubric = q.get("rubric");
        if (rubric != null && rubric.isArray() && requireMarks) {
            int sum = 0;
            for (JsonNode r : rubric) {
                sum += r.path("marks").asInt(0);
            }
            int marks = q.path("marks").asInt(0);
            if (sum != marks) {
                errors.add("question '" + key + "' rubric marks (" + sum + ") must equal question marks (" + marks + ")");
            }
        }
    }

    public static AiValidator<MockPaperResult> mockPaper() {
        return root -> {
            List<String> errors = new ArrayList<>();
            MockPaperResult result = convert(root, MockPaperResult.class, errors);
            if (blank(result.title)) {
                errors.add("title is required");
            }
            if (result.questions.isEmpty()) {
                errors.add("questions must contain at least one question");
            }
            int total = 0;
            for (JsonNode q : root.path("questions")) {
                validateQuestion(q, errors, true);
                total += q.path("marks").asInt(0);
            }
            if (result.declaredTotalMarks == null) {
                errors.add("declaredTotalMarks is required");
            } else if (result.declaredTotalMarks != total) {
                errors.add("declaredTotalMarks (" + result.declaredTotalMarks + ") must equal the sum of question marks (" + total + ")");
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<PracticeQuestionResult> practiceQuestions() {
        return root -> {
            List<String> errors = new ArrayList<>();
            PracticeQuestionResult result = convert(root, PracticeQuestionResult.class, errors);
            if (result.questions.isEmpty()) {
                errors.add("questions must contain at least one question");
            }
            for (JsonNode q : root.path("questions")) {
                validateQuestion(q, errors, true);
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<AnswerKeyResult> answerKey() {
        return root -> {
            List<String> errors = new ArrayList<>();
            AnswerKeyResult result = convert(root, AnswerKeyResult.class, errors);
            if (result.solutions.isEmpty()) {
                errors.add("solutions must contain at least one solution");
            }
            for (AnswerKeyResult.Solution s : result.solutions) {
                if (blank(s.questionId)) {
                    errors.add("every solution requires a questionId");
                }
                if (blank(s.modelAnswer)) {
                    errors.add("solution for '" + s.questionId + "' requires a modelAnswer");
                }
                int sum = 0;
                for (PracticeDtos.Rubric r : s.rubric) {
                    sum += r.marks == null ? 0 : r.marks;
                }
                if (s.totalRubricMarks == null) {
                    errors.add("solution for '" + s.questionId + "' requires totalRubricMarks");
                } else if (Math.abs(s.totalRubricMarks - sum) > 0) {
                    errors.add("solution for '" + s.questionId + "' totalRubricMarks (" + s.totalRubricMarks
                            + ") must equal the sum of rubric marks (" + sum + ")");
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<EvaluationResult> evaluation() {
        return root -> {
            List<String> errors = new ArrayList<>();
            EvaluationResult result = convert(root, EvaluationResult.class, errors);
            if (result.awardedMarks == null) {
                errors.add("awardedMarks is required");
            }
            if (result.maximumMarks == null || result.maximumMarks <= 0) {
                errors.add("maximumMarks must be a positive number");
            }
            if (result.awardedMarks != null && result.maximumMarks != null) {
                if (result.awardedMarks < 0) {
                    errors.add("awardedMarks must not be negative");
                }
                if (result.awardedMarks > result.maximumMarks) {
                    errors.add("awardedMarks must not exceed maximumMarks");
                }
            }
            if (result.confidence != null && !CONFIDENCE.contains(result.confidence)) {
                errors.add("confidence must be HIGH, MEDIUM or LOW");
            }
            throwIfErrors(errors);
            return result;
        };
    }

    // ------------------------------------------------------------------ P13 / P14 / P15 / P16

    public static AiValidator<ExplanationResult> explanation() {
        return root -> {
            List<String> errors = new ArrayList<>();
            ExplanationResult result = convert(root, ExplanationResult.class, errors);
            if (blank(result.directAnswer) && blank(result.explanation)) {
                errors.add("either directAnswer or explanation must be present");
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<RevisionResourcesResult> revisionResources() {
        return root -> {
            List<String> errors = new ArrayList<>();
            RevisionResourcesResult result = convert(root, RevisionResourcesResult.class, errors);
            if (result.topicResources.isEmpty()) {
                errors.add("topicResources must contain at least one topic");
            }
            for (RevisionResourcesResult.TopicResource r : result.topicResources) {
                if (blank(r.topic)) {
                    errors.add("every topic resource requires a topic");
                }
                for (RevisionResourcesResult.Flashcard f : r.flashcards) {
                    if (blank(f.front) || blank(f.back)) {
                        errors.add("flashcards for '" + r.topic + "' require both front and back");
                    }
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<ReadinessResult> readiness() {
        return root -> {
            List<String> errors = new ArrayList<>();
            ReadinessResult result = convert(root, ReadinessResult.class, errors);
            if (blank(result.readinessSummary)) {
                errors.add("readinessSummary is required");
            }
            for (ReadinessResult.WeakTopic t : result.weakTopics) {
                if (blank(t.topic)) {
                    errors.add("every weak topic requires a topic name");
                }
                if (t.confidence != null && !CONFIDENCE.contains(t.confidence)) {
                    errors.add("weak topic confidence must be HIGH, MEDIUM or LOW");
                }
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<SessionFeedbackResult> sessionFeedback() {
        return root -> {
            List<String> errors = new ArrayList<>();
            SessionFeedbackResult result = convert(root, SessionFeedbackResult.class, errors);
            if (blank(result.summary)) {
                errors.add("summary is required");
            }
            throwIfErrors(errors);
            return result;
        };
    }

    public static AiValidator<AssistantIntakeResult> assistantIntake() {
        return root -> {
            List<String> errors = new ArrayList<>();
            AssistantIntakeResult result = convert(root, AssistantIntakeResult.class, errors);
            if (blank(result.intent)) {
                errors.add("intent is required");
            }
            if (blank(result.actionRequested)) {
                errors.add("actionRequested is required");
            } else if (!Set.of("CREATE_PLAN", "REVISE_PLAN", "EXPLAIN_CONCEPT", "GENERATE_PRACTICE",
                    "GENERATE_REVISION", "ASSESS_READINESS", "LAST_MINUTE_REVISION", "ANALYSE_PYQ", "UNKNOWN")
                    .contains(result.actionRequested)) {
                errors.add("actionRequested has an unsupported value: " + result.actionRequested);
            }
            throwIfErrors(errors);
            return result;
        };
    }

    // ------------------------------------------------------------------ helpers

    private static <T> T convert(JsonNode root, Class<T> type, List<String> errors) {
        try {
            return Json.convert(root, type);
        } catch (RuntimeException e) {
            errors.add("response did not match the required structure: " + e.getMessage());
            throwIfErrors(errors);
            throw e;
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static void throwIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new AiValidationException(errors);
        }
    }
}
