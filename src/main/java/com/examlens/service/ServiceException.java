package com.examlens.service;

/**
 * A user-facing domain failure raised by a service (missing record, invalid input,
 * unmet precondition). Controllers translate this into an accurate error message;
 * it never represents a successful operation.
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String userMessage;

    public ServiceException(String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    public ServiceException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
