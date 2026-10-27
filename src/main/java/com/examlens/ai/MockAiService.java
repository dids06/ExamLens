package com.examlens.ai;

import com.examlens.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic simulated provider used ONLY for automated tests and explicit offline
 * demonstration (AI_PROVIDER=mock). It is always reported as "simulated" so the interface
 * can label it clearly; it is never presented as a Gemini response.
 */
public class MockAiService implements AiService {

    @Override
    public String providerId() {
        return "simulated-mock";
    }

    @Override
    public String modelId() {
        return "exam-lens-simulated-1";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String unavailabilityMessage() {
        return null;
    }

    @Override
    public AiResponse generate(AiRequest request) throws AiException {
        JsonNode input;
        try {
            input = request.getInputJson() == null ? Json.object() : Json.read(request.getInputJson());
        } catch (RuntimeException e) {
            throw new AiException(AiFailureType.SCHEMA_INVALID, "mock input was not valid JSON");
        }
        ObjectNode output = switch (request.getAction()) {
            case P01_SYLLABUS_EXTRACTION -> syllabusExtraction(input);
            case P02_TEACHER_INSTRUCTIONS -> teacherInstructions(input);
            case P03_STUDY_MATERIAL_INDEXING -> materialIndex(input);
            case P04_SOURCE_COMPARISON -> sourceComparison(input);
            case P05_PYQ_ANALYSIS -> pyqAnalysis(input);
            case P06_STUDY_PLAN_GENERATION -> studyPlan(input);
            case P07_FEASIBILITY_ASSESSMENT -> feasibility(input);
            case P08_ADAPTIVE_REPLANNING -> replan(input);
            case P09_MOCK_PAPER_GENERATION -> mockPaper(input);
            case P10_PRACTICE_QUESTION_GENERATION -> practiceQuestions(input);
            case P11_ANSWER_KEYS_AND_SOLUTIONS -> answerKey(input);
            case P12_SUBJECTIVE_ANSWER_EVALUATION -> evaluate(input);
            case P13_TOPIC_EXPLANATIONS -> explanation(input);
            case P14_REVISION_RESOURCES -> revisionResources(input);
            case P15_READINESS_ANALYSIS -> readiness(input);
            case P16_STUDY_SESSION_FEEDBACK -> sessionFeedback(input);
            case P17_LAST_MINUTE_REVISION -> lastMinute(input);
            case P18_PLAN_CHANGE_EXPLANATIONS -> planChangeExplanation(input);
            case ASSISTANT_INTAKE -> assistantIntake(input);
        };
        return new AiResponse(Json.write(output), providerId(), modelId(), 5L, 0, 0);
    }

    // ---------------------------------------------------------------- P01

    private ObjectNode syllabusExtraction(JsonNode input) {
        ObjectNode out = Json.object();
        out.put("summary", "Simulated extraction: structured topics derived from the supplied document blocks.");
        Map<String, ObjectNode> units = new LinkedHashMap<>();
        ArrayNode classifications = Json.array();
        JsonNode blocks = input.path("blocks");
        if (blocks.isArray()) {
            for (JsonNode block : blocks) {
                String blockKey = block.path("blockKey").asText("block");
                Integer page = block.has("page") && !block.get("page").isNull() ? block.get("page").asInt() : null;
                String text = block.path("text").asText("");
                String currentUnit = "Unit 1";
                for (String rawLine : text.split("\\r?\\n")) {
                    String line = rawLine.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    ObjectNode cls = Json.object();
                    cls.put("sourceBlockId", blockKey);
                    if (page != null) {
                        cls.put("page", page);
                    }
                    cls.put("originalText", line);
                    if (line.matches("(?i)^page\\s+\\d+\\s+of\\s+\\d+.*") || line.matches("^\\d{1,3}$")) {
                        cls.put("classification", "PAGE_NUMBER");
                        cls.put("confidence", 0.99);
                        cls.putNull("normalizedTopic");
                        cls.putNull("parentUnit");
                        cls.put("reason", "The line is pagination rather than academic content.");
                        cls.put("includeInSyllabus", false);
                        classifications.add(cls);
                        continue;
                    }
                    if (line.length() < 4) {
                        cls.put("classification", "OCR_NOISE");
                        cls.put("confidence", 0.4);
                        cls.putNull("normalizedTopic");
                        cls.putNull("parentUnit");
                        cls.put("reason", "The line is too short to be meaningful syllabus content.");
                        cls.put("includeInSyllabus", false);
                        classifications.add(cls);
                        continue;
                    }
                    if (line.matches("(?i)^(unit|module|chapter|section)\\s+[ivxlcdm0-9]+.*")) {
                        currentUnit = line.length() > 90 ? line.substring(0, 90) : line;
                        units.computeIfAbsent(currentUnit, k -> {
                            ObjectNode u = Json.object();
                            u.put("unitName", k);
                            u.put("unitRationale", "A unit-style heading was found in the uploaded document.");
                            u.set("topics", Json.array());
                            return u;
                        });
                        cls.put("classification", "SYLLABUS_TOPIC");
                        cls.put("confidence", 0.9);
                        cls.put("normalizedTopic", currentUnit);
                        cls.put("parentUnit", currentUnit);
                        cls.put("reason", "A unit heading explicitly identifies a syllabus unit.");
                        cls.put("includeInSyllabus", true);
                        classifications.add(cls);
                        continue;
                    }
                    ObjectNode unit = units.computeIfAbsent(currentUnit, k -> {
                        ObjectNode u = Json.object();
                        u.put("unitName", k);
                        u.put("unitRationale", "Grouped under the most recent unit heading found in the document.");
                        u.set("topics", Json.array());
                        return u;
                    });
                    String title = line.length() > 120 ? line.substring(0, 120) : line;
                    ObjectNode topic = Json.object();
                    topic.put("title", title);
                    topic.set("subtopics", Json.array());
                    topic.set("learningOutcomes", Json.array());
                    topic.put("estimatedMinutes", 60);
                    topic.put("importance", "MEDIUM");
                    topic.put("difficulty", "MEDIUM");
                    ArrayNode pages = Json.array();
                    if (page != null) {
                        pages.add(page);
                    }
                    topic.set("sourcePages", pages);
                    topic.put("confidence", "MEDIUM");
                    topic.put("needsReview", true);
                    topic.put("reason", "Simulated extraction from document text; the student must confirm it.");
                    ((ArrayNode) unit.get("topics")).add(topic);

                    cls.put("classification", "SYLLABUS_TOPIC");
                    cls.put("confidence", 0.75);
                    cls.put("normalizedTopic", title);
                    cls.put("parentUnit", currentUnit);
                    cls.put("reason", "A heading-like line inside a unit was treated as a candidate topic.");
                    cls.put("includeInSyllabus", true);
                    classifications.add(cls);
                }
            }
        }
        ArrayNode unitsArray = Json.array();
        units.values().forEach(unitsArray::add);
        out.set("units", unitsArray);
        out.set("classifications", classifications);
        ArrayNode warnings = Json.array();
        warnings.add("Simulated provider: topics were derived with simple rules, not by Gemini. "
                + "Confirm every topic before planning.");
        out.set("warnings", warnings);
        out.set("assumptions", Json.array());
        out.set("nextActions", Json.array());
        return out;
    }

    // ---------------------------------------------------------------- P02

    private ObjectNode teacherInstructions(JsonNode input) {
        ObjectNode out = Json.object();
        out.put("summary", "Simulated extraction of teacher guidance from the supplied text.");
        ArrayNode hints = Json.array();
        ArrayNode instructions = Json.array();
        ArrayNode format = Json.array();
        ArrayNode uncertainties = Json.array();
        String text = collectText(input, "blocks", "rawText");
        int index = 0;
        for (String line : text.split("\\r?\\n")) {
            String s = line.trim();
            if (s.length() < 6) {
                continue;
            }
            String lower = s.toLowerCase(Locale.ROOT);
            ObjectNode hint = null;
            if (lower.contains("important") || lower.contains("focus") || lower.contains("revise")) {
                hint = Json.object();
                hint.put("text", s);
                hint.put("evidenceQuote", s);
                hint.putNull("sourceReference");
                hint.put("confidence", "MEDIUM");
                hints.add(hint);
            }
            if (lower.contains("marks") || lower.contains("duration") || lower.contains("sections")
                    || lower.contains("pattern") || lower.contains("question")) {
                format.add(s);
            } else if (lower.contains("submit") || lower.contains("deadline") || lower.contains("bring")
                    || lower.contains("allowed") || lower.contains("not allowed")) {
                instructions.add(s);
            }
            index++;
            if (index > 60) {
                break;
            }
        }
        if (hints.isEmpty()) {
            uncertainties.add("No explicit teacher hints were detected in the supplied text. "
                    + "Confirm with your teacher or add notes manually.");
        }
        out.set("hints", hints);
        out.set("importantTopics", Json.array());
        out.set("instructions", instructions);
        out.set("examFormatDetails", format);
        out.set("uncertainties", uncertainties);
        return out;
    }

    // ---------------------------------------------------------------- P03

    private ObjectNode materialIndex(JsonNode input) {
        ObjectNode out = Json.object();
        out.put("summary", "Simulated index of the supplied study material.");
        ArrayNode sections = Json.array();
        ObjectNode section = Json.object();
        section.put("title", input.path("documentName").asText("Uploaded material"));
        JsonNode blocks = input.path("blocks");
        Integer minPage = null;
        Integer maxPage = null;
        ArrayNode coverage = Json.array();
        if (blocks.isArray()) {
            for (JsonNode b : blocks) {
                if (b.has("page") && !b.get("page").isNull()) {
                    int p = b.get("page").asInt();
                    minPage = minPage == null ? p : Math.min(minPage, p);
                    maxPage = maxPage == null ? p : Math.max(maxPage, p);
                }
            }
        }
        for (JsonNode topic : input.path("syllabusTopics")) {
            String title = topic.path("title").asText("");
            if (title.isBlank()) {
                continue;
            }
            String evidence = findEvidence(blocks, title);
            ObjectNode item = Json.object();
            item.put("topic", title);
            item.put("coverage", evidence == null ? "POSSIBLE" : "EXPLICIT");
            item.put("evidence", evidence == null
                    ? "Simulated index found no direct match; verify coverage manually."
                    : evidence);
            coverage.add(item);
        }
        if (minPage != null) {
            section.put("startPage", minPage);
        }
        if (maxPage != null) {
            section.put("endPage", maxPage);
        }
        section.set("topics", coverage);
        sections.add(section);
        out.set("sections", sections);
        out.set("unmappedTopics", Json.array());
        ArrayNode warnings = Json.array();
        if (minPage == null) {
            warnings.add("The extracted text had no page metadata, so no page references are shown.");
        }
        warnings.add("Simulated provider: coverage labels are rule-based and must not be treated as verified.");
        out.set("warnings", warnings);
        return out;
    }

    // ---------------------------------------------------------------- P04

    private ObjectNode sourceComparison(JsonNode input) {
        ObjectNode out = Json.object();
        out.put("summary", "Simulated comparison of the indexed sources.");
        ArrayNode recommendations = Json.array();
        for (JsonNode topic : input.path("syllabusTopics")) {
            String title = topic.path("title").asText("");
            if (title.isBlank()) {
                continue;
            }
            ObjectNode rec = Json.object();
            rec.put("topic", title);
            String chosenId = null;
            String chosenName = null;
            List<String> others = new ArrayList<>();
            for (JsonNode source : input.path("sources")) {
                String id = source.path("sourceId").asText("");
                String name = source.path("name").asText("");
                if (chosenId == null) {
                    chosenId = id;
                    chosenName = name;
                } else {
                    others.add(name);
                }
            }
            rec.put("primarySourceId", chosenId == null ? "" : chosenId);
            rec.put("primarySourceName", chosenName == null ? "" : chosenName);
            rec.set("pageReferences", Json.array());
            ArrayNode otherArray = Json.array();
            others.forEach(otherArray::add);
            rec.set("otherSources", otherArray);
            rec.put("rationale", "Simulated selection using the first supplied indexed source that mentions the topic.");
            rec.put("confidence", "LOW");
            recommendations.add(rec);
        }
        out.set("recommendations", recommendations);
        out.set("uncoveredTopics", Json.array());
        ArrayNode warnings = Json.array();
        warnings.add("Simulated provider: recommendations are illustrative and not based on Gemini analysis.");
        out.set("warnings", warnings);
        return out;
    }

    // ---------------------------------------------------------------- P05

    private ObjectNode pyqAnalysis(JsonNode input) {
        ObjectNode out = Json.object();
        out.put("summary", "Simulated analysis of the supplied previous-year questions.");
        ArrayNode questions = Json.array();
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Integer> marks = new LinkedHashMap<>();
        Map<String, ArrayNode> evidence = new LinkedHashMap<>();
        Map<String, ArrayNode> paperIds = new LinkedHashMap<>();
        for (JsonNode paper : input.path("papers")) {
            String paperId = paper.path("paperId").asText("");
            for (JsonNode q : paper.path("questions")) {
                String key = q.path("questionKey").asText("");
                String text = q.path("text").asText("");
                Integer qMarks = q.has("marks") && !q.get("marks").isNull() ? q.get("marks").asInt() : null;
                Integer page = q.has("page") && !q.get("page").isNull() ? q.get("page").asInt() : null;
                String topic = matchTopic(text, input.path("syllabusTopics"));
                ObjectNode item = Json.object();
                item.put("questionKey", key);
                item.put("text", text);
                if (qMarks != null) {
                    item.put("marks", qMarks);
                }
                if (page != null) {
                    item.put("page", page);
                }
                item.put("year", paper.path("year").asText(""));
                item.put("paperId", paperId);
                item.put("topic", topic == null ? "" : topic);
                questions.add(item);
                if (topic != null) {
                    counts.merge(topic, 1, Integer::sum);
                    if (qMarks != null) {
                        marks.merge(topic, qMarks, Integer::sum);
                    }
                    evidence.computeIfAbsent(topic, k -> Json.array()).add(key);
                    ArrayNode pids = paperIds.computeIfAbsent(topic, k -> Json.array());
                    boolean seen = false;
                    for (JsonNode existing : pids) {
                        if (existing.asText().equals(paperId)) {
                            seen = true;
                        }
                    }
                    if (!seen) {
                        pids.add(paperId);
                    }
                }
            }
        }
        ArrayNode frequency = Json.array();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            ObjectNode f = Json.object();
            f.put("topic", e.getKey());
            f.put("observedCount", e.getValue());
            f.set("paperIds", paperIds.getOrDefault(e.getKey(), Json.array()));
            f.set("years", Json.array());
            f.put("observedMarks", marks.getOrDefault(e.getKey(), 0));
            f.put("confidence", "MEDIUM");
            f.set("evidenceQuestionIds", evidence.getOrDefault(e.getKey(), Json.array()));
            frequency.add(f);
        }
        out.set("questions", questions);
        out.set("topicFrequency", frequency);
        ArrayNode patterns = Json.array();
        patterns.add("Counts are derived only from the questions extracted from the uploaded papers.");
        out.set("recurringPatterns", patterns);
        out.set("unmatchedQuestions", Json.array());
        ArrayNode limitations = Json.array();
        limitations.add("Simulated provider: frequencies are simple keyword matches and are not predictive.");
        out.set("limitations", limitations);
        out.set("warnings", Json.array());
        return out;
    }

