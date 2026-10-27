package com.examlens.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Cross-Site Request Forgery (CSRF) protection utility.
 * Generates and validates cryptographic tokens bound to the user's HTTP session.
 */
public final class CsrfUtil {

    public static final String CSRF_SESSION_ATTRIBUTE = "CSRF_TOKEN";
    public static final String CSRF_PARAM_NAME = "csrfToken";
    public static final String CSRF_HEADER_NAME = "X-CSRF-Token";

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CsrfUtil() {
        // Prevent instantiation
    }

    /**
     * Retrieves the existing CSRF token from the session, or generates and stores a new one.
     *
     * @param session the user's HTTP session (must not be null)
     * @return 64-character hex token string
     */
    public static String getOrCreateToken(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null when generating CSRF token.");
        }

        String token = (String) session.getAttribute(CSRF_SESSION_ATTRIBUTE);
        if (token == null || token.isBlank()) {
            token = generateToken();
            session.setAttribute(CSRF_SESSION_ATTRIBUTE, token);
        }
        return token;
    }

    /**
     * Regenerates a fresh CSRF token in the session (useful after login/logout).
     */
    public static String rotateToken(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null when rotating CSRF token.");
        }
        String newToken = generateToken();
        session.setAttribute(CSRF_SESSION_ATTRIBUTE, newToken);
        return newToken;
    }

    /**
     * Validates the submitted CSRF token from request parameter or header against the session token.
     *
     * @param request current HttpServletRequest
     * @return true if valid, false otherwise
     */
    public static boolean isValid(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        String sessionToken = (String) session.getAttribute(CSRF_SESSION_ATTRIBUTE);
        if (sessionToken == null || sessionToken.isBlank()) {
            return false;
        }

        String requestToken = request.getParameter(CSRF_PARAM_NAME);
        if (requestToken == null || requestToken.isBlank()) {
            requestToken = request.getHeader(CSRF_HEADER_NAME);
        }

        if (requestToken == null || requestToken.isBlank()) {
            return false;
        }

        // Constant-time comparison
        byte[] expectedBytes = sessionToken.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = requestToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
