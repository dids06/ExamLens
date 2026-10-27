package com.examlens.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiFailureTypeTest {

    @Test
    void modelNotFoundMessageIsProviderNeutral() {
        String msg = AiFailureType.MODEL_NOT_FOUND.userMessage();
        assertNotNull(msg);
        assertFalse(msg.contains("GEMINI"), "message must not mention Gemini: " + msg);
        assertFalse(msg.contains("GROQ"), "message must not mention Groq: " + msg);
    }

    @Test
    void missingApiKeyMessageIsProviderNeutral() {
        String msg = AiFailureType.MISSING_API_KEY.userMessage();
        assertFalse(msg.contains("GEMINI"), "message must not mention Gemini: " + msg);
        assertFalse(msg.contains("GROQ"), "message must not mention Groq: " + msg);
    }

    @Test
    void invalidApiKeyMessageIsProviderNeutral() {
        String msg = AiFailureType.INVALID_API_KEY.userMessage();
        assertFalse(msg.contains("GEMINI"), "message must not mention Gemini: " + msg);
        assertFalse(msg.contains("GROQ"), "message must not mention Groq: " + msg);
    }
}
