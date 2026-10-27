package com.examlens.ai.prompt;

import com.examlens.ai.AiAction;
import com.examlens.ai.AiRequest;
import com.examlens.util.Json;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptLibraryTest {

    @Test
    void everyActionHasAVersionedSystemPromptContainingTheSecurityRule() {
        for (AiAction action : AiAction.values()) {
            String system = PromptLibrary.systemPrompt(action);
            assertNotNull(system, action + " must have a system prompt");
            assertFalseBlank(system, action + " system prompt");
            assertTrue(system.contains("UNTRUSTED DATA"),
                    action + " system prompt must contain the prompt-injection defence");
            assertTrue(system.contains("Return ONLY a single JSON object"),
                    action + " system prompt must demand JSON-only output");
            assertFalseBlank(action.promptVersion(), action + " prompt version");
        }
    }

    @Test
    void userPromptEmbedsTheInputAndTheRequiredShape() {
        var input = Json.object();
        input.put("subjectName", "Computer Science");
        for (AiAction action : AiAction.values()) {
            String prompt = PromptLibrary.userPrompt(action, input);
            assertTrue(prompt.contains("INPUT DATA (JSON)"), action + " must label the input section");
            assertTrue(prompt.contains("Computer Science"), action + " must embed the supplied input");
            assertTrue(prompt.contains("REQUIRED OUTPUT"), action + " must include the expected output shape");
        }
    }

    @Test
    void buildProducesAUsableRequestForEveryAction() {
        var input = Json.object().put("sample", "value");
        for (AiAction action : AiAction.values()) {
            AiRequest request = PromptLibrary.build(action, input, 2048);
            assertNotNull(request.getSystemPrompt());
            assertFalseBlank(request.getUserPrompt(), action + " user prompt");
            assertFalseBlank(request.getInputJson(), action + " input json");
            assertTrue(request.getMaxOutputTokens() >= 1024);
            assertTrue(request.getTemperature() > 0);
        }
    }

    @Test
    void correctionRequestIncludesTheValidationErrors() {
        var input = Json.object().put("sample", "value");
        AiRequest correction = PromptLibrary.buildCorrection(
                AiAction.P06_STUDY_PLAN_GENERATION, input, "{\"bad\":true}",
                List.of("summary is required", "tasks must contain at least one scheduled task"));
        assertTrue(correction.getUserPrompt().contains("summary is required"));
        assertTrue(correction.getUserPrompt().contains("tasks must contain at least one scheduled task"));
        assertTrue(correction.getUserPrompt().contains("REJECTED BY SERVER-SIDE VALIDATION"));
        assertNotNull(correction.getCorrectionInstruction());
    }

    @Test
    void knownActionsCarryAStructuredOutputSchema() {
        assertNotNull(PromptLibrary.jsonSchema(AiAction.P01_SYLLABUS_EXTRACTION));
        assertNotNull(PromptLibrary.jsonSchema(AiAction.P06_STUDY_PLAN_GENERATION));
        assertNotNull(PromptLibrary.jsonSchema(AiAction.ASSISTANT_INTAKE));
    }

    @Test
    void unknownActionKeysAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> AiAction.fromKey("NOT_AN_ACTION"));
    }

    @Test
    void coreActionsCoverAllEighteenRequiredActions() {
        long core = java.util.Arrays.stream(AiAction.values()).filter(AiAction::isCoreAction).count();
        org.junit.jupiter.api.Assertions.assertEquals(18, core,
                "the specification requires exactly 18 core AI actions (P01-P18)");
    }

    private static void assertFalseBlank(String value, String label) {
        assertTrue(value != null && !value.isBlank(), label + " must not be blank");
    }
}
