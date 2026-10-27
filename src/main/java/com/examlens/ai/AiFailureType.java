package com.examlens.ai;

import java.util.List;

/** Failure classification used for user messaging and retry policy. */
public enum AiFailureType {
    MISSING_API_KEY(false, "The AI provider is not configured. An administrator must set the AI provider API key."),
    INVALID_API_KEY(false, "The AI provider rejected the configured credentials. Check the AI provider API key."),
    QUOTA_EXHAUSTED(false, "The AI provider quota is exhausted. AI features will work again after the quota resets."),
    RATE_LIMITED(true, "The AI provider is rate limiting requests. Please wait a moment and try again."),
    MODEL_NOT_FOUND(false, "The configured AI model is unavailable. Check the AI model setting for the active provider."),
    TIMEOUT(true, "The AI request timed out before it finished. You can retry."),
    NETWORK_ERROR(true, "ExamLens could not reach the AI provider. Check the network connection and retry."),
    PROVIDER_ERROR(true, "The AI provider returned an error. Please retry."),
    REFUSED(false, "The AI provider declined to process this request. Try rephrasing or provide different material."),
    INVALID_JSON(false, "The AI response was not valid JSON, and the automatic correction attempt did not fix it."),
    SCHEMA_INVALID(false, "The AI response did not match the required structure, and the automatic correction attempt did not fix it."),
    INPUT_TOO_LARGE(false, "The supplied material is too large for one AI request. Reduce the amount of text and retry."),
    RATE_LIMITED_LOCAL(true, "You have reached the ExamLens AI usage limit for this hour. Please try again later."),
    DOCUMENT_UNREADABLE(false, "The document did not contain readable text. Upload a text-based file or clearer scan."),
    UNSUPPORTED(false, "This AI action is not supported by the configured provider."),
    UNKNOWN(true, "The AI request failed unexpectedly. Please retry.");

    private final boolean retryable;
    private final String userMessage;

    AiFailureType(boolean retryable, String userMessage) {
        this.retryable = retryable;
        this.userMessage = userMessage;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String userMessage() {
        return userMessage;
    }

    public boolean indicatesUnavailableAi() {
        return switch (this) {
            case MISSING_API_KEY, INVALID_API_KEY, QUOTA_EXHAUSTED, MODEL_NOT_FOUND, UNSUPPORTED -> true;
            default -> false;
        };
    }

    public String code() {
        return name();
    }

    public static AiFailureType fromHttpStatus(int status, String body) {
        String lower = body == null ? "" : body.toLowerCase();
        if (status == 400 && (lower.contains("api key not valid") || lower.contains("api_key_invalid"))) {
            return INVALID_API_KEY;
        }
        if (status == 401 || status == 403) {
            return lower.contains("quota") ? QUOTA_EXHAUSTED : INVALID_API_KEY;
        }
        if (status == 404) {
            return MODEL_NOT_FOUND;
        }
        if (status == 429) {
            return lower.contains("quota") || lower.contains("exceeded") ? QUOTA_EXHAUSTED : RATE_LIMITED;
        }
        if (status >= 500) {
            return PROVIDER_ERROR;
        }
        if (status == 400) {
            return PROVIDER_ERROR;
        }
        return UNKNOWN;
    }

    public static List<AiFailureType> retryableTypes() {
        return List.of(RATE_LIMITED, TIMEOUT, NETWORK_ERROR, PROVIDER_ERROR);
    }
}