    // ---------------------------------------------------------------- P06

    private ObjectNode studyPlan(JsonNode input) {
        ObjectNode out = Json.object();
        List<DateCapacity> availability = readAvailability(input.path("availability"));
        JsonNode topics = input.path("topics");

        int totalCapacity = availability.stream().mapToInt(d -> d.minutes).sum();
        int totalRequired = 0;
        ArrayNode topicPriorities = Json.array();
        int rank = 1;
        for (JsonNode t : topics) {
            int minutes = t.has("estimatedMinutes") && !t.get("estimatedMinutes").isNull()
                    ? t.get("estimatedMinutes").asInt() : 60;
            totalRequired += minutes;
            ObjectNode p = Json.object();
            p.put("topicId", t.path("topicId").asText(""));
            p.put("topicTitle", t.path("title").asText(""));
            p.put("priorityRank", rank++);
            p.put("priority", t.path("importance").asText("MEDIUM"));
            p.put("recommendedMinutes", minutes);
            p.put("reason", "Simulated priority based on the importance supplied in the confirmed syllabus.");
            p.set("sourceReferences", Json.array());
            topicPriorities.add(p);
        }
        out.set("topics", topicPriorities);

        String feasibility = totalRequired > totalCapacity ? "INFEASIBLE"
                : (totalRequired > totalCapacity * 0.85 ? "TIGHT" : "FEASIBLE");
        out.put("summary", "Simulated study plan: " + topics.size() + " topic(s) totalling "
                + totalRequired + " minutes were distributed across " + availability.size()
                + " available day(s) (" + totalCapacity + " minutes of declared capacity). "
                + "This schedule was produced by the deterministic simulated provider, and every topic "
                + "must be confirmed by the student.");
        out.put("feasibility", feasibility);
        out.put("feasibilityNote", feasibility.equals("FEASIBLE")
                ? "The estimated workload fits the declared availability."
                : "The estimated workload is close to or above the declared availability; trade-offs are listed.");
        out.put("totalEstimatedMinutes", totalRequired);

        // Pack tasks into available dates without exceeding capacity.
        ArrayNode tasks = Json.array();
        int taskIndex = 1;
        int dateIndex = 0;
        int remainingToday = availability.isEmpty() ? 0 : availability.get(0).minutes;
        int orderCounter = 1;
        for (JsonNode t : topics) {
            int minutes = t.has("estimatedMinutes") && !t.get("estimatedMinutes").isNull()
                    ? t.get("estimatedMinutes").asInt() : 60;
            String topicId = t.path("topicId").asText("");
            String title = t.path("title").asText("");
            int remaining = minutes;
            while (remaining > 0 && dateIndex < availability.size()) {
                if (remainingToday <= 0) {
                    dateIndex++;
                    if (dateIndex >= availability.size()) {
                        break;
                    }
                    remainingToday = availability.get(dateIndex).minutes;
                    continue;
                }
                int chunk = Math.min(remaining, Math.max(15, remainingToday));
                chunk = Math.min(chunk, remainingToday);
                ObjectNode task = Json.object();
                task.put("taskKey", "t" + (taskIndex++));
                task.put("topicId", topicId);
                task.put("topicTitle", title);
                task.put("taskType", remaining == minutes ? "LEARN" : "PRACTICE");
                task.put("title", (remaining == minutes ? "Study: " : "Continue: ") + title);
                task.put("objective", "Work through the key ideas of " + title + " and confirm understanding.");
                task.put("studyMethod", "Active reading plus short recall questions");
                task.put("estimatedMinutes", chunk);
                task.put("date", availability.get(dateIndex).date.toString());
                task.put("order", orderCounter++);
                task.put("priority", t.path("importance").asText("MEDIUM"));
                task.put("rationale", "Simulated allocation based on declared availability and topic order.");
                task.set("prerequisites", Json.array());
                task.set("learningObjectives", Json.array());
                task.set("activities", Json.array());
                tasks.add(task);
                remaining -= chunk;
                remainingToday -= chunk;
            }
            if (remaining > 0) {
                // Not enough remaining capacity — report the unscheduled portion explicitly.
                ArrayNode unscheduled = (ArrayNode) out.withArray("unscheduled");
                unscheduled.add("Approximately " + remaining + " minutes for \"" + title
                        + "\" could not be scheduled within the declared availability.");
            }
        }

        // Reserve revision capacity when possible.
        out.set("tasks", tasks);
        out.put("revisionMinutes", Math.min(60, Math.max(0, totalCapacity / 5)));
        out.put("practiceMinutes", Math.min(60, Math.max(0, totalCapacity / 6)));
        out.set("assumptions", Json.array().add("Simulated provider used a simple packing strategy."));
        ArrayNode warnings = Json.array();
        warnings.add("Simulated provider: this schedule was NOT produced by Gemini. Configure GEMINI_API_KEY for real planning.");
        if (totalRequired > totalCapacity) {
            warnings.add("The estimated workload exceeds the declared availability. Review the trade-offs.");
        }
        out.set("warnings", warnings);
        out.set("risks", Json.array());
        out.set("tradeoffs", Json.array());
        out.set("nextActions", Json.array());
        return out;
    }

