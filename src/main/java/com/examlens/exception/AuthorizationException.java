package com.examlens.exception;

/**
 * Thrown when an authenticated user attempts to access or modify resources owned by another student.
 */
public class AuthorizationException extends Exception {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
