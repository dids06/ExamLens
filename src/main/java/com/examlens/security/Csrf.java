package com.examlens.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

/** Session-bound CSRF tokens. */
public final class Csrf {

    public static final String SESSION_KEY = "csrfToken";
    public static final String PARAM = "_csrf";
    public static final String HEADER = "X-CSRF-Token";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Csrf() {
    }

    public static String token(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof String s && !s.isBlank()) {
            return s;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_KEY);
        if (!(expected instanceof String expectedToken) || expectedToken.isBlank()) {
            return false;
        }
        String provided = request.getHeader(HEADER);
        if (provided == null || provided.isBlank()) {
            provided = request.getParameter(PARAM);
        }
        if (provided == null) {
            return false;
        }
        // Constant-time comparison.
        byte[] a = expectedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = provided.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(a, b);
    }
}
