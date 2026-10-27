package com.examlens.service;

import com.examlens.ai.AiAction;
import com.examlens.ai.AiException;
import com.examlens.ai.AiFailureType;
import com.examlens.ai.AiOutcome;
import com.examlens.ai.AiServiceRunner;
import com.examlens.ai.dto.PracticeDtos;
import com.examlens.ai.validate.AiValidators;
import com.examlens.dao.PracticeDao;
import com.examlens.model.AttemptAnswer;
import com.examlens.model.Exam;
import com.examlens.model.PaperAttempt;
import com.examlens.model.PaperQuestion;
import com.examlens.model.PracticePaper;
import com.examlens.model.Topic;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * P09 (mock paper), P10 (practice questions), P11 (answer keys), P12 (subjective evaluation).
 *
 * <p>Multiple-choice questions are graded deterministically by the server — never by the AI.
 * Subjective answers are graded by P12, then clamped to the question's marks in Java. A paper id
 * is validated at every boundary so a null/zero/foreign id can never reach JDBC auto-unboxing.</p>
 */
public class PracticeService {

    private final PracticeDao practice;
    private final SyllabusService syllabus;
    private final ExamService exams;
    private final AiServiceRunner ai;

    public PracticeService(PracticeDao practice, SyllabusService syllabus, ExamService exams, AiServiceRunner ai) {
        this.practice = practice;
        this.syllabus = syllabus;
        this.exams = exams;
        this.ai = ai;
    }

    public record PaperConfig(Integer totalMarks, Integer questionCount, Integer durationMinutes,
                              String difficulty, List<String> types, List<Long> topicIds) {
    }

    public record AttemptView(PaperAttempt attempt, PracticePaper paper, List<PaperQuestion> questions,
                              List<AttemptAnswer> answers) {
        public double percentage() {
            if (attempt == null || attempt.getTotalPossible() == null || attempt.getTotalPossible() == 0
                    || attempt.getTotalAwarded() == null) {
                return 0;
            }
            return Math.round(attempt.getTotalAwarded() * 1000.0 / attempt.getTotalPossible()) / 10.0;
        }

        public PaperAttempt getAttempt() { return attempt; }
        public PracticePaper getPaper() { return paper; }
        public List<PaperQuestion> getQuestions() { return questions; }
        public List<AttemptAnswer> getAnswers() { return answers; }
        public double getPercentage() { return percentage(); }
    }

    private static final List<String> ALL_TYPES = List.of("MCQ", "SHORT_ANSWER", "THEORY", "NUMERICAL", "APPLICATION");

    // ------------------------------------------------------------------ P09

    public long generateMockPaper(long userId, long examId, PaperConfig config) {
        Exam exam = exams.require(userId, examId);
        List<Topic> topics = syllabus.confirmedTopics(userId, examId);
        if (topics.isEmpty()) {
            throw new ServiceException("Confirm your syllabus topics before generating a practice paper.");
        }
        int totalMarks = config.totalMarks() == null ? 50 : config.totalMarks();
        if (totalMarks < 1 || totalMarks > 300) {
            throw new ServiceException("Total marks must be between 1 and 300.");
        }
        int questionCount = config.questionCount() == null ? Math.min(10, totalMarks) : config.questionCount();
        if (questionCount < 1 || questionCount > 60) {
            throw new ServiceException("Question count must be between 1 and 60.");
        }
        if (questionCount > totalMarks) {
            throw new ServiceException("You need at least one mark per question, so the question count cannot exceed the total marks.");
        }
        if (config.durationMinutes() != null && (config.durationMinutes() < 5 || config.durationMinutes() > 600)) {
            throw new ServiceException("Duration must be between 5 and 600 minutes.");
        }
        List<String> types = normaliseTypes(config.types());
        String difficulty = normaliseDifficulty(config.difficulty());

        ObjectNode input = AiInputs.object();
        input.put("subjectName", exam.getSubjectName());
        input.set("topics", AiInputs.topics(topics));
        input.put("totalMarks", totalMarks);
        input.put("questionCount", questionCount);
        input.set("questionTypes", AiInputs.strings(types));
        input.put("difficulty", difficulty);
        if (config.durationMinutes() != null) {
            input.put("durationMinutes", config.durationMinutes());
        }

        AiOutcome<PracticeDtos.MockPaperResult> outcome = ai.run(
                AiAction.P09_MOCK_PAPER_GENERATION, userId, examId, input,
                AiValidators.mockPaper(),
                data -> paperDomainCheck(data.declaredTotalMarks, totalMarks, data.questions, topicIdSet(topics)));

        PracticeDtos.MockPaperResult result = outcome.getData();
        return persistPaper(userId, examId, outcome.getJobId(), result.title, result.instructions,
                result.declaredTotalMarks, config.durationMinutes(), difficulty, result.questions);
    }

