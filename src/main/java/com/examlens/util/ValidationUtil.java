package com.examlens.util;

import com.examlens.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Common input validation helper for user registration, authentication, and form inputs.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 255;

    private ValidationUtil() {
        // Prevent instantiation
    }

    public static void validateRegistration(String name, String email, String password, String confirmPassword) throws ValidationException {
        validateName(name);
        validateEmail(email);
        validatePassword(password);

        if (confirmPassword == null || !password.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }
    }

    public static void validateLogin(String email, String password) throws ValidationException {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required.");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required.");
        }
    }

    public static void validateName(String name) throws ValidationException {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Full name is required.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Name must be between 2 and " + MAX_NAME_LENGTH + " characters.");
        }
    }

    public static void validateEmail(String email) throws ValidationException {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email address is required.");
        }
        String trimmed = email.trim();
        if (trimmed.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException("Email address is too long.");
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Please provide a valid email address.");
        }
    }

    public static void validatePassword(String password) throws ValidationException {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Password cannot exceed " + MAX_PASSWORD_LENGTH + " characters.");
        }
    }
}
