package com.examlens.ai;

import com.examlens.ai.prompt.PromptLibrary;
import com.examlens.ai.validate.AiValidators;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the deterministic simulated provider and the real structural validators for all 18
 * core AI actions plus the assistant intake, proving the mock output satisfies the same contract
 * that live Gemini output must satisfy. Because the mock is a pure function, these are stable.
 */
class MockAiServiceActionsTest {

    private final MockAiService ai = new MockAiService();

    private JsonNode run(AiAction action, ObjectNode input) throws AiException {
        AiRequest request = PromptLibrary.build(action, input, 4096);
        AiResponse response = ai.generate(request);
        assertNotNull(response.getRawText());
        return Json.extractJson(response.getRawText());
    }

    private static ObjectNode obj() {
        return Json.object();
    }

    private static ArrayNode one(String... pairs) {
        ArrayNode array = Json.array();
        ObjectNode node = Json.object();
        for (int i = 0; i < pairs.length; i += 2) {
            node.put(pairs[i], pairs[i + 1]);
        }
        array.add(node);
        return array;
    }

    @Test
    void p01SyllabusExtractionIsValid() throws Exception {
        ObjectNode input = obj();
        ArrayNode blocks = Json.array();
        ObjectNode block = Json.object();
        block.put("blockKey", "b1");
        block.put("page", 1);
        block.put("text", "Unit 1\nAlgebra basics\nGeometry introduction");
        blocks.add(block);
        input.set("blocks", blocks);

        JsonNode out = run(AiAction.P01_SYLLABUS_EXTRACTION, input);
        var result = AiValidators.syllabusExtraction().validate(out);
        assertFalse(result.units.isEmpty());
        assertTrue(out.path("classifications").isArray());
    }

    @Test
    void p02TeacherInstructionsIsValid() throws Exception {
        ObjectNode input = obj();
        ArrayNode blocks = Json.array();
        ObjectNode block = Json.object();
        block.put("text", "Focus on data structures and algorithms. The exam has 100 marks in total.");
        blocks.add(block);
        input.set("blocks", blocks);

        JsonNode out = run(AiAction.P02_TEACHER_INSTRUCTIONS, input);
        AiValidators.teacherInstructions().validate(out);
        assertFalse(out.path("hints").isEmpty());
    }

    @Test
    void p03MaterialIndexingIsValid() throws Exception {
        ObjectNode input = obj();
        input.put("documentName", "Course notes");
        input.set("blocks", one("page", "1", "text", "Recursion is introduced in this chapter."));
        input.set("syllabusTopics", one("title", "Recursion"));

        JsonNode out = run(AiAction.P03_STUDY_MATERIAL_INDEXING, input);
        AiValidators.materialIndex().validate(out);
        assertEquals("EXPLICIT", out.path("sections").get(0).path("topics").get(0).path("coverage").asText());
    }

    @Test
    void p04SourceComparisonIsValid() throws Exception {
        ObjectNode input = obj();
        input.set("syllabusTopics", one("title", "Recursion"));
        input.set("sources", one("sourceId", "1", "name", "Textbook"));

        JsonNode out = run(AiAction.P04_SOURCE_COMPARISON, input);
        AiValidators.sourceComparison().validate(out);
        assertEquals("1", out.path("recommendations").get(0).path("primarySourceId").asText());
    }

    @Test
    void p05PyqAnalysisIsValid() throws Exception {
        ObjectNode input = obj();
        ObjectNode paper = Json.object();
        paper.put("paperId", "p1");
        paper.put("year", "2024");
        ArrayNode questions = Json.array();
        ObjectNode q = Json.object();
        q.put("questionKey", "q1");
        q.put("text", "Explain recursion with an example.");
        q.put("marks", 5);
        q.put("page", 1);
        questions.add(q);
        paper.set("questions", questions);
        ArrayNode papers = Json.array();
        papers.add(paper);
        input.set("papers", papers);
        input.set("syllabusTopics", one("title", "Recursion"));

        JsonNode out = run(AiAction.P05_PYQ_ANALYSIS, input);
        AiValidators.pyqAnalysis().validate(out);
        assertEquals(1, out.path("topicFrequency").get(0).path("observedCount").asInt());
    }

