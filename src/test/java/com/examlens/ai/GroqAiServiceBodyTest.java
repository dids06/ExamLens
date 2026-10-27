package com.examlens.ai;

import com.examlens.config.AppConfig;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GroqAiServiceBodyTest {

    private AppConfig cfg() {
        return AppConfig.of(Map.of(
                "GROQ_API_KEY", "test-key-not-real",
                "GROQ_MODEL", "llama-3.3-70b-versatile",
                "AI_PROVIDER", "groq"));
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
    void buildBodyIsValidChatCompletionsRequest() {
        GroqAiService service = new GroqAiService(cfg());
        String body = service.buildBody(simpleRequest());
        JsonNode root = Json.read(body);
        assertEquals("llama-3.3-70b-versatile", root.get("model").asText());
        assertTrue(root.get("messages").isArray());
        assertEquals(2, root.get("messages").size());
        assertEquals("system", root.get("messages").get(0).get("role").asText());
        assertEquals("user", root.get("messages").get(1).get("role").asText());
        assertEquals("json_object", root.get("response_format").get("type").asText());
        assertEquals("groq", service.providerId());
        assertTrue(service.isConfigured());
    }

    @Test
    void buildBodyWithSchemaDoesNotCrash() {
        GroqAiService service = new GroqAiService(cfg());
        AiRequest req = AiRequest.builder(AiAction.P13_TOPIC_EXPLANATIONS)
                .systemPrompt("sys").userPrompt("user")
                .jsonSchema("{\"type\":\"object\",\"properties\":{\"x\":{\"type\":\"string\"}}}")
                .build();
        String body = service.buildBody(req);
        JsonNode root = Json.read(body);
        assertNotNull(root.get("messages"));
    }

    @Test
    void modelIdComesFromConfig() {
        GroqAiService service = new GroqAiService(cfg());
        assertEquals("llama-3.3-70b-versatile", service.modelId());
    }

    @Test
    void unconfiguredServiceReportsUnavailable() {
        AppConfig uncfg = AppConfig.of(Map.of("AI_PROVIDER", "groq"));
        GroqAiService service = new GroqAiService(uncfg);
        assertFalse(service.isConfigured());
    }

    @Test
    void sanitizeTruncatesAndRedacts() {
        String longBody = "x".repeat(600) + " bearer SECRET123";
        String out = GroqAiService.sanitize(longBody);
        assertTrue(out.length() <= 420);
        assertTrue(out.endsWith("...(truncated)"));
        assertFalse(out.contains("SECRET123"));
    }

    @Test
    void sanitizeHandlesEmptyBody() {
        assertEquals("(empty body)", GroqAiService.sanitize(null));
        assertEquals("(empty body)", GroqAiService.sanitize("   "));
    }
}
