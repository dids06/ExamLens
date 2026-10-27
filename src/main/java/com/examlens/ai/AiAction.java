package com.examlens.ai;

/** The 18 required AI actions plus the assistant intake helper. */
public enum AiAction {
    P01_SYLLABUS_EXTRACTION("SYLLABUS_EXTRACTION", "1.1"),
    P02_TEACHER_INSTRUCTIONS("TEACHER_INSTRUCTIONS", "1.0"),
    P03_STUDY_MATERIAL_INDEXING("STUDY_MATERIAL_INDEXING", "1.0"),
    P04_SOURCE_COMPARISON("SOURCE_COMPARISON", "1.0"),
    P05_PYQ_ANALYSIS("PYQ_ANALYSIS", "1.0"),
    P06_STUDY_PLAN_GENERATION("STUDY_PLAN_GENERATION", "1.2"),
    P07_FEASIBILITY_ASSESSMENT("FEASIBILITY_ASSESSMENT", "1.0"),
    P08_ADAPTIVE_REPLANNING("ADAPTIVE_REPLANNING", "1.2"),
    P09_MOCK_PAPER_GENERATION("MOCK_PAPER_GENERATION", "1.1"),
    P10_PRACTICE_QUESTION_GENERATION("PRACTICE_QUESTION_GENERATION", "1.0"),
    P11_ANSWER_KEYS_AND_SOLUTIONS("ANSWER_KEYS_AND_SOLUTIONS", "1.0"),
    P12_SUBJECTIVE_ANSWER_EVALUATION("SUBJECTIVE_ANSWER_EVALUATION", "1.0"),
    P13_TOPIC_EXPLANATIONS("TOPIC_EXPLANATIONS", "1.1"),
    P14_REVISION_RESOURCES("REVISION_RESOURCES", "1.0"),
    P15_READINESS_ANALYSIS("READINESS_ANALYSIS", "1.0"),
    P16_STUDY_SESSION_FEEDBACK("STUDY_SESSION_FEEDBACK", "1.0"),
    P17_LAST_MINUTE_REVISION("LAST_MINUTE_REVISION", "1.0"),
    P18_PLAN_CHANGE_EXPLANATIONS("PLAN_CHANGE_EXPLANATIONS", "1.0"),
    ASSISTANT_INTAKE("ASSISTANT_INTAKE", "1.0");

    private final String actionKey;
    private final String promptVersion;

    AiAction(String actionKey, String promptVersion) {
        this.actionKey = actionKey;
        this.promptVersion = promptVersion;
    }

    public String actionKey() {
        return actionKey;
    }

    public String promptVersion() {
        return promptVersion;
    }

    public boolean isCoreAction() {
        return this != ASSISTANT_INTAKE;
    }

    public static AiAction fromKey(String key) {
        for (AiAction a : values()) {
            if (a.actionKey.equalsIgnoreCase(key)) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown AI action: " + key);
    }
}
