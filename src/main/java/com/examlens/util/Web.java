package com.examlens.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Flash messages and request helpers for servlets. */
public final class Web {

    public static final String FLASH_SUCCESS = "flash_success";
    public static final String FLASH_ERROR = "flash_error";
    public static final String FLASH_INFO = "flash_info";

    private Web() {
    }

    public static void success(HttpServletRequest req, String message) {
        req.getSession().setAttribute(FLASH_SUCCESS, message);
    }

    public static void error(HttpServletRequest req, String message) {
        req.getSession().setAttribute(FLASH_ERROR, message);
    }

    public static void info(HttpServletRequest req, String message) {
        req.getSession().setAttribute(FLASH_INFO, message);
    }

    public static String param(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? null : value.trim();
    }

    public static String param(HttpServletRequest req, String name, String fallback) {
        String value = param(req, name);
        return value == null || value.isEmpty() ? fallback : value;
    }

    public static long longParam(HttpServletRequest req, String name, long fallback) {
        String value = param(req, name);
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Returns null for missing/blank/malformed IDs so callers must handle them explicitly. */
    public static Long nullableId(HttpServletRequest req, String name) {
        return Text.parseLongOrNull(param(req, name));
    }

    public static int intParam(HttpServletRequest req, String name, int fallback) {
        return Text.parseInt(param(req, name), fallback);
    }

    public static boolean boolParam(HttpServletRequest req, String name) {
        String v = param(req, name);
        return v != null && (v.equalsIgnoreCase("on") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("1"));
    }

    public static String query(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    /** Only permits same-application relative redirect targets. */
    public static String safeRedirect(String target, String fallback) {
        if (target == null || target.isBlank()) {
            return fallback;
        }
        String t = target.trim();
        if (t.startsWith("//") || t.contains("://") || t.startsWith("\\")) {
            return fallback;
        }
        if (!t.startsWith("/")) {
            return fallback;
        }
        return t;
    }
}