    // ---------------------------------------------------------------- P07

    private ObjectNode feasibility(JsonNode input) {
        int available = 0;
        for (JsonNode d : input.path("availability")) {
            available += d.path("minutes").asInt(0);
        }
        int required = 0;
        for (JsonNode t : input.path("topicWorkloadMinutes")) {
            required += t.path("minutes").asInt(0);
        }
        required += input.path("revisionPracticeReserveMinutes").asInt(0);
        ObjectNode out = Json.object();
        out.put("availableMinutes", available);
        out.put("estimatedRequiredMinutes", required);
        out.put("bufferMinutes", available - required);
        String feasibility = required > available ? "INFEASIBLE" : (required > available * 0.85 ? "TIGHT" : "FEASIBLE");
        out.put("feasibility", feasibility);
        out.put("notes", "Simulated feasibility check from the supplied arithmetic.");
        ArrayNode tradeoffs = Json.array();
        if (required > available) {
            tradeoffs.add("Reduce optional practice sessions to protect core learning time.");
            tradeoffs.add("Prioritise high-importance topics and defer low-priority detail.");
        }
        out.set("highestPriorityTradeoffs", tradeoffs);
        out.set("assumptions", Json.array().add("Totals computed from the supplied values only."));
        return out;
    }

    // ---------------------------------------------------------------- P08

