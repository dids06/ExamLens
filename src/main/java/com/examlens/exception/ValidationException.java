package com.examlens.exception;

/**
 * Thrown when client or user input fails business or format validation.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
