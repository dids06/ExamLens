package com.examlens.ai;

/** Raised when an AI request cannot produce a validated result. */
public class AiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final AiFailureType failureType;
    private final String diagnostic;

    public AiException(AiFailureType failureType, String diagnostic) {
        super(failureType.userMessage() + (diagnostic == null || diagnostic.isBlank() ? "" : " (" + diagnostic + ")"));
        this.failureType = failureType;
        this.diagnostic = diagnostic;
    }

    public AiException(AiFailureType failureType, String diagnostic, Throwable cause) {
        super(failureType.userMessage(), cause);
        this.failureType = failureType;
        this.diagnostic = diagnostic;
    }

    public AiFailureType getFailureType() {
        return failureType;
    }

    public String getDiagnostic() {
        return diagnostic;
    }

    public String getUserMessage() {
        return failureType.userMessage();
    }
}
