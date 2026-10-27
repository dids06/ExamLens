package com.examlens.ai.dto;

import java.util.ArrayList;
import java.util.List;

/** Typed results for practice and evaluation AI actions (P09-P12). */
public final class PracticeDtos {

    private PracticeDtos() {
    }

    public static class GeneratedQuestion {
        public String clientKey;
        public String type;
        public String topic;
        public String topicId;
        public String prompt;
        public List<String> options = new ArrayList<>();
        public String correctOption;
        public Integer marks;
        public String difficulty;
        public String modelAnswer;
        public List<Rubric> rubric = new ArrayList<>();
        public Integer expectedMinutes;
    }

    public static class Rubric {
        public String criterion;
        public Integer marks;
    }

    public static class MockPaperResult {
        public String title;
        public String instructions;
        public Integer declaredTotalMarks;
        public Integer durationMinutes;
        public List<GeneratedQuestion> questions = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> assumptions = new ArrayList<>();
    }

    public static class PracticeQuestionResult {
        public String summary;
        public List<GeneratedQuestion> questions = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
    }

    public static class AnswerKeyResult {
        public String summary;
        public List<Solution> solutions = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public static class Solution {
            public String questionId;
            public String modelAnswer;
            public List<String> steps = new ArrayList<>();
            public List<Rubric> rubric = new ArrayList<>();
            public Integer totalRubricMarks;
            public String uncertainty;
        }
    }

    public static class EvaluationResult {
        public String questionId;
        public Double awardedMarks;
        public Double maximumMarks;
        public List<CriterionResult> criterionResults = new ArrayList<>();
        public String feedback;
        public List<String> strengths = new ArrayList<>();
        public List<String> improvements = new ArrayList<>();
        public String confidence;
        public Boolean requiresManualReview;

        public static class CriterionResult {
            public String criterion;
            public Double awardedMarks;
            public Double maximumMarks;
            public String reason;
        }
    }
}
