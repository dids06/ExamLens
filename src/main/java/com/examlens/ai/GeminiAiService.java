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
 * Live Gemini provider. Requires GEMINI_API_KEY. Uses JSON response mode so the model
 * returns structured output; malformed output is still validated by AIServiceRunner.
 */
public class GeminiAiService implements AiService {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final AppConfig config;
    private final HttpClient http;

    public GeminiAiService(AppConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String providerId() {
        return "gemini";
    }

    @Override
    public String modelId() {
        return config.geminiModel();
    }

    @Override
    public boolean isConfigured() {
        return !config.geminiApiKey().isBlank();
    }

    @Override
    public String unavailabilityMessage() {
        return AiFailureType.MISSING_API_KEY.userMessage();
    }

    @Override
    public AiResponse generate(AiRequest request) throws AiException {
        String apiKey = config.geminiApiKey();
        if (apiKey.isBlank()) {
            throw new AiException(AiFailureType.MISSING_API_KEY, "GEMINI_API_KEY is not set");
        }
        String body = buildBody(request);
        String url = BASE + config.geminiModel() + ":generateContent?key=" + apiKey;
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(config.aiTimeoutSeconds()))
                .header("Content-Type", "application/json; charset=UTF-8")
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
            String diagnostic = "HTTP " + response.statusCode() + ": " + sanitize(response.body());
            throw new AiException(type, diagnostic);
        }
        JsonNode root;
        try {
            root = Json.read(response.body());
        } catch (RuntimeException e) {
            throw new AiException(AiFailureType.PROVIDER_ERROR, "provider returned non-JSON envelope", e);
        }
        String text = extractText(root);
        if (text == null || text.isBlank()) {
            String blockReason = root.path("promptFeedback").path("blockReason").asText("");
            if (!blockReason.isBlank()) {
                throw new AiException(AiFailureType.REFUSED, "blocked: " + blockReason);
            }
            throw new AiException(AiFailureType.PROVIDER_ERROR, "empty candidate text");
        }
        Integer inTokens = intOrNull(root.path("usageMetadata").get("promptTokenCount"));
        Integer outTokens = intOrNull(root.path("usageMetadata").get("candidatesTokenCount"));
        return new AiResponse(text, providerId(), modelId(), latency, inTokens, outTokens);
    }

    private Integer intOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private String extractText(JsonNode root) {
        JsonNode candidates = root.get("candidates");
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            return null;
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                sb.append(part.get("text").asText());
            }
        }
        return sb.toString();
    }

    /** Truncates and lightly redacts a provider error body so it is safe to persist/log. */
    static String sanitize(String body) {
        if (body == null || body.isBlank()) {
            return "(empty body)";
        }
        String trimmed = body.replaceAll("\\s+", " ").trim();
        // Redact anything that looks like a key=... query parameter value, defensively.
        trimmed = trimmed.replaceAll("(?i)(key=)[^\\s\"']+", "$1[redacted]");
        if (trimmed.length() > 400) {
            trimmed = trimmed.substring(0, 400) + "...(truncated)";
        }
        return trimmed;
    }

    String buildBody(AiRequest request) {
        ObjectNode root = Json.object();

        ObjectNode system = Json.object();
        ArrayNode systemParts = Json.array();
        systemParts.add(Json.object().put("text", request.getSystemPrompt()));
        system.set("parts", systemParts);
        root.set("systemInstruction", system);

        ArrayNode contents = Json.array();
        ObjectNode userContent = Json.object();
        userContent.put("role", "user");
        ArrayNode parts = Json.array();
        ObjectNode textPart = Json.object();
        textPart.put("text", request.getUserPrompt());
        parts.add(textPart);
        for (AiRequest.ImagePart image : request.getImages()) {
            ObjectNode inline = Json.object();
            inline.put("mimeType", image.mimeType());
            inline.put("data", image.base64Data());
            ObjectNode part = Json.object();
            part.set("inlineData", inline);
            parts.add(part);
        }
        userContent.set("parts", parts);
        contents.add(userContent);
        root.set("contents", contents);

        ObjectNode generationConfig = Json.object();
        generationConfig.put("temperature", request.getTemperature());
        generationConfig.put("maxOutputTokens", request.getMaxOutputTokens());
        generationConfig.put("responseMimeType", "application/json");
        if (request.getJsonSchema() != null && !request.getJsonSchema().isBlank()) {
            try {
                generationConfig.set("responseSchema", Json.read(request.getJsonSchema()));
            } catch (RuntimeException ignored) {
                // Schema is advisory; validation still happens server-side.
            }
        }
        root.set("generationConfig", generationConfig);

        return Json.write(root);
    }
}