    private ObjectNode replan(JsonNode input) {
        ObjectNode out = Json.object();
        List<DateCapacity> availability = readAvailability(input.path("remainingAvailability"));
        JsonNode currentTasks = input.path("currentPlan").path("tasks");
        ArrayNode tasks = Json.array();
        ArrayNode preserved = Json.array();
        ArrayNode removed = Json.array();
        int index = 1;
        int dateIndex = 0;
        int remainingToday = availability.isEmpty() ? 0 : availability.get(0).minutes;
        int order = 1;
        for (JsonNode t : currentTasks) {
            String status = t.path("status").asText("PENDING");
            String taskKey = t.path("taskKey").asText("t" + index);
            if ("COMPLETED".equals(status) || "SKIPPED".equals(status)) {
                preserved.add(taskKey);
                ObjectNode copy = t.deepCopy();
                copy.put("changeType", "UNCHANGED");
                tasks.add(copy);
                continue;
            }
            if ("REMOVED_BY_STUDENT".equals(status)) {
                removed.add(taskKey);
                continue;
            }
            int minutes = t.path("estimatedMinutes").asInt(45);
            int remain = minutes;
            boolean scheduled = false;
            while (remain > 0 && dateIndex < availability.size()) {
                if (remainingToday <= 0) {
                    dateIndex++;
                    if (dateIndex >= availability.size()) {
                        break;
                    }
                    remainingToday = availability.get(dateIndex).minutes;
                    continue;
                }
                int chunk = Math.min(remain, remainingToday);
                ObjectNode task = Json.object();
                task.put("taskKey", taskKey + (scheduled ? "-b" : ""));
                task.put("existingTaskKey", taskKey);
                task.put("topicId", t.path("topicId").asText(""));
                task.put("topicTitle", t.path("topicTitle").asText(t.path("title").asText("")));
                task.put("taskType", t.path("taskType").asText("LEARN"));
                task.put("title", t.path("title").asText("Study task"));
                task.put("objective", t.path("objective").asText(""));
                task.put("studyMethod", t.path("studyMethod").asText(""));
                task.put("estimatedMinutes", chunk);
                task.put("date", availability.get(dateIndex).date.toString());
                task.put("order", order++);
                task.put("priority", t.path("priority").asText("MEDIUM"));
                task.put("rationale", "Simulated replanning kept the remaining work and fitted it into the new availability.");
                task.set("prerequisites", Json.array());
                task.set("learningObjectives", Json.array());
                task.set("activities", Json.array());
                String changeType = t.path("date").asText("").equals(availability.get(dateIndex).date.toString())
                        ? "UPDATED" : "MOVED";
                task.put("changeType", changeType);
                tasks.add(task);
                remain -= chunk;
                remainingToday -= chunk;
                scheduled = true;
            }
            if (!scheduled) {
                removed.add(taskKey);
            }
        }
        out.set("tasks", tasks);
        out.set("preservedTaskKeys", preserved);
        out.set("removedTaskKeys", removed);
        out.put("summary", "Simulated replanning of the remaining tasks.");
        out.put("feasibility", "FEASIBLE");
        out.put("capacityRisk", "LOW");
        out.put("totalEstimatedMinutes", totalTaskMinutes(tasks));
        out.put("changeSummary", "Remaining tasks were re-fitted into the updated availability. Completed work was preserved.");
        out.set("assumptions", Json.array());
        out.set("warnings", Json.array().add("Simulated provider: this plan was not produced by Gemini."));
        out.set("nextActions", Json.array());
        return out;
    }

