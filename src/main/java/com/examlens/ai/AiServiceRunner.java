package com.examlens.ai;

import com.examlens.config.AppConfig;
import com.examlens.dao.AiJobDao;
import com.examlens.util.Json;
import com.examlens.ai.prompt.PromptLibrary;
import com.fasterxml.jackson.databind.JsonNode;

import java.security.MessageDigest;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiServiceRunner {
    private static final int MAX_TRANSIENT_ATTEMPTS = 2;
    private static final int CACHE_MAX_ENTRIES = 256;

    private final AiService service;
    private final AppConfig config;
    private final AiJobDao jobDao;

    private final Map<String, CachedResponse> cache = java.util.Collections.synchronizedMap(
            new LinkedHashMap<String, CachedResponse>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CachedResponse> eldest) {
                    return size() > CACHE_MAX_ENTRIES;
                }
            });

    public AiServiceRunner(AiService service, AppConfig config, AiJobDao jobDao) {
        this.service = service;
        this.config = config;
        this.jobDao = jobDao;
    }

    public AiService service() { return service; }
    public boolean isAiAvailable() { return service.isConfigured(); }
    public String unavailabilityMessage() { return service.isConfigured() ? null : service.unavailabilityMessage(); }
    public boolean isSimulated() { return service.providerId().toLowerCase().contains("simulated"); }

    public <T> AiOutcome<T> run(AiAction action, long userId, Long examId, JsonNode input,
                                AiValidator<T> validator, java.util.function.Function<T, List<String>> domainCheck) {
        if (!service.isConfigured()) {
            throw new AiException(AiFailureType.MISSING_API_KEY, service.unavailabilityMessage());
        }
        checkLocalRateLimit(userId, action);

        int maxTokens = Math.min(config.aiMaxOutputTokens(), PromptLibrary.recommendedMaxOutputTokens(action));
        String cacheKey = cacheKey(userId, action, input, maxTokens);

        CachedResponse cached = cache.get(cacheKey);
        if (cached != null) {
            T cachedValue;
            try {
                cachedValue = parseAndValidate(cached.raw, validator, domainCheck);
            } catch (RuntimeException e) {
                cache.remove(cacheKey);
                cachedValue = null;
            }
            if (cachedValue != null) {
                long jobId;
                try {
                    jobId = jobDao.create(userId, examId, action.actionKey(), action.promptVersion(),
                            service.providerId(), service.modelId(), cached.raw.length());
                } catch (SQLException e) {
                    throw new IllegalStateException("Cannot record AI job", e);
                }
                try {
                    jobDao.markSucceeded(jobId, cached.raw, 0, 0, 0);
                } catch (SQLException e) {
                    System.err.println("[ExamLens] Could not record cached AI job success: " + e.getMessage());
                }
                logUsage(action, service.modelId(), cached.inputTokens, cached.outputTokens, true, 200, null);
                return new AiOutcome<>(cachedValue, jobId, service.providerId(), service.modelId(),
                        action.promptVersion(), isSimulated(), 0, 0, List.of(), true);
            }
        }

        long start = System.currentTimeMillis();
        String payload = Json.write(input);
        long jobId;
        try {
            jobId = jobDao.create(userId, examId, action.actionKey(), action.promptVersion(),
                    service.providerId(), service.modelId(), payload.length());
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot record AI job", e);
        }

        AiRequest request = PromptLibrary.build(action, input, maxTokens);
        int attempts = 0;
        int corrections = 0;
        String lastRaw = null;
        List<String> lastErrors = new ArrayList<>();
        boolean[] failureRecorded = {false};

        try {
            AiResponse response = null;
            while (response == null) {
                attempts++;
                try {
                    response = service.generate(request);
                    lastRaw = response.getRawText();
                } catch (AiException e) {
                    boolean transientFailure = e.getFailureType().isRetryable()
                            && !(e.getFailureType() == AiFailureType.PROVIDER_ERROR
                                    && e.getDiagnostic() != null
                                    && e.getDiagnostic().startsWith("HTTP 4"));
                    if (transientFailure && attempts < MAX_TRANSIENT_ATTEMPTS) {
                        backoff(attempts);
                        continue;
                    }
                    throw e;
                }
            }

            try {
                T value = parseAndValidate(lastRaw, validator, domainCheck);
                long duration = System.currentTimeMillis() - start;
                completeJob(jobId, lastRaw, attempts, corrections, duration);
                cache.put(cacheKey, new CachedResponse(lastRaw, response.getInputTokens(), response.getOutputTokens()));
                logUsage(action, service.modelId(), response.getInputTokens(), response.getOutputTokens(), false, 200, null);
                return new AiOutcome<>(value, jobId, service.providerId(), service.modelId(),
                        action.promptVersion(), isSimulated(), corrections, duration, List.of(), false);
            } catch (AiValidationException | IllegalArgumentException first) {
                lastErrors = first instanceof AiValidationException ve
                        ? ve.getErrors()
                        : List.of(first.getMessage() == null ? "Invalid AI JSON output" : first.getMessage());
            }

            corrections = 1;
            attempts++;
            AiRequest correction = PromptLibrary.buildCorrection(action, input, lastRaw, lastErrors);
            AiResponse corrected;
            try {
                corrected = service.generate(correction);
            } catch (AiException e) {
                AiFailureType type = e.getFailureType();
                recordFailure(failureRecorded, jobId, type, e.getDiagnostic(), attempts, corrections, start);
                logUsage(action, service.modelId(), null, null, false, statusFromDiagnostic(e.getDiagnostic()), type.code());
                throw e;
            }
            lastRaw = corrected.getRawText();
            try {
                T value = parseAndValidate(lastRaw, validator, domainCheck);
                long duration = System.currentTimeMillis() - start;
                completeJob(jobId, lastRaw, attempts, corrections, duration);
                cache.put(cacheKey, new CachedResponse(lastRaw, corrected.getInputTokens(), corrected.getOutputTokens()));
                logUsage(action, service.modelId(), corrected.getInputTokens(), corrected.getOutputTokens(), false, 200, null);
                return new AiOutcome<>(value, jobId, service.providerId(), service.modelId(),
                        action.promptVersion(), isSimulated(), corrections, duration, List.of(), false);
            } catch (AiValidationException | IllegalArgumentException e) {
                AiFailureType type = e instanceof AiValidationException
                        ? AiFailureType.SCHEMA_INVALID : AiFailureType.INVALID_JSON;
                String detail = lastErrors.isEmpty() ? e.getMessage() : String.join(" | ", lastErrors);
                recordFailure(failureRecorded, jobId, type, detail, attempts, corrections, start);
                logUsage(action, service.modelId(), null, null, false, 200, type.code());
                throw new AiException(type, detail);
            }
        } catch (AiException e) {
            System.err.println("[ExamLens] AI request failed: type=" + e.getFailureType().code()
                    + " diagnostic=" + e.getDiagnostic());
            if (!failureRecorded[0]) {
                recordFailure(failureRecorded, jobId, e.getFailureType(), e.getDiagnostic(), attempts, corrections, start);
            }
            logUsage(action, service.modelId(), null, null, false, statusFromDiagnostic(e.getDiagnostic()), e.getFailureType().code());
            throw e;
        } catch (RuntimeException e) {
            if (!failureRecorded[0]) {
                recordFailure(failureRecorded, jobId, AiFailureType.UNKNOWN,
                        e.getClass().getSimpleName() + ": " + e.getMessage(), attempts, corrections, start);
            }
            logUsage(action, service.modelId(), null, null, false, 0, AiFailureType.UNKNOWN.code());
            throw new AiException(AiFailureType.UNKNOWN, e.getMessage(), e);
        }
    }

    private static int statusFromDiagnostic(String diagnostic) {
        if (diagnostic == null) return 0;
        if (diagnostic.startsWith("HTTP ")) {
            try {
                return Integer.parseInt(diagnostic.substring(5).split(":")[0].trim());
            } catch (NumberFormatException ignored) { }
        }
        return 0;
    }

    private void logUsage(AiAction action, String model, Integer inputTokens, Integer outputTokens,
                          boolean cacheHit, int httpStatus, String errorCategory) {
        StringBuilder sb = new StringBuilder("[ExamLens] AI usage: action=").append(action.actionKey())
                .append(" model=").append(model)
                .append(" cacheHit=").append(cacheHit)
                .append(" inputTokens=").append(inputTokens == null ? "-" : inputTokens)
                .append(" outputTokens=").append(outputTokens == null ? "-" : outputTokens);
        if (inputTokens != null && outputTokens != null) {
            sb.append(" totalTokens=").append(inputTokens + outputTokens);
        }
        sb.append(" httpStatus=").append(httpStatus);
        if (errorCategory != null) {
            sb.append(" errorCategory=").append(errorCategory);
        }
        System.err.println(sb);
    }

    private String cacheKey(long userId, AiAction action, JsonNode input, int maxTokens) {
        String raw = userId + "|" + action.actionKey() + "|" + action.promptVersion() + "|"
                + service.modelId() + "|" + config.aiMaxOutputTokens() + "|" + maxTokens + "|"
                + (input == null ? "" : Json.write(input));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private <T> T parseAndValidate(String raw, AiValidator<T> validator,
                                   java.util.function.Function<T, List<String>> domainCheck) {
        if (raw == null || raw.isBlank()) {
            throw new AiValidationException("AI returned an empty response");
        }
        JsonNode node;
        try {
            node = Json.extractJson(raw);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Response was not valid JSON: " + e.getMessage());
        }
        if (node == null || !node.isObject()) {
            throw new AiValidationException("AI response must be a JSON object");
        }
        T value = validator.validate(node);
        if (domainCheck != null) {
            List<String> domainErrors = domainCheck.apply(value);
            if (domainErrors != null && !domainErrors.isEmpty()) {
                throw new AiValidationException(domainErrors);
            }
        }
        return value;
    }

    private void checkLocalRateLimit(long userId, AiAction action) {
        int limit = config.aiRateLimitPerHour();
        if (limit <= 0) return;
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            int used = jobDao.countSince(userId, action.actionKey(), oneHourAgo);
            if (used >= limit) {
                throw new AiException(AiFailureType.RATE_LIMITED_LOCAL,
                        "used " + used + " of " + limit + " for " + action.actionKey()
                                + " in the last hour");
            }
        } catch (SQLException e) {
            System.err.println("[ExamLens] Rate-limit check failed: " + e.getMessage());
        }
    }

    private void backoff(int attempt) {
        try { Thread.sleep(500L * attempt); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void completeJob(long jobId, String raw, int attempts, int corrections, long duration) {
        try { jobDao.markSucceeded(jobId, raw, attempts, corrections, duration); }
        catch (SQLException e) { System.err.println("[ExamLens] Could not record AI job success: " + e.getMessage()); }
    }

    private void recordFailure(boolean[] recorded, long jobId, AiFailureType type, String detail,
                               int attempts, int corrections, long start) {
        if (recorded[0]) return;
        recorded[0] = true;
        failJob(jobId, type, detail, attempts, corrections, start);
    }

    private void failJob(long jobId, AiFailureType type, String detail, int attempts, int corrections, long start) {
        try { jobDao.markFailed(jobId, type.code(), detail, attempts, corrections, System.currentTimeMillis() - start); }
        catch (SQLException e) { System.err.println("[ExamLens] Could not record AI job failure: " + e.getMessage()); }
    }

    private record CachedResponse(String raw, Integer inputTokens, Integer outputTokens) { }
}