    @Test
    void p06StudyPlanIsValidAndRespectsCapacity() throws Exception {
        ObjectNode input = obj();
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        ArrayNode availability = Json.array();
        availability.add(Json.object().put("date", today).put("minutes", 120));
        availability.add(Json.object().put("date", tomorrow).put("minutes", 120));
        input.set("availability", availability);
        ArrayNode topics = Json.array();
        ObjectNode topic = Json.object();
        topic.put("topicId", "1");
        topic.put("title", "Recursion");
        topic.put("importance", "HIGH");
        topic.put("estimatedMinutes", 90);
        topics.add(topic);
        input.set("topics", topics);

        JsonNode out = run(AiAction.P06_STUDY_PLAN_GENERATION, input);
        AiValidators.studyPlan().validate(out);
        assertFalse(out.path("tasks").isEmpty(), "the plan must contain scheduled tasks");

        java.util.Map<String, Integer> perDay = new java.util.HashMap<>();
        for (JsonNode task : out.path("tasks")) {
            perDay.merge(task.path("date").asText(), task.path("estimatedMinutes").asInt(), Integer::sum);
        }
        for (var entry : perDay.entrySet()) {
            assertTrue(entry.getValue() <= 120,
                    "day " + entry.getKey() + " must not exceed the declared 120 minutes");
        }
    }

    @Test
    void p07FeasibilityIsValidAndArithmeticallyConsistent() throws Exception {
        ObjectNode input = obj();
        input.set("availability", one("minutes", "120"));
        input.set("topicWorkloadMinutes", one("minutes", "100"));
        input.put("revisionPracticeReserveMinutes", 20);

        JsonNode out = run(AiAction.P07_FEASIBILITY_ASSESSMENT, input);
        AiValidators.feasibility().validate(out);
        assertEquals(0, out.path("bufferMinutes").asInt());
    }

    @Test
    void p08AdaptiveReplanningIsValid() throws Exception {
        ObjectNode input = obj();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        input.set("remainingAvailability", one("date", tomorrow, "minutes", "120"));
        ObjectNode currentPlan = Json.object();
        ArrayNode tasks = Json.array();
        ObjectNode task = Json.object();
        task.put("taskKey", "t1");
        task.put("status", "PENDING");
        task.put("topicId", "1");
        task.put("topicTitle", "Recursion");
        task.put("taskType", "LEARN");
        task.put("title", "Study: Recursion");
        task.put("objective", "Understand recursion");
        task.put("studyMethod", "Reading");
        task.put("estimatedMinutes", 60);
        task.put("date", tomorrow);
        task.put("order", 1);
        task.put("priority", "HIGH");
        tasks.add(task);
        currentPlan.set("tasks", tasks);
        input.set("currentPlan", currentPlan);

        JsonNode out = run(AiAction.P08_ADAPTIVE_REPLANNING, input);
        AiValidators.replan().validate(out);
        assertFalse(out.path("tasks").isEmpty());
    }

    @Test
    void p09MockPaperMarksSumExactly() throws Exception {
        ObjectNode input = obj();
        input.put("subjectName", "Computer Science");
        input.put("totalMarks", 20);
        input.put("questionCount", 3);
        input.set("questionTypes", Json.array().add("MCQ").add("SHORT_ANSWER").add("THEORY"));
        input.set("topics", one("topicId", "1", "title", "Recursion"));

        JsonNode out = run(AiAction.P09_MOCK_PAPER_GENERATION, input);
        AiValidators.mockPaper().validate(out);
        int sum = 0;
        for (JsonNode q : out.path("questions")) {
            sum += q.path("marks").asInt();
        }
        assertEquals(20, sum);
        assertEquals(20, out.path("declaredTotalMarks").asInt());
    }

    @Test
    void p10PracticeQuestionsAreValid() throws Exception {
        ObjectNode input = obj();
        input.put("count", 3);
        input.set("questionTypes", Json.array().add("MCQ").add("SHORT_ANSWER"));
        input.set("topics", one("topicId", "1", "title", "Recursion"));
        input.put("difficulty", "EASY");

        JsonNode out = run(AiAction.P10_PRACTICE_QUESTION_GENERATION, input);
        AiValidators.practiceQuestions().validate(out);
        assertEquals(3, out.path("questions").size());
    }

