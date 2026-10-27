package com.examlens.service;

import com.examlens.ai.AiAction;
import com.examlens.ai.AiOutcome;
import com.examlens.ai.AiServiceRunner;
import com.examlens.ai.dto.LearningDtos;
import com.examlens.ai.dto.PlanDtos;
import com.examlens.ai.validate.AiValidators;
import com.examlens.dao.AiJobDao;
import com.examlens.dao.LearningDao;
import com.examlens.model.ConceptExplanation;
import com.examlens.model.Conversation;
import com.examlens.model.ConversationMessage;
import com.examlens.model.Exam;
import com.examlens.model.Topic;
import com.examlens.util.Json;
import com.examlens.util.Text;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The AI assistant: natural-language intake (ASSISTANT_INTAKE), concept explanation (P13) and the
 * persisted doubt-chat thread. Every reply is produced by the AI; Java only assembles the input,
 * records the audit job, routes the student to the right workflow and stores the conversation.
 */
public class AssistantService {

    private static final int MAX_MESSAGE_CHARS = 4000;
    private static final int MAX_HISTORY_MESSAGES = 8;

    private final LearningDao learning;
    private final ExamService exams;
    private final SyllabusService syllabus;
    private final AiServiceRunner ai;
    private final SimulatedFlags flags;

    public AssistantService(LearningDao learning, ExamService exams, SyllabusService syllabus,
                            AiServiceRunner ai, AiJobDao jobs) {
        this.learning = learning;
        this.exams = exams;
        this.syllabus = syllabus;
        this.ai = ai;
        this.flags = new SimulatedFlags(jobs);
    }

    public record AssistantReply(long conversationId, PlanDtos.AssistantIntakeResult intake,
                                 Optional<ConceptExplanation> explanation, String replyText,
                                 List<String> suggestedActions, boolean simulated) {
        public long getConversationId() { return conversationId; }
        public PlanDtos.AssistantIntakeResult getIntake() { return intake; }
        public Optional<ConceptExplanation> getExplanation() { return explanation; }
        public String getReplyText() { return replyText; }
        public List<String> getSuggestedActions() { return suggestedActions; }
        public boolean isSimulated() { return simulated; }
    }

    public AssistantReply chat(long userId, Long examId, String message, String depth) {
        String text = validateMessage(message);
        Exam exam = examId == null ? null : exams.find(userId, examId).orElse(null);
        Long conversationExamId = exam == null ? null : exam.getId();
        List<Topic> topics = exam == null ? List.of() : syllabus.confirmedTopics(userId, exam.getId());

        long conversationId;
        try {
            conversationId = learning.ensureConversation(userId, conversationExamId, titleFor(text));
            learning.insertMessage(conversationId, "USER", text, null, null);
        } catch (SQLException e) {
            throw new ServiceException("Could not start the conversation.", e);
        }

        ObjectNode input = AiInputs.object();
        input.put("message", text);
        input.put("hasSyllabus", !topics.isEmpty());
        if (exam != null) {
            ObjectNode context = AiInputs.object();
            context.put("subjectName", exam.getSubjectName());
            if (exam.getExamDatetime() != null) {
                context.put("examDateTime", exam.getExamDatetime().toString());
            }
            if (exam.getTargetMarks() != null) {
                context.put("targetMarks", exam.getTargetMarks());
            }
            input.set("examContext", context);
            ArrayNode topicTitles = Json.array();
            for (Topic t : topics) {
                topicTitles.add(t.getTitle());
            }
            input.set("knownTopics", topicTitles);
        }
        input.set("recentMessages", history(userId, conversationId));

        AiOutcome<PlanDtos.AssistantIntakeResult> intake = ai.run(
                AiAction.ASSISTANT_INTAKE, userId, examId, input, AiValidators.assistantIntake(), null);
        PlanDtos.AssistantIntakeResult result = intake.getData();

        Optional<ConceptExplanation> explanation = Optional.empty();
        if ("EXPLAIN_CONCEPT".equals(result.actionRequested)) {
            String topic = result.requestedTopic != null && !result.requestedTopic.isBlank()
                    ? result.requestedTopic : text;
            ExplanationOutcome eo = runExplanation(userId, examId, text, topic, depth);
            explanation = Optional.of(eo.explanation());
        }

        String replyText = compose(result, explanation);
        try {
            learning.insertMessage(conversationId, "ASSISTANT", replyText, result.actionRequested, intake.getJobId());
        } catch (SQLException e) {
            throw new ServiceException("The reply was generated but could not be stored.", e);
        }
        return new AssistantReply(conversationId, result, explanation, replyText,
                suggestedActions(result), intake.isSimulated());
    }

    public ConceptExplanation explain(long userId, Long examId, String question, String topic, String depth) {
        String safeQuestion = validateMessage(question);
        String safeTopic = topic == null || topic.isBlank() ? safeQuestion : Text.trimTo(topic, 200);
        return runExplanation(userId, examId, safeQuestion, safeTopic, depth).explanation();
    }

    public List<ConceptExplanation> explanations(long userId, Long examId, int limit) {
        try {
            List<ConceptExplanation> list = learning.explanations(userId, examId, limit <= 0 ? 50 : limit);
            var simulatedById = examId == null ? java.util.Map.<Long, Boolean>of() : flags.forExam(userId, examId);
            for (ConceptExplanation e : list) {
                e.setSimulated(Boolean.TRUE.equals(simulatedById.get(e.getAiJobId())));
            }
            return list;
        } catch (SQLException e) {
            throw new ServiceException("Could not load past explanations.", e);
        }
    }