    // ---------------------------------------------------------------- P09

    private ObjectNode mockPaper(JsonNode input) {
        int totalMarks = Math.max(1, input.path("totalMarks").asInt(50));
        int count = Math.max(1, input.path("questionCount").asInt(Math.min(10, totalMarks)));
        List<String> types = new ArrayList<>();
        for (JsonNode t : input.path("questionTypes")) {
            types.add(t.asText().toUpperCase(Locale.ROOT));
        }
        if (types.isEmpty()) {
            types.add("MCQ");
            types.add("SHORT_ANSWER");
            types.add("THEORY");
        }
        List<JsonNode> topics = new ArrayList<>();
        input.path("topics").forEach(topics::add);
        ObjectNode out = Json.object();
        out.put("title", input.path("subjectName").asText("Practice paper") + " — practice paper");
        out.put("instructions", "Answer all questions. The total marks equal the requested configuration.");
        out.put("declaredTotalMarks", totalMarks);
        if (input.has("durationMinutes")) {
            out.put("durationMinutes", input.path("durationMinutes").asInt());
        }
        ArrayNode questions = Json.array();
        int[] allocations = allocateMarks(totalMarks, count);
        for (int i = 0; i < count; i++) {
            String type = types.get(i % types.size());
            JsonNode topic = topics.isEmpty() ? null : topics.get(i % topics.size());
            String topicTitle = topic == null ? "General" : topic.path("title").asText("General");
            ObjectNode q = Json.object();
            q.put("clientKey", "q" + (i + 1));
            q.put("type", type);
            q.put("topic", topicTitle);
            if (topic != null) {
                q.put("topicId", topic.path("topicId").asText(""));
            }
            q.put("prompt", promptFor(type, topicTitle, allocations[i]));
            if ("MCQ".equals(type)) {
                ArrayNode options = Json.array();
                options.add("Correct statement about " + topicTitle);
                options.add("Plausible but incorrect option one");
                options.add("Plausible but incorrect option two");
                options.add("Plausible but incorrect option three");
                q.set("options", options);
                q.put("correctOption", "A");
            }
            q.put("marks", allocations[i]);
            q.put("difficulty", input.path("difficulty").asText("MEDIUM"));
            q.put("modelAnswer", "Simulated model answer for: " + topicTitle + ". "
                    + "A complete answer states the definition, explains the mechanism and gives one concrete example.");
            ArrayNode rubric = Json.array();
            ObjectNode criterion = Json.object();
            criterion.put("criterion", "Accurate explanation of " + topicTitle);
            criterion.put("marks", allocations[i]);
            rubric.add(criterion);
            q.set("rubric", rubric);
            q.put("expectedMinutes", Math.max(1, allocations[i] * 2));
            questions.add(q);
        }
        out.set("questions", questions);
        out.set("warnings", Json.array().add("Simulated provider: this paper was not produced by Gemini."));
        out.set("assumptions", Json.array());
        return out;
    }

