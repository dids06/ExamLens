package com.examlens.ai;

/**
 * Provider-independent AI service. Implementations must be replaceable without touching
 * controllers, JSPs or business services.
 */
public interface AiService {

    String providerId();

    String modelId();

    boolean isConfigured();

    String unavailabilityMessage();

    AiResponse generate(AiRequest request) throws AiException;
}
