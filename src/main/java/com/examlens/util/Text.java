package com.examlens.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Small text helpers used by extraction, prompts and views. */
public final class Text {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");

    private Text() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String trimTo(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    /** Truncates with an explicit notice so the AI and the student both see that content was cut. */
    public static String truncateWithNotice(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "\n\n[content truncated by ExamLens: "
                + (s.length() - max) + " additional characters were not sent]";
    }

    public static String normalizeWhitespace(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\u00a0', ' ').replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    public static String formatDateTime(LocalDateTime dt) {
        return dt == null ? "—" : dt.format(DATE_TIME);
    }

    public static String formatDate(java.time.LocalDate d) {
        return d == null ? "—" : d.format(DATE);
    }

    /** Wraps untrusted document text in delimiters and neutralizes obvious prompt-injection framing. */
    public static String wrapUntrusted(String label, String content) {
        String safe = content == null ? "" : content
                .replace("\u0000", "")
                .replaceAll("(?i)<\\s*/?\\s*(system|assistant|user)\\s*>", "[removed-tag]");
        return "<<<BEGIN UNTRUSTED " + label + ">>>\n" + safe + "\n<<<END UNTRUSTED " + label + ">>>";
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "—";
        }
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        String visible = name.length() <= 2 ? name.substring(0, 1) : name.substring(0, 2);
        return visible + "***" + email.substring(at);
    }

    public static int parseInt(String value, int fallback) {
        if (isBlank(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static Long parseLongOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String joinLines(Iterable<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String s : items) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