    private ObjectNode practiceQuestions(JsonNode input) {
        ObjectNode out = Json.object();
        out.put("summary", "Simulated practice question set.");
        int count = Math.max(1, input.path("count").asInt(5));
        ObjectNode paperInput = Json.object();
        paperInput.put("totalMarks", count * 2);
        paperInput.put("questionCount", count);
        paperInput.set("questionTypes", input.path("questionTypes"));
        paperInput.set("topics", input.path("topics"));
        paperInput.put("difficulty", input.path("difficulty").asText("MEDIUM"));
        paperInput.put("subjectName", "Practice");
        ObjectNode generated = mockPaper(paperInput);
        out.set("questions", generated.get("questions"));
        out.set("warnings", generated.get("warnings"));
        return out;
    }

    private ObjectNode answerKey(JsonNode input) {
        ObjectNode out = Json.object();
        out.put("summary", "Simulated worked solutions.");
        ArrayNode solutions = Json.array();
        for (JsonNode q : input.path("questions")) {
            int marks = q.path("marks").asInt(1);
            ObjectNode s = Json.object();
            s.put("questionId", q.path("questionId").asText(q.path("clientKey").asText("")));
            String existing = q.path("modelAnswer").asText("");
            s.put("modelAnswer", existing.isBlank()
                    ? "Simulated model answer covering the key expected points." : existing);
            ArrayNode steps = Json.array();
            steps.add("Identify what the question asks.");
            steps.add("State the relevant concept precisely.");
            steps.add("Apply the concept and verify the result.");
            s.set("steps", steps);
            ArrayNode rubric = Json.array();
            ObjectNode criterion = Json.object();
            criterion.put("criterion", "Correctness of the answer");
            criterion.put("marks", marks);
            rubric.add(criterion);
            s.set("rubric", rubric);
            s.put("totalRubricMarks", marks);
            s.putNull("uncertainty");
            solutions.add(s);
        }
        out.set("solutions", solutions);
        out.set("warnings", Json.array().add("Simulated provider: solutions are illustrative, not Gemini-generated."));
        return out;
    }

    private ObjectNode evaluate(JsonNode input) {
        double maximum = input.path("maximumMarks").asDouble(input.path("question").path("marks").asDouble(1));
        String answer = input.path("studentAnswer").asText("");
        double ratio;
        if (answer.isBlank()) {
            ratio = 0.0;
        } else if (answer.trim().length() < 20) {
            ratio = 0.4;
        } else if (answer.trim().length() < 80) {
            ratio = 0.65;
        } else {
            ratio = 0.8;
        }
        double awarded = Math.round(maximum * ratio * 2) / 2.0;
        ObjectNode out = Json.object();
        out.put("questionId", input.path("question").path("questionId").asText(""));
        out.put("awardedMarks", awarded);
        out.put("maximumMarks", maximum);
        ArrayNode criteria = Json.array();
        ObjectNode c = Json.object();
        c.put("criterion", "Overall correctness and completeness");
        c.put("awardedMarks", awarded);
        c.put("maximumMarks", maximum);
        c.put("reason", answer.isBlank() ? "No answer was provided." : "Simulated evaluation based on answer completeness.");
        criteria.add(c);
        out.set("criterionResults", criteria);
        out.put("feedback", answer.isBlank() ? "No answer was submitted for this question."
                : "Simulated feedback: the answer addresses the question at a basic level. Expand the reasoning and add an example.");
        out.set("strengths", Json.array().add("Attempted to address the question directly."));
        out.set("improvements", Json.array().add("Add a concrete example and define key terms precisely."));
        out.put("confidence", "LOW");
        out.put("requiresManualReview", true);
        return out;
    }

    private ObjectNode explanation(JsonNode input) {
        String topic = input.path("topic").asText("the requested concept");
        String question = input.path("question").asText("");
        ObjectNode out = Json.object();
        out.put("topic", topic);
        out.put("directAnswer", "Simulated explanation for: " + question);
        out.put("explanation", "This simulated response outlines " + topic + " in a structured way: definition, purpose, "
                + "how it works, and where it is used. Configure GEMINI_API_KEY for a real, source-aware explanation.");
        ArrayNode examples = Json.array();
        examples.add("Example 1: a minimal, concrete illustration of " + topic + ".");
        examples.add("Example 2: a slightly more advanced scenario involving " + topic + ".");
        out.set("examples", examples);
        ArrayNode checks = Json.array();
        checks.add("Can you explain " + topic + " in one sentence without notes?");
        checks.add("Where would " + topic + " change the outcome of a program or system?");
        out.set("checkYourUnderstanding", checks);
        out.set("sourceReferences", Json.array());
        out.set("limitations", Json.array().add("Simulated provider: not based on your uploaded sources."));
        out.put("depth", input.path("depth").asText("STANDARD"));
        return out;
    }

