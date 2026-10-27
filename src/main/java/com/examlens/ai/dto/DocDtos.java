package com.examlens.ai.dto;

import java.util.ArrayList;
import java.util.List;

/** Typed results for document and syllabus AI actions (P01-P05). */
public final class DocDtos {

    private DocDtos() {
    }

    public static class SyllabusExtractionResult {
        public String summary;
        public List<Unit> units = new ArrayList<>();
        public List<Classification> classifications = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> assumptions = new ArrayList<>();
        public List<String> nextActions = new ArrayList<>();

        public static class Unit {
            public String unitName;
            public String unitRationale;
            public List<TopicDraft> topics = new ArrayList<>();
        }

        public static class TopicDraft {
            public String title;
            public List<String> subtopics = new ArrayList<>();
            public List<String> learningOutcomes = new ArrayList<>();
            public Integer estimatedMinutes;
            public String importance;
            public String difficulty;
            public List<Integer> sourcePages = new ArrayList<>();
            public String confidence;
            public Boolean needsReview;
            public String reason;
        }

        public static class Classification {
            public String sourceBlockId;
            public Integer page;
            public String originalText;
            public String classification;
            public Double confidence;
            public String normalizedTopic;
            public String parentUnit;
            public String reason;
            public Boolean includeInSyllabus;
        }
    }

    public static class TeacherInstructionsResult {
        public String summary;
        public List<Hint> hints = new ArrayList<>();
        public List<ImportantTopic> importantTopics = new ArrayList<>();
        public List<String> instructions = new ArrayList<>();
        public List<String> examFormatDetails = new ArrayList<>();
        public List<String> uncertainties = new ArrayList<>();

        public static class Hint {
            public String text;
            public String evidenceQuote;
            public String sourceReference;
            public String confidence;
        }

        public static class ImportantTopic {
            public String topic;
            public String priority;
            public String evidenceQuote;
            public String sourceReference;
        }
    }

    public static class MaterialIndexResult {
        public String summary;
        public List<Section> sections = new ArrayList<>();
        public List<String> unmappedTopics = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public static class Section {
            public String title;
            public Integer startPage;
            public Integer endPage;
            public List<TopicCoverage> topics = new ArrayList<>();
        }

        public static class TopicCoverage {
            public String topic;
            public String coverage;
            public String evidence;
        }
    }

    public static class SourceComparisonResult {
        public String summary;
        public List<Recommendation> recommendations = new ArrayList<>();
        public List<String> uncoveredTopics = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public static class Recommendation {
            public String topic;
            public String primarySourceId;
            public String primarySourceName;
            public List<String> pageReferences = new ArrayList<>();
            public List<String> otherSources = new ArrayList<>();
            public String rationale;
            public String confidence;
        }
    }

    public static class PyqAnalysisResult {
        public String summary;
        public List<Question> questions = new ArrayList<>();
        public List<TopicFrequency> topicFrequency = new ArrayList<>();
        public List<String> recurringPatterns = new ArrayList<>();
        public List<String> unmatchedQuestions = new ArrayList<>();
        public List<String> limitations = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public static class Question {
            public String questionKey;
            public String text;
            public Integer marks;
            public Integer page;
            public String year;
            public String paperId;
            public String topic;
        }

        public static class TopicFrequency {
            public String topic;
            public Integer observedCount;
            public List<String> paperIds = new ArrayList<>();
            public List<String> years = new ArrayList<>();
            public Integer observedMarks;
            public String confidence;
            public List<String> evidenceQuestionIds = new ArrayList<>();
        }
    }
}
