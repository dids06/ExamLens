package com.examlens.ai;

import com.examlens.config.AppConfig;
import com.examlens.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Groq provider (OpenAI-compatible Chat Completions). Requires GROQ_API_KEY.
 * Selected model defaults to llama-3.3-70b-versatile (verified production model with
 * 131k context and JSON object mode). No secrets are logged.
 */
public class GroqAiService implements AiService {

    private static final String BASE = "https://api.groq.com/openai/v1/chat/completions";

    private final AppConfig config;
    private final HttpClient http;

    public GroqAiService(AppConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String providerId() {
        return "groq";
    }

    @Override
    public String modelId() {
        return config.groqModel();
    }

    @Override
    public boolean isConfigured() {
        return !config.groqApiKey().isBlank();
    }

    @Override
    public String unavailabilityMessage() {
        return AiFailureType.MISSING_API_KEY.userMessage();
    }

    @Override
    public AiResponse generate(AiRequest request) throws AiException {
        String apiKey = config.groqApiKey();
        if (apiKey.isBlank()) {
            throw new AiException(AiFailureType.MISSING_API_KEY, "GROQ_API_KEY is not set");
        }
        String body = buildBody(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .timeout(Duration.ofSeconds(config.aiTimeoutSeconds()))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, java.nio.charset.StandardCharsets.UTF_8))
                .build();
        long start = System.currentTimeMillis();
        HttpResponse<String> response;
        try {
            response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.net.http.HttpTimeoutException e) {
            throw new AiException(AiFailureType.TIMEOUT, "request timeout after " + config.aiTimeoutSeconds() + "s", e);
        } catch (IOException e) {
            throw new AiException(AiFailureType.NETWORK_ERROR, "network failure: " + e.getClass().getSimpleName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException(AiFailureType.TIMEOUT, "request interrupted", e);
        }
        long latency = System.currentTimeMillis() - start;

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            AiFailureType type = AiFailureType.fromHttpStatus(response.statusCode(), response.body());
            throw new AiException(type, "HTTP " + response.statusCode() + ": " + sanitize(response.body()));
        }
        JsonNode root;
        try {
            root = Json.read(response.body());
        } catch (RuntimeException e) {
            throw new AiException(AiFailureType.PROVIDER_ERROR, "provider returned non-JSON envelope", e);
        }
        String text = extractText(root);
        if (text == null || text.isBlank()) {
            throw new AiException(AiFailureType.PROVIDER_ERROR, "empty candidate text");
        }
        Integer inTokens = intOrNull(root.path("usage").get("prompt_tokens"));
        Integer outTokens = intOrNull(root.path("usage").get("completion_tokens"));
        return new AiResponse(text, providerId(), modelId(), latency, inTokens, outTokens);
    }

    private Integer intOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private String extractText(JsonNode root) {
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return null;
        }
        JsonNode message = choices.get(0).path("message");
        if (message.isMissingNode()) {
            return null;
        }
        if (message.has("content") && !message.get("content").isNull()) {
            return message.get("content").asText();
        }
        return null;
    }

    /** Truncates and lightly redacts a provider error body so it is safe to persist/log. */
    static String sanitize(String body) {
        if (body == null || body.isBlank()) {
            return "(empty body)";
        }
        String trimmed = body.replaceAll("\\s+", " ").trim();
        trimmed = trimmed.replaceAll("(?i)(key=|bearer\\s+)[^\\s\"']+", "$1[redacted]");
        if (trimmed.length() > 400) {
            trimmed = trimmed.substring(0, 400) + "...(truncated)";
        }
        return trimmed;
    }

    String buildBody(AiRequest request) {
        ObjectNode root = Json.object();
        root.put("model", config.groqModel());

        ArrayNode messages = Json.array();
        ObjectNode systemMsg = Json.object();
        systemMsg.put("role", "system");
        systemMsg.put("content", request.getSystemPrompt());
        messages.add(systemMsg);

        ObjectNode userMsg = Json.object();
        userMsg.put("role", "user");
        userMsg.put("content", request.getUserPrompt());
        messages.add(userMsg);
        root.set("messages", messages);

        root.put("temperature", request.getTemperature());
        root.put("max_tokens", request.getMaxOutputTokens());
        // Groq supports OpenAI-style JSON object mode; server-side validation still enforces schema.
        ObjectNode responseFormat = Json.object();
        responseFormat.put("type", "json_object");
        root.set("response_format", responseFormat);

        return Json.write(root);
    }
}