    // ------------------------------------------------------------------ P10

    public long generatePracticeSet(long userId, long examId, int count, String difficulty,
                                    List<String> types, List<Long> topicIds) {
        Exam exam = exams.require(userId, examId);
        List<Topic> allTopics = syllabus.confirmedTopics(userId, examId);
        if (allTopics.isEmpty()) {
            throw new ServiceException("Confirm your syllabus topics before generating practice questions.");
        }
        List<Topic> selected = selectTopics(allTopics, topicIds);
        if (count < 1 || count > 40) {
            throw new ServiceException("Choose between 1 and 40 questions.");
        }

        ObjectNode input = AiInputs.object();
        input.put("subjectName", exam.getSubjectName());
        input.set("topics", AiInputs.topics(selected));
        input.put("count", count);
        input.set("questionTypes", AiInputs.strings(normaliseTypes(types)));
        input.put("difficulty", normaliseDifficulty(difficulty));

        AiOutcome<PracticeDtos.PracticeQuestionResult> outcome = ai.run(
                AiAction.P10_PRACTICE_QUESTION_GENERATION, userId, examId, input,
                AiValidators.practiceQuestions(),
                data -> paperDomainCheck(null, null, data.questions, topicIdSet(selected)));

        PracticeDtos.PracticeQuestionResult result = outcome.getData();
        int total = result.questions.stream().mapToInt(q -> q.marks == null ? 0 : q.marks).sum();
        return persistPaper(userId, examId, outcome.getJobId(),
                "Practice set — " + exam.getSubjectName(), "Focused practice on selected topics.",
                total, null, normaliseDifficulty(difficulty), result.questions);
    }

    // ------------------------------------------------------------------ P11

    public void prepareSolutions(long userId, long paperId) {
        PracticePaper paper = requirePaper(userId, paperId);
        List<PaperQuestion> questions = paper.getQuestions();
        if (questions.isEmpty()) {
            throw new ServiceException("This paper has no questions.");
        }
        ArrayNode questionNodes = Json.array();
        Set<String> ids = new LinkedHashSet<>();
        for (PaperQuestion q : questions) {
            String id = String.valueOf(q.getId());
            ids.add(id);
            ObjectNode node = AiInputs.object();
            node.put("questionId", id);
            node.put("clientKey", q.getClientKey());
            node.put("marks", q.getMarks());
            node.put("prompt", q.getPrompt());
            node.put("modelAnswer", q.getModelAnswer() == null ? "" : q.getModelAnswer());
            questionNodes.add(node);
        }
        ObjectNode input = AiInputs.object();
        input.set("questions", questionNodes);
        input.put("subjectName", paper.getTitle());

        AiOutcome<PracticeDtos.AnswerKeyResult> outcome = ai.run(
                AiAction.P11_ANSWER_KEYS_AND_SOLUTIONS, userId, paper.getExamId(), input,
                AiValidators.answerKey(),
                data -> answerKeyDomainCheck(data, ids, questions));

        try {
            Map<String, PaperQuestion> byId = new java.util.HashMap<>();
            for (PaperQuestion q : questions) {
                byId.put(String.valueOf(q.getId()), q);
            }
            for (PracticeDtos.AnswerKeyResult.Solution solution : outcome.getData().solutions) {
                PaperQuestion question = byId.get(solution.questionId);
                if (question == null) {
                    continue;
                }
                List<PaperQuestion.RubricItem> rubric = new ArrayList<>();
                for (PracticeDtos.Rubric r : solution.rubric) {
                    rubric.add(new PaperQuestion.RubricItem(r.criterion, r.marks == null ? 0 : r.marks));
                }
                practice.updateQuestionSolutions(question.getId(), solution.modelAnswer,
                        rubric.isEmpty() ? null : Json.write(rubric));
            }
        } catch (SQLException e) {
            throw new ServiceException("The solutions could not be saved.", e);
        }
    }

    // ------------------------------------------------------------------ attempts