    private ObjectNode revisionResources(JsonNode input) {
        ObjectNode out = Json.object();
        out.put("summary", "Simulated revision resources.");
        ArrayNode resources = Json.array();
        for (JsonNode t : input.path("topics")) {
            String title = t.path("title").asText("Topic");
            ObjectNode r = Json.object();
            r.put("topic", title);
            r.put("topicId", t.path("topicId").asText(""));
            ArrayNode keyPoints = Json.array();
            keyPoints.add(title + " — define the core term precisely.");
            keyPoints.add(title + " — state where it is used and one limitation.");
            r.set("keyPoints", keyPoints);
            ArrayNode cards = Json.array();
            ObjectNode card = Json.object();
            card.put("front", "What is the core idea of " + title + "?");
            card.put("back", "A concise definition plus one example.");
            cards.add(card);
            r.set("flashcards", cards);
            ArrayNode recall = Json.array();
            ObjectNode item = Json.object();
            item.put("question", "Explain " + title + " as you would in an exam answer.");
            item.put("answer", "Simulated answer outline: definition, mechanism, example, limitation.");
            recall.add(item);
            r.set("activeRecallQuestions", recall);
            r.set("sourceReferences", Json.array());
            resources.add(r);
        }
        out.set("topicResources", resources);
        out.set("warnings", Json.array().add("Simulated provider: verify these notes against your own material."));
        return out;
    }

    private ObjectNode readiness(JsonNode input) {
        ObjectNode out = Json.object();
        double completion = input.path("metrics").path("completionPercent").asDouble(0);
        double practiceAverage = input.path("metrics").path("practiceAverage").asDouble(-1);
        out.put("readinessSummary", "Simulated readiness summary: " + String.format(Locale.ROOT, "%.0f%%", completion)
                + " of scheduled tasks are complete. ExamLens computed these metrics from your persisted activity.");
        ArrayNode weak = Json.array();
        for (JsonNode t : input.path("topics")) {
            ArrayNode evidenceArr = Json.array();
            ObjectNode w = Json.object();
            w.put("topic", t.path("title").asText(""));
            w.put("topicId", t.path("topicId").asText(""));
            evidenceArr.add("Topic status: " + t.path("status").asText("UNKNOWN"));
            w.set("evidence", evidenceArr);
            w.put("recommendedNextAction", "Schedule a focused practice block and log the outcome.");
            w.put("confidence", "LOW");
            weak.add(w);
        }
        out.set("weakTopics", weak);
        out.set("strengths", Json.array().add("You have logged study activity on this exam."));
        out.set("coverageGaps", Json.array());
        ArrayNode limits = Json.array();
        limits.add("Simulated provider: this analysis is not Gemini-generated.");
        if (practiceAverage < 0) {
            limits.add("No practice attempts have been graded yet, so performance cannot be assessed.");
        }
        out.set("dataLimitations", limits);
        out.set("nextActions", Json.array().add("Generate a revision set for the weakest topic."));
        return out;
    }

    private ObjectNode sessionFeedback(JsonNode input) {
        int planned = input.path("plannedMinutes").asInt(0);
        int actual = input.path("actualMinutes").asInt(0);
        ObjectNode out = Json.object();
        out.put("summary", "You planned " + planned + " minutes and recorded " + actual + " minutes.");
        out.put("observation", actual >= planned
                ? "You met or exceeded the planned time for this session."
                : "This session ran short of the planned time.");
        ArrayNode suggestions = Json.array();
        suggestions.add("Start the next session with a 5-minute recall of what you covered today.");
        if (actual < planned) {
            suggestions.add("Break the next block into two shorter blocks with a clear stopping point.");
        }
        out.set("suggestions", suggestions);
        out.put("nextStep", "Continue with the next scheduled task in your plan.");
        out.set("limitations", Json.array().add("Simulated provider: feedback is based only on the recorded durations."));
        return out;
    }

    private ObjectNode lastMinute(JsonNode input) {
        ObjectNode out = Json.object();
        int available = 0;
        for (JsonNode d : input.path("availability")) {
            available += d.path("minutes").asInt(0);
        }
        ArrayNode blocks = Json.array();
        int remaining = available;
        for (JsonNode t : input.path("incompleteTopics")) {
            if (remaining <= 0) {
                break;
            }
            int minutes = Math.min(remaining, 45);
            ObjectNode b = Json.object();
            b.put("topic", t.path("title").asText(""));
            b.put("topicId", t.path("topicId").asText(""));
            b.put("recommendedMinutes", minutes);
            b.put("objective", "Recall and apply the highest-value points of " + t.path("title").asText(""));
            b.put("activity", "Active recall, then one exam-style question.");
            b.set("sourceReferences", Json.array());
            blocks.add(b);
            remaining -= minutes;
        }
        out.set("priorityBlocks", blocks);
        out.set("deferredItems", Json.array());
        out.put("summary", "Simulated last-minute revision plan within the remaining availability.");
        out.put("feasibility", remaining >= 0 ? "TIGHT" : "INFEASIBLE");
        out.set("tradeoffs", Json.array());
        out.set("assumptions", Json.array());
        out.set("warnings", Json.array().add("Simulated provider: not Gemini-generated."));
        return out;
    }

    private ObjectNode planChangeExplanation(JsonNode input) {
        ObjectNode out = Json.object();
        out.put("summary", "Simulated explanation of the computed plan changes.");
        ArrayNode changes = Json.array();
        for (JsonNode c : input.path("changes")) {
            ObjectNode item = Json.object();
            item.put("change", c.path("type").asText("CHANGE") + ": " + c.path("title").asText(""));
            item.put("evidence", c.path("detail").asText("Computed by ExamLens from the previous and new plan versions."));
            item.put("reason", "The change follows from the updated progress and availability evidence.");
            changes.add(item);
        }
        out.set("changes", changes);
        out.set("unchangedPriorities", Json.array());
        out.set("studentActionItems", Json.array().add("Review the highlighted changes and continue with the next task."));
        out.set("warnings", Json.array().add("Simulated provider: explanation is rule-based."));
        return out;
    }

