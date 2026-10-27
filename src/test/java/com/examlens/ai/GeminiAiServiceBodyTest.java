package com.examlens.ai;

import com.examlens.config.AppConfig;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the Gemini request body shape and provider-error diagnostics.
 * No network call is made; only the locally built request JSON is inspected.
 */
class GeminiAiServiceBodyTest {

    private AppConfig cfg() {
        return AppConfig.of(Map.of(
                "GEMINI_API_KEY", "test-key-not-real",
                "GEMINI_MODEL", "gemini-2.5-flash",
                "AI_PROVIDER", "gemini"));
    }

    private AiRequest simpleRequest() {
        return AiRequest.builder(AiAction.P13_TOPIC_EXPLANATIONS)
                .systemPrompt("You are a helpful tutor.")
                .userPrompt("Explain activation energy.")
                .temperature(0.3)
                .maxOutputTokens(256)
                .build();
    }

    @Test
    void buildBodyOmitsSafetySettingsOrUsesArray() {
        GeminiAiService service = new GeminiAiService(cfg());
        String body = service.buildBody(simpleRequest());
        JsonNode root = Json.read(body);
        if (root.has("safetySettings")) {
            assertTrue(root.get("safetySettings").isArray(),
                    "safetySettings must be an array, never an object: " + body);
        }
    }

    @Test
    void buildBodyIsValidStructuredRequest() {
        GeminiAiService service = new GeminiAiService(cfg());
        String body = service.buildBody(simpleRequest());
        JsonNode root = Json.read(body);
        assertTrue(root.has("contents"), "contents is required");
        assertTrue(root.has("generationConfig"), "generationConfig is required");
        assertEquals("application/json",
                root.get("generationConfig").get("responseMimeType").asText());
        assertEquals("gemini-2.5-flash", service.modelId());
    }

    @Test
    void buildBodyWithSchemaEmbedsItAsJson() {
        GeminiAiService service = new GeminiAiService(cfg());
        AiRequest req = AiRequest.builder(AiAction.P13_TOPIC_EXPLANATIONS)
                .systemPrompt("sys").userPrompt("user")
                .jsonSchema("{\"type\":\"object\",\"properties\":{\"x\":{\"type\":\"string\"}}}")
                .build();
        String body = service.buildBody(req);
        JsonNode root = Json.read(body);
        JsonNode schema = root.get("generationConfig").get("responseSchema");
        assertNotNull(schema, "responseSchema should be embedded when provided");
        assertEquals("object", schema.get("type").asText());
    }

    @Test
    void sanitizeTruncatesAndRedacts() {
        String longBody = "x".repeat(600) + " key=SECRET123";
        String out = GeminiAiService.sanitize(longBody);
        assertTrue(out.length() <= 420, "sanitized output must be bounded");
        assertTrue(out.endsWith("...(truncated)"), "long bodies must be truncated");
        assertFalse(out.contains("SECRET123"), "key values must be redacted");
    }

    @Test
    void sanitizeHandlesEmptyBody() {
        assertEquals("(empty body)", GeminiAiService.sanitize(null));
        assertEquals("(empty body)", GeminiAiService.sanitize("   "));
    }
}