    public List<Conversation> conversations(long userId) {
        try {
            return learning.conversations(userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load conversations.", e);
        }
    }

    public List<ConversationMessage> messages(long userId, long conversationId, int limit) {
        if (conversationId <= 0) {
            return List.of();
        }
        try {
            return learning.messages(conversationId, userId, limit <= 0 ? 200 : limit);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the conversation.", e);
        }
    }

    public void deleteConversation(long userId, long conversationId) {
        if (conversationId <= 0) {
            return;
        }
        try {
            learning.deleteConversation(conversationId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not delete the conversation.", e);
        }
    }

    // ------------------------------------------------------------------ internals

    private record ExplanationOutcome(ConceptExplanation explanation, boolean simulated) {
    }

    private ExplanationOutcome runExplanation(long userId, Long examId, String question, String topic, String depth) {
        ObjectNode input = AiInputs.object();
        input.put("topic", topic);
        input.put("question", question);
        input.put("depth", normaliseDepth(depth));
        if (examId != null) {
            Exam exam = exams.find(userId, examId).orElse(null);
            if (exam != null) {
                input.put("subjectName", exam.getSubjectName());
                input.set("syllabusTopics", AiInputs.topics(syllabus.confirmedTopics(userId, exam.getId())));
            }
        }

        AiOutcome<LearningDtos.ExplanationResult> outcome = ai.run(
                AiAction.P13_TOPIC_EXPLANATIONS, userId, examId, input,
                AiValidators.explanation(), null);

        LearningDtos.ExplanationResult result = outcome.getData();
        try {
            ConceptExplanation e = new ConceptExplanation();
            e.setExamId(examId);
            e.setUserId(userId);
            e.setAiJobId(outcome.getJobId());
            e.setTopicTitle(result.topic == null || result.topic.isBlank() ? topic : result.topic);
            e.setQuestion(question);
            e.setDirectAnswer(result.directAnswer);
            e.setExplanation(result.explanation);
            e.setExamplesJson(Json.write(result.examples));
            e.setCheckJson(Json.write(result.checkYourUnderstanding));
            e.setSourceReferences(Json.write(result.sourceReferences));
            e.setLimitations(Json.write(result.limitations));
            e.setSimulated(outcome.isSimulated());
            e.setId(learning.insertExplanation(e));
            return new ExplanationOutcome(e, outcome.isSimulated());
        } catch (SQLException e) {
            throw new ServiceException("The explanation could not be saved.", e);
        }
    }

    private ArrayNode history(long userId, long conversationId) {
        ArrayNode array = Json.array();
        List<ConversationMessage> messages;
        try {
            messages = learning.messages(conversationId, userId, 50);
        } catch (SQLException e) {
            return array;
        }
        int start = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
        for (int i = start; i < messages.size(); i++) {
            ConversationMessage m = messages.get(i);
            ObjectNode node = AiInputs.object();
            node.put("role", m.getRole());
            node.put("content", Text.trimTo(m.getContent(), 1000));
            array.add(node);
        }
        return array;
    }

    private String compose(PlanDtos.AssistantIntakeResult result, Optional<ConceptExplanation> explanation) {
        StringBuilder sb = new StringBuilder();
        if (explanation.isPresent()) {
            ConceptExplanation e = explanation.get();
            if (e.getDirectAnswer() != null && !e.getDirectAnswer().isBlank()) {
                sb.append(e.getDirectAnswer().trim());
            }
            if (e.getExplanation() != null && !e.getExplanation().isBlank()) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(e.getExplanation().trim());
            }
            List<String> examples = e.getExampleList();
            if (!examples.isEmpty()) {
                sb.append("\n\nExamples:\n");
                for (String ex : examples) {
                    sb.append("- ").append(ex).append('\n');
                }
            }
        } else {
            if (result.summary != null && !result.summary.isBlank()) {
                sb.append(result.summary.trim());
            }
            if (result.clarificationQuestion != null && !result.clarificationQuestion.isBlank()) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(result.clarificationQuestion.trim());
            }
        }
        if (result.nextActions != null && !result.nextActions.isEmpty()) {
            sb.append("\n\nNext steps:\n");
            for (String action : result.nextActions) {
                sb.append("- ").append(action).append('\n');
            }
        }
        return sb.toString().trim();
    }

    private List<String> suggestedActions(PlanDtos.AssistantIntakeResult result) {
        List<String> actions = new ArrayList<>();
        String key = result.actionRequested;
        if (key == null) {
            return actions;
        }
        switch (key) {
            case "CREATE_PLAN" -> actions.add("plan");
            case "REVISE_PLAN" -> actions.add("replan");
            case "EXPLAIN_CONCEPT" -> actions.add("explain");
            case "GENERATE_PRACTICE" -> actions.add("practice");
            case "GENERATE_REVISION" -> actions.add("revision");
            case "ASSESS_READINESS" -> actions.add("readiness");
            case "LAST_MINUTE_REVISION" -> actions.add("last-minute");
            case "ANALYSE_PYQ" -> actions.add("pyq");
            default -> {
                // UNKNOWN: no routing suggestion
            }
        }
        return actions;
    }

    private String validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new ServiceException("Type a question or request first.");
        }
        return Text.trimTo(message, MAX_MESSAGE_CHARS);
    }

    private static String titleFor(String message) {
        String t = message.strip().replaceAll("\\s+", " ");
        return t.length() <= 60 ? t : t.substring(0, 57) + "...";
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
}