    public long startAttempt(long userId, Long paperId, boolean timed, Integer durationMinutes) {
        // Boundary validation: a null/zero/negative paperId can never reach JDBC auto-unboxing.
        PracticePaper paper = requirePaper(userId, paperId);
        if (paper.getQuestions().isEmpty()) {
            throw new ServiceException("This paper has no questions to attempt.");
        }
        if (timed && (durationMinutes == null || durationMinutes < 1 || durationMinutes > 600)) {
            durationMinutes = paper.getDurationMinutes();
        }
        int totalPossible = paper.getQuestions().stream().mapToInt(PaperQuestion::getMarks).sum();
        try {
            return practice.insertAttempt(paper.getId(), userId, timed, durationMinutes, totalPossible);
        } catch (SQLException e) {
            throw new ServiceException("Could not start the attempt.", e);
        }
    }

    public void submitAttempt(long userId, long attemptId, Map<Long, String> answers) {
        PaperAttempt attempt = requireAttempt(userId, attemptId);
        if ("SUBMITTED".equals(attempt.getStatus()) || "GRADED".equals(attempt.getStatus())) {
            throw new ServiceException("This attempt has already been submitted.");
        }
        PracticePaper paper = attempt.getPaper();
        if (paper == null) {
            paper = requirePaper(userId, attempt.getPaperId());
        }
        boolean hasSubjective = paper.getQuestions().stream().anyMatch(q -> !q.isMultipleChoice());
        if (hasSubjective && !ai.isAiAvailable()) {
            throw new AiException(AiFailureType.MISSING_API_KEY, ai.unavailabilityMessage());
        }

        double totalAwarded = 0;
        int totalPossible = 0;
        Long evaluationJobId = null;
        try {
            for (PaperQuestion q : paper.getQuestions()) {
                String answer = answers.get(q.getId());
                totalPossible += q.getMarks();
                if (q.isMultipleChoice()) {
                    boolean correct = answer != null && q.getCorrectOption() != null
                            && answer.trim().equalsIgnoreCase(q.getCorrectOption().trim());
                    double awarded = correct ? q.getMarks() : 0;
                    practice.upsertAnswer(attemptId, q.getId(), null, answer, awarded, "DETERMINISTIC_MCQ",
                            correct ? "Correct answer." : "Incorrect answer.", null, "HIGH", false, null);
                    totalAwarded += awarded;
                    continue;
                }
                if (answer == null || answer.isBlank()) {
                    practice.upsertAnswer(attemptId, q.getId(), answer, null, 0.0, "EMPTY",
                            "No answer was submitted for this question.", null, "HIGH", false, null);
                    continue;
                }
                ObjectNode input = AiInputs.object();
                ObjectNode questionNode = AiInputs.object();
                questionNode.put("questionId", String.valueOf(q.getId()));
                questionNode.put("marks", q.getMarks());
                questionNode.put("prompt", q.getPrompt());
                questionNode.put("modelAnswer", q.getModelAnswer() == null ? "" : q.getModelAnswer());
                input.set("question", questionNode);
                input.put("maximumMarks", q.getMarks());
                input.put("studentAnswer", answer);
                if (!q.getRubric().isEmpty()) {
                    input.set("rubric", Json.mapper().valueToTree(q.getRubric()));
                }
                AiOutcome<PracticeDtos.EvaluationResult> outcome = ai.run(
                        AiAction.P12_SUBJECTIVE_ANSWER_EVALUATION, userId, paper.getExamId(), input,
                        AiValidators.evaluation(),
                        data -> evaluationDomainCheck(data, q));
                PracticeDtos.EvaluationResult result = outcome.getData();
                double awarded = clamp(result.awardedMarks == null ? 0 : result.awardedMarks, 0, q.getMarks());
                totalAwarded += awarded;
                evaluationJobId = outcome.getJobId();
                practice.upsertAnswer(attemptId, q.getId(), answer, null, awarded, "AI_RUBRIC",
                        result.feedback, Json.write(result.criterionResults), result.confidence,
                        result.requiresManualReview != null && result.requiresManualReview, outcome.getJobId());
            }
            practice.updateAttemptTotals(attemptId, userId, "SUBMITTED", totalAwarded, totalPossible, evaluationJobId);
        } catch (SQLException e) {
            throw new ServiceException("The attempt could not be submitted.", e);
        }
    }

    // ------------------------------------------------------------------ reads

    public PracticePaper requirePaper(long userId, Long paperId) {
        if (paperId == null || paperId <= 0) {
            throw new ServiceException("Select a valid practice paper.");
        }
        try {
            return practice.findPaper(paperId, userId)
                    .orElseThrow(() -> new ServiceException("That practice paper was not found in your account."));
        } catch (SQLException e) {
            throw new ServiceException("Could not load the practice paper.", e);
        }
    }

