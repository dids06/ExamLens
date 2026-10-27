package com.examlens.ai;

/** Raw provider response with safe metadata (never includes the API key). */
public class AiResponse {

    private final String rawText;
    private final String provider;
    private final String model;
    private final long latencyMs;
    private final Integer inputTokens;
    private final Integer outputTokens;

    public AiResponse(String rawText, String provider, String model, long latencyMs,
                      Integer inputTokens, Integer outputTokens) {
        this.rawText = rawText;
        this.provider = provider;
        this.model = model;
        this.latencyMs = latencyMs;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public String getRawText() { return rawText; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public long getLatencyMs() { return latencyMs; }
    public Integer getInputTokens() { return inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
}
