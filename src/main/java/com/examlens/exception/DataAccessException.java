package com.examlens.exception;

/**
 * Unchecked runtime exception wrapping unexpected database or persistence failures.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