    @Test
    void p11AnswerKeysAreValid() throws Exception {
        ObjectNode input = obj();
        input.set("questions", one("questionId", "q1", "marks", "5", "modelAnswer", "A model answer."));

        JsonNode out = run(AiAction.P11_ANSWER_KEYS_AND_SOLUTIONS, input);
        AiValidators.answerKey().validate(out);
        assertEquals("q1", out.path("solutions").get(0).path("questionId").asText());
    }

    @Test
    void p12EvaluationStaysWithinMarks() throws Exception {
        ObjectNode input = obj();
        input.put("maximumMarks", 5);
        input.put("studentAnswer", "Recursion is a function that calls itself with a smaller input.");
        ObjectNode question = Json.object();
        question.put("questionId", "q1");
        question.put("marks", 5);
        input.set("question", question);

        JsonNode out = run(AiAction.P12_SUBJECTIVE_ANSWER_EVALUATION, input);
        AiValidators.evaluation().validate(out);
        assertTrue(out.path("awardedMarks").asDouble() <= 5);
    }

    @Test
    void p13ExplanationIsValid() throws Exception {
        ObjectNode input = obj();
        input.put("topic", "Recursion");
        input.put("question", "What is recursion?");
        input.put("depth", "STANDARD");

        JsonNode out = run(AiAction.P13_TOPIC_EXPLANATIONS, input);
        AiValidators.explanation().validate(out);
        assertFalse(out.path("directAnswer").asText().isBlank());
    }

    @Test
    void p14RevisionResourcesAreValid() throws Exception {
        ObjectNode input = obj();
        input.set("topics", one("topicId", "1", "title", "Recursion"));

        JsonNode out = run(AiAction.P14_REVISION_RESOURCES, input);
        AiValidators.revisionResources().validate(out);
        assertEquals(1, out.path("topicResources").size());
    }

    @Test
    void p15ReadinessIsValid() throws Exception {
        ObjectNode input = obj();
        ObjectNode metrics = Json.object();
        metrics.put("completionPercent", 50);
        metrics.put("practiceAverage", -1);
        input.set("metrics", metrics);
        input.set("topics", one("topicId", "1", "title", "Recursion", "status", "PENDING"));

        JsonNode out = run(AiAction.P15_READINESS_ANALYSIS, input);
        AiValidators.readiness().validate(out);
        assertFalse(out.path("readinessSummary").asText().isBlank());
    }

    @Test
    void p16SessionFeedbackIsValid() throws Exception {
        ObjectNode input = obj();
        input.put("plannedMinutes", 60);
        input.put("actualMinutes", 45);

        JsonNode out = run(AiAction.P16_STUDY_SESSION_FEEDBACK, input);
        AiValidators.sessionFeedback().validate(out);
        assertFalse(out.path("summary").asText().isBlank());
    }

    @Test
    void p17LastMinuteRevisionIsValid() throws Exception {
        ObjectNode input = obj();
        input.set("availability", one("minutes", "60"));
        input.set("incompleteTopics", one("topicId", "1", "title", "Recursion"));

        JsonNode out = run(AiAction.P17_LAST_MINUTE_REVISION, input);
        AiValidators.lastMinute().validate(out);
        assertFalse(out.path("priorityBlocks").isEmpty());
    }

    @Test
    void p18PlanChangeExplanationIsValid() throws Exception {
        ObjectNode input = obj();
        input.set("changes", one("type", "MOVED", "title", "Study: Recursion", "detail", "Moved from Day 1 to Day 2."));
        input.put("previousVersion", 1);

        JsonNode out = run(AiAction.P18_PLAN_CHANGE_EXPLANATIONS, input);
        AiValidators.planChangeExplanation().validate(out);
        assertFalse(out.path("summary").asText().isBlank());
    }

    @Test
    void assistantIntakeIsValid() throws Exception {
        ObjectNode input = obj();
        input.put("message", "Please help me plan for my exam next month");
        input.put("hasSyllabus", false);

        JsonNode out = run(AiAction.ASSISTANT_INTAKE, input);
        AiValidators.assistantIntake().validate(out);
        assertEquals("CREATE_PLAN", out.path("actionRequested").asText());
        assertFalse(out.path("missingInformation").isEmpty());
    }

    @Test
    void theProviderIsAlwaysLabelledSimulated() {
        assertEquals("simulated-mock", ai.providerId());
        assertTrue(ai.providerId().contains("simulated"));
        assertTrue(ai.isConfigured());
    }
}