    private ObjectNode assistantIntake(JsonNode input) {
        String message = input.path("message").asText("");
        String lower = message.toLowerCase(Locale.ROOT);
        ObjectNode out = Json.object();
        String action = "UNKNOWN";
        if (lower.contains("plan") || lower.contains("prepare") || lower.contains("schedule")) {
            action = "CREATE_PLAN";
        } else if (lower.contains("explain") || lower.contains("what is") || lower.contains("how does")) {
            action = "EXPLAIN_CONCEPT";
        } else if (lower.contains("revis") || lower.contains("flashcard")) {
            action = "GENERATE_REVISION";
        } else if (lower.contains("practice") || lower.contains("mock") || lower.contains("test")) {
            action = "GENERATE_PRACTICE";
        } else if (lower.contains("ready") || lower.contains("readiness") || lower.contains("weak")) {
            action = "ASSESS_READINESS";
        }
        out.put("intent", "Simulated intent classification");
        out.put("actionRequested", action);
        out.put("summary", message.length() > 200 ? message.substring(0, 200) : message);
        if (input.hasNonNull("examContext")) {
            out.put("subjectName", input.path("examContext").path("subjectName").asText(""));
        } else {
            out.putNull("subjectName");
        }
        out.putNull("examDateTime");
        out.put("weekdayMinutes", 120);
        out.put("weekendMinutes", 240);
        out.set("strengths", Json.array());
        out.set("difficulties", Json.array());
        out.putNull("requestedTopic");
        out.put("targetMarks", 0);
        out.set("constraints", Json.array());
        ArrayNode missing = Json.array();
        if (!input.path("hasSyllabus").asBoolean(false)) {
            missing.add("confirmed syllabus topics");
        }
        out.set("missingInformation", missing);
        out.put("clarificationQuestion", missing.isEmpty() ? null
                : "Upload your syllabus or confirm the topics, then I can build a plan.");
        out.set("referencedMaterials", Json.array());
        out.set("nextActions", Json.array());
        return out;
    }

    // ---------------------------------------------------------------- helpers

    private record DateCapacity(LocalDate date, int minutes) {
    }

    private List<DateCapacity> readAvailability(JsonNode array) {
        List<DateCapacity> list = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode d : array) {
                String date = d.path("date").asText("");
                int minutes = d.path("minutes").asInt(0);
                try {
                    list.add(new DateCapacity(LocalDate.parse(date), Math.max(0, minutes)));
                } catch (DateTimeParseException ignored) {
                    // skip malformed dates; backend validation will report them
                }
            }
        }
        return list;
    }

    private int totalTaskMinutes(ArrayNode tasks) {
        int total = 0;
        for (JsonNode t : tasks) {
            total += t.path("estimatedMinutes").asInt(0);
        }
        return total;
    }

    private int[] allocateMarks(int total, int count) {
        int[] out = new int[count];
        int base = Math.max(1, total / count);
        int assigned = 0;
        for (int i = 0; i < count; i++) {
            out[i] = base;
            assigned += base;
        }
        int diff = total - assigned;
        int i = 0;
        while (diff != 0 && i < count) {
            if (diff > 0) {
                out[i]++;
                diff--;
            } else if (out[i] > 1) {
                out[i]--;
                diff++;
            }
            i++;
        }
        if (diff != 0) {
            out[count - 1] += diff;
        }
        return out;
    }

    private String promptFor(String type, String topic, int marks) {
        return switch (type) {
            case "MCQ" -> "Which statement about " + topic + " is correct? (" + marks + " mark(s))";
            case "NUMERICAL" -> "Solve the following problem about " + topic + " and show your working. (" + marks + " marks)";
            case "SHORT_ANSWER" -> "Briefly explain " + topic + ". (" + marks + " marks)";
            case "THEORY" -> "Discuss " + topic + " in detail, including an example and its limitations. (" + marks + " marks)";
            case "APPLICATION" -> "Apply " + topic + " to the following scenario and justify your approach. (" + marks + " marks)";
            default -> "Answer the following question about " + topic + ". (" + marks + " marks)";
        };
    }

    private String collectText(JsonNode input, String blocksField, String rawField) {
        StringBuilder sb = new StringBuilder();
        JsonNode blocks = input.path(blocksField);
        if (blocks.isArray()) {
            for (JsonNode b : blocks) {
                sb.append(b.path("text").asText("")).append('\n');
            }
        }
        if (sb.length() == 0) {
            sb.append(input.path(rawField).asText(""));
        }
        return sb.toString();
    }

    private String findEvidence(JsonNode blocks, String topic) {
        if (!blocks.isArray()) {
            return null;
        }
        String needle = topic.toLowerCase(Locale.ROOT).trim();
        if (needle.isEmpty()) {
            return null;
        }
        for (JsonNode b : blocks) {
            String text = b.path("text").asText("");
            if (text.toLowerCase(Locale.ROOT).contains(needle)) {
                return text.length() > 160 ? text.substring(0, 160) : text;
            }
        }
        return null;
    }

    private String matchTopic(String text, JsonNode syllabusTopics) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String best = null;
        int bestScore = 0;
        for (JsonNode t : syllabusTopics) {
            String title = t.path("title").asText("");
            if (title.isBlank()) {
                continue;
            }
            int score = 0;
            for (String word : title.toLowerCase(Locale.ROOT).split("\\W+")) {
                if (word.length() > 3 && lower.contains(word)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = title;
            }
        }
        return bestScore > 0 ? best : null;
    }
}
