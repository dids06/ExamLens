package com.examlens.ai.dto;

import java.util.ArrayList;
import java.util.List;

/** Typed results for planning AI actions (P06, P07, P08, P17, P18). */
public final class PlanDtos {

    private PlanDtos() {
    }

    public static class StudyPlanResult {
        public String summary;
        public String feasibility;
        public String feasibilityNote;
        public Integer totalEstimatedMinutes;
        public List<TopicPriority> topics = new ArrayList<>();
        public List<PlannedTask> tasks = new ArrayList<>();
        public Integer revisionMinutes;
        public Integer practiceMinutes;
        public List<String> assumptions = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> risks = new ArrayList<>();
        public List<String> tradeoffs = new ArrayList<>();
        public List<String> unscheduled = new ArrayList<>();
        public List<String> nextActions = new ArrayList<>();

        public static class TopicPriority {
            public String topicId;
            public String topicTitle;
            public Integer priorityRank;
            public String priority;
            public Integer recommendedMinutes;
            public String reason;
            public List<String> sourceReferences = new ArrayList<>();
        }

        public static class PlannedTask {
            public String taskKey;
            public String existingTaskKey;
            public String topicId;
            public String topicTitle;
            public String taskType;
            public String title;
            public String objective;
            public String studyMethod;
            public Integer estimatedMinutes;
            public String date;
            public Integer order;
            public String priority;
            public String rationale;
            public List<String> prerequisites = new ArrayList<>();
            public List<String> learningObjectives = new ArrayList<>();
            public List<String> activities = new ArrayList<>();
            public String revisionOfTaskKey;
        }
    }

    public static class FeasibilityResult {
        public String notes;
        public Integer availableMinutes;
        public Integer estimatedRequiredMinutes;
        public Integer bufferMinutes;
        public String feasibility;
        public List<String> highestPriorityTradeoffs = new ArrayList<>();
        public List<String> assumptions = new ArrayList<>();
    }

    public static class ReplanResult {
        public String summary;
        public String feasibility;
        public String capacityRisk;
        public Integer totalEstimatedMinutes;
        public List<StudyPlanResult.PlannedTask> tasks = new ArrayList<>();
        public List<String> preservedTaskKeys = new ArrayList<>();
        public List<String> removedTaskKeys = new ArrayList<>();
        public String changeSummary;
        public List<String> assumptions = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> nextActions = new ArrayList<>();
    }

    public static class LastMinuteRevisionResult {
        public String summary;
        public List<PriorityBlock> priorityBlocks = new ArrayList<>();
        public List<String> deferredItems = new ArrayList<>();
        public String feasibility;
        public List<String> tradeoffs = new ArrayList<>();
        public List<String> assumptions = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public static class PriorityBlock {
            public String topic;
            public String topicId;
            public Integer recommendedMinutes;
            public String objective;
            public String activity;
            public List<String> sourceReferences = new ArrayList<>();
        }
    }

    public static class PlanChangeExplanationResult {
        public String summary;
        public List<Change> changes = new ArrayList<>();
        public List<String> unchangedPriorities = new ArrayList<>();
        public List<String> studentActionItems = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public static class Change {
            public String change;
            public String evidence;
            public String reason;
        }
    }

    public static class AssistantIntakeResult {
        public String intent;
        public String actionRequested;
        public String summary;
        public String subjectName;
        public String examDateTime;
        public Integer weekdayMinutes;
        public Integer weekendMinutes;
        public List<String> strengths = new ArrayList<>();
        public List<String> difficulties = new ArrayList<>();
        public String requestedTopic;
        public Integer targetMarks;
        public List<String> constraints = new ArrayList<>();
        public List<String> missingInformation = new ArrayList<>();
        public String clarificationQuestion;
        public List<String> referencedMaterials = new ArrayList<>();
        public List<String> nextActions = new ArrayList<>();
    }
}
