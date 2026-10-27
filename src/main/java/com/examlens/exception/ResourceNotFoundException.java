package com.examlens.exception;

/**
 * Thrown when a requested resource (exam, topic, plan) does not exist.
 */
public class ResourceNotFoundException extends Exception {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
