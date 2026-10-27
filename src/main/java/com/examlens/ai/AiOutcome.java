package com.examlens.ai;

import java.util.List;

/** Result of a validated AI action, including audit metadata. */
public class AiOutcome<T> {

    private final T data;
    private final long jobId;
    private final String provider;
    private final String model;
    private final String promptVersion;
    private final boolean simulated;
    private final int correctionsApplied;
    private final long durationMs;
    private final List<String> providerWarnings;
    private final boolean cacheHit;

    public AiOutcome(T data, long jobId, String provider, String model, String promptVersion,
                     boolean simulated, int correctionsApplied, long durationMs, List<String> providerWarnings) {
        this(data, jobId, provider, model, promptVersion, simulated, correctionsApplied, durationMs, providerWarnings, false);
    }

    public AiOutcome(T data, long jobId, String provider, String model, String promptVersion,
                     boolean simulated, int correctionsApplied, long durationMs, List<String> providerWarnings,
                     boolean cacheHit) {
        this.data = data;
        this.jobId = jobId;
        this.provider = provider;
        this.model = model;
        this.promptVersion = promptVersion;
        this.simulated = simulated;
        this.correctionsApplied = correctionsApplied;
        this.durationMs = durationMs;
        this.providerWarnings = providerWarnings == null ? List.of() : providerWarnings;
        this.cacheHit = cacheHit;
    }

    public T getData() { return data; }
    public long getJobId() { return jobId; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public String getPromptVersion() { return promptVersion; }
    public boolean isSimulated() { return simulated; }
    public int getCorrectionsApplied() { return correctionsApplied; }
    public long getDurationMs() { return durationMs; }
    public List<String> getProviderWarnings() { return providerWarnings; }
    public boolean isCacheHit() { return cacheHit; }
}
