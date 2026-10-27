package com.examlens.exception;

/**
 * Thrown when credentials fail authentication (invalid email/password, unauthenticated session).
 */
public class AuthenticationException extends Exception {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
