package com.examlens.ai.dto;

import java.util.ArrayList;
import java.util.List;

/** Typed results for learning and analytics AI actions (P13-P16). */
public final class LearningDtos {

    private LearningDtos() {
    }

    public static class ExplanationResult {
        public String topic;
        public String directAnswer;
        public String explanation;
        public List<String> examples = new ArrayList<>();
        public List<String> checkYourUnderstanding = new ArrayList<>();
        public List<String> sourceReferences = new ArrayList<>();
        public List<String> limitations = new ArrayList<>();
        public String depth;
    }

    public static class RevisionResourcesResult {
        public String summary;
        public List<TopicResource> topicResources = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public static class TopicResource {
            public String topic;
            public String topicId;
            public List<String> keyPoints = new ArrayList<>();
            public List<Flashcard> flashcards = new ArrayList<>();
            public List<Recall> activeRecallQuestions = new ArrayList<>();
            public List<String> sourceReferences = new ArrayList<>();
        }

        public static class Flashcard {
            public String front;
            public String back;
        }

        public static class Recall {
            public String question;
            public String answer;
        }
    }

    public static class ReadinessResult {
        public String readinessSummary;
        public List<WeakTopic> weakTopics = new ArrayList<>();
        public List<String> strengths = new ArrayList<>();
        public List<String> coverageGaps = new ArrayList<>();
        public List<String> dataLimitations = new ArrayList<>();
        public List<String> nextActions = new ArrayList<>();

        public static class WeakTopic {
            public String topic;
            public String topicId;
            public List<String> evidence = new ArrayList<>();
            public String recommendedNextAction;
            public String confidence;
        }
    }

    public static class SessionFeedbackResult {
        public String summary;
        public String observation;
        public List<String> suggestions = new ArrayList<>();
        public String nextStep;
        public List<String> limitations = new ArrayList<>();
    }
}
