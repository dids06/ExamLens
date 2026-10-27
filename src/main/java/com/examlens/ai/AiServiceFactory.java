package com.examlens.ai;

import com.examlens.config.AppConfig;

/** Chooses the configured AI provider. Never silently substitutes a mock for a missing Gemini key. */
public final class AiServiceFactory {

    private AiServiceFactory() {
    }

    public static AiService create(AppConfig config) {
        String configured = config.aiProvider();
        if ("mock".equals(configured) || "simulated".equals(configured)) {
            return new MockAiService();
        }
        if ("groq".equals(configured)) {
            return new GroqAiService(config);
        }
        return new GeminiAiService(config);
    }
}