    public Optional<PracticePaper> findPaper(long userId, Long paperId) {
        if (paperId == null || paperId <= 0) {
            return Optional.empty();
        }
        try {
            return practice.findPaper(paperId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the practice paper.", e);
        }
    }

    public List<PracticePaper> papers(long userId, long examId) {
        try {
            return practice.papersForExam(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load practice papers.", e);
        }
    }

    public PracticePaper requirePaper(long userId, long paperId) {
        return requirePaper(userId, Long.valueOf(paperId));
    }

    public PaperAttempt requireAttempt(long userId, long attemptId) {
        if (attemptId <= 0) {
            throw new ServiceException("Select a valid attempt.");
        }
        try {
            return practice.findAttempt(attemptId, userId)
                    .orElseThrow(() -> new ServiceException("That attempt was not found in your account."));
        } catch (SQLException e) {
            throw new ServiceException("Could not load the attempt.", e);
        }
    }

    public Optional<AttemptView> viewAttempt(long userId, Long attemptId) {
        if (attemptId == null || attemptId <= 0) {
            return Optional.empty();
        }
        PaperAttempt attempt = requireAttempt(userId, attemptId);
        PracticePaper paper = attempt.getPaper() != null
                ? attempt.getPaper() : requirePaper(userId, attempt.getPaperId());
        List<AttemptAnswer> answers;
        try {
            answers = practice.answers(attemptId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load your answers.", e);
        }
        return Optional.of(new AttemptView(attempt, paper, paper.getQuestions(), answers));
    }

    public List<PaperAttempt> attempts(long userId, long examId, int limit) {
        try {
            return practice.attemptsForExam(examId, userId, limit);
        } catch (SQLException e) {
            throw new ServiceException("Could not load practice attempts.", e);
        }
    }

    public List<PaperAttempt> attemptsForPaper(long userId, long paperId) {
        try {
            return practice.attemptsForPaper(paperId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load attempts for this paper.", e);
        }
    }

    public int countPapers(long userId, long examId) {
        try {
            return practice.countPapers(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not count practice papers.", e);
        }
    }

    public Double practiceAverage(long userId, long examId) {
        try {
            return practice.practiceAverageForExam(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not compute the practice average.", e);
        }
    }

    public Double topicScoreRatio(long userId, long examId, long topicId) {
        try {
            return practice.topicScoreRatio(examId, userId, topicId);
        } catch (SQLException e) {
            throw new ServiceException("Could not compute the topic score.", e);
        }
    }

    // ------------------------------------------------------------------ persistence

    private long persistPaper(long userId, long examId, long aiJobId, String title, String instructions,
                              Integer totalMarks, Integer durationMinutes, String difficulty,
                              List<PracticeDtos.GeneratedQuestion> questions) {
        try {
            int total = totalMarks == null ? questions.stream().mapToInt(q -> q.marks == null ? 0 : q.marks).sum() : totalMarks;
            long paperId = practice.insertPaper(examId, userId, aiJobId,
                    title == null || title.isBlank() ? "Practice paper" : title,
                    instructions, total, durationMinutes, difficulty, "READY");
            int order = 0;
            for (PracticeDtos.GeneratedQuestion q : questions) {
                PaperQuestion question = new PaperQuestion();
                question.setPaperId(paperId);
                question.setClientKey(q.clientKey == null || q.clientKey.isBlank() ? "q" + (++order) : q.clientKey);
                question.setQuestionType(q.type);
                question.setTopic(q.topic);
                question.setTopicId(StudyPlanService.parseLong(q.topicId));
                question.setPrompt(q.prompt);
                question.setOptions(q.options == null ? new ArrayList<>() : new ArrayList<>(q.options));
                question.setCorrectOption(q.correctOption);
                question.setMarks(q.marks == null ? 1 : q.marks);
                question.setDifficulty(q.difficulty);
                question.setModelAnswer(q.modelAnswer);
                List<PaperQuestion.RubricItem> rubric = new ArrayList<>();
                if (q.rubric != null) {
                    for (PracticeDtos.Rubric r : q.rubric) {
                        rubric.add(new PaperQuestion.RubricItem(r.criterion, r.marks == null ? 0 : r.marks));
                    }
                }
                question.setRubric(rubric);
                question.setDisplayOrder(++order);
                practice.insertQuestion(question);
            }
            return paperId;
        } catch (SQLException e) {
            throw new ServiceException("The practice paper could not be saved.", e);
        }
    }

    // ------------------------------------------------------------------ domain checks

    private List<String> paperDomainCheck(Integer declaredTotal, Integer requestedTotal,
                                          List<PracticeDtos.GeneratedQuestion> questions, Set<String> confirmedIds) {
        List<String> errors = new ArrayList<>();
        if (declaredTotal != null && requestedTotal != null && !declaredTotal.equals(requestedTotal)) {
            errors.add("declaredTotalMarks (" + declaredTotal + ") must equal the requested total of "
                    + requestedTotal + " marks.");
        }
        for (PracticeDtos.GeneratedQuestion q : questions) {
            if (q.topicId != null && !q.topicId.isBlank() && !confirmedIds.contains(q.topicId.trim())) {
                errors.add("question \"" + q.clientKey + "\" references topicId \"" + q.topicId
                        + "\" which is not a confirmed topic.");
            }
        }
        return errors;
    }

    private List<String> answerKeyDomainCheck(PracticeDtos.AnswerKeyResult data, Set<String> questionIds,
                                              List<PaperQuestion> questions) {
        Map<String, Integer> marksById = new java.util.HashMap<>();
        for (PaperQuestion q : questions) {
            marksById.put(String.valueOf(q.getId()), q.getMarks());
        }
        List<String> errors = new ArrayList<>();
        for (PracticeDtos.AnswerKeyResult.Solution solution : data.solutions) {
            if (solution.questionId == null || !questionIds.contains(solution.questionId.trim())) {
                errors.add("solution references questionId \"" + solution.questionId + "\" which is not one of the supplied questions.");
                continue;
            }
            Integer marks = marksById.get(solution.questionId.trim());
            if (marks != null && solution.totalRubricMarks != null && !solution.totalRubricMarks.equals(marks)) {
                errors.add("solution for question " + solution.questionId + " has totalRubricMarks "
                        + solution.totalRubricMarks + " but the question is worth " + marks + " marks.");
            }
        }
        return errors;
    }

    private List<String> evaluationDomainCheck(PracticeDtos.EvaluationResult data, PaperQuestion question) {
        List<String> errors = new ArrayList<>();
        double maximum = data.maximumMarks == null ? question.getMarks() : data.maximumMarks;
        if (Math.abs(maximum - question.getMarks()) > 0.001) {
            errors.add("maximumMarks (" + data.maximumMarks + ") must equal the question marks (" + question.getMarks() + ").");
        }
        if (data.awardedMarks != null && data.awardedMarks > question.getMarks() + 0.001) {
            errors.add("awardedMarks (" + data.awardedMarks + ") must not exceed the question marks (" + question.getMarks() + ").");
        }
        if (data.criterionResults != null && !data.criterionResults.isEmpty()) {
            double sum = 0;
            for (PracticeDtos.EvaluationResult.CriterionResult c : data.criterionResults) {
                sum += c.maximumMarks == null ? 0 : c.maximumMarks;
            }
            if (Math.abs(sum - maximum) > 0.001) {
                errors.add("the criterion maximumMarks sum to " + sum + " but the question maximum is " + maximum + ".");
            }
        }
        return errors;
    }

    // ------------------------------------------------------------------ helpers

    private Set<String> topicIdSet(List<Topic> topics) {
        Set<String> ids = new LinkedHashSet<>();
        for (Topic t : topics) {
            ids.add(String.valueOf(t.getId()));
        }
        return ids;
    }

    private List<Topic> selectTopics(List<Topic> all, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return all;
        }
        List<Topic> selected = new ArrayList<>();
        for (Topic t : all) {
            if (ids.contains(t.getId())) {
                selected.add(t);
            }
        }
        if (selected.isEmpty()) {
            throw new ServiceException("None of the selected topics belong to this exam's confirmed syllabus.");
        }
        return selected;
    }

    private List<String> normaliseTypes(List<String> types) {
        List<String> out = new ArrayList<>();
        if (types != null) {
            for (String t : types) {
                if (t == null) {
                    continue;
                }
                String upper = t.trim().toUpperCase(java.util.Locale.ROOT);
                if (ALL_TYPES.contains(upper) && !out.contains(upper)) {
                    out.add(upper);
                }
            }
        }
        if (out.isEmpty()) {
            out.addAll(List.of("MCQ", "SHORT_ANSWER", "THEORY"));
        }
        return out;
    }

    private String normaliseDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return "MEDIUM";
        }
        String upper = difficulty.trim().toUpperCase(java.util.Locale.ROOT);
        return List.of("EASY", "MEDIUM", "HARD").contains(upper) ? upper : "MEDIUM";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
