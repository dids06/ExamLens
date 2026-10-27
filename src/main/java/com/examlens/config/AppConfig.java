package com.examlens.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central configuration loaded from OS environment variables, optionally augmented by a
 * key=value file outside the repository. Secrets are never logged or exposed to JSPs.
 */
public final class AppConfig {

    private static final Map<String, String> FILE_VALUES = new LinkedHashMap<>();
    private static volatile AppConfig instance;

    private final Map<String, String> values;

    private AppConfig(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    public static synchronized AppConfig load() {
        if (instance != null) {
            return instance;
        }
        Map<String, String> merged = new LinkedHashMap<>();
        Path envFile = envFile();
        if (envFile != null && Files.isRegularFile(envFile)) {
            try {
                for (String raw : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    int idx = line.indexOf('=');
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    merged.put(key, value);
                }
            } catch (IOException e) {
                System.err.println("[ExamLens] Could not read env file: " + e.getMessage());
            }
        }
        // Real environment variables always win over the file.
        for (String key : merged.keySet().stream().toList()) {
            String env = System.getenv(key);
            if (env != null && !env.isBlank()) {
                merged.put(key, env.trim());
            }
        }
        for (String key : new String[]{"GEMINI_API_KEY", "GEMINI_MODEL", "GROQ_API_KEY", "GROQ_MODEL", "AI_PROVIDER", "DB_URL", "DB_USERNAME",
                "DB_PASSWORD", "EXAMLENS_STORAGE_DIR", "TESSERACT_DATA_PATH", "APP_SECURE_COOKIES"}) {
            String env = System.getenv(key);
            if (env != null && !env.isBlank()) {
                merged.put(key, env.trim());
            }
        }
        instance = new AppConfig(merged);
        return instance;
    }

    public static AppConfig get() {
        return instance == null ? load() : instance;
    }

    /** Test hook: replaces the singleton. */
    public static synchronized void override(Map<String, String> overrides) {
        instance = new AppConfig(new LinkedHashMap<>(overrides));
    }

    /** Builds an isolated configuration from explicit values without touching the global singleton. */
    public static AppConfig of(Map<String, String> values) {
        return new AppConfig(new LinkedHashMap<>(values));
    }

    private static Path envFile() {
        String explicit = System.getenv("EXAMLENS_ENV_FILE");
        if (explicit != null && !explicit.isBlank()) {
            return Path.of(explicit.trim());
        }
        String home = System.getProperty("user.home");
        return home == null ? null : Path.of(home, ".examlens", "examlens.env");
    }

    public String get(String key) {
        return values.get(key);
    }

    public String get(String key, String fallback) {
        String v = values.get(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    public int getInt(String key, int fallback) {
        try {
            String v = values.get(key);
            return v == null || v.isBlank() ? fallback : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        String v = values.get(key);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(v.trim());
    }

    public String dbUrl() {
        return get("DB_URL", "jdbc:mysql://localhost:3306/examlens?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
    }

    public String dbUsername() {
        return get("DB_USERNAME", "examlens");
    }

    public String dbPassword() {
        return get("DB_PASSWORD", "");
    }

    public String aiProvider() {
        return get("AI_PROVIDER", "gemini").toLowerCase();
    }

    public String geminiApiKey() {
        return get("GEMINI_API_KEY", "");
    }

    public String geminiModel() {
        return get("GEMINI_MODEL", "gemini-2.5-flash");
    }

    public String groqApiKey() {
        return get("GROQ_API_KEY", "");
    }

    public String groqModel() {
        return get("GROQ_MODEL", "llama-3.3-70b-versatile");
    }

    public int aiTimeoutSeconds() {
        return Math.max(10, getInt("AI_TIMEOUT_SECONDS", 60));
    }

    public int aiMaxOutputTokens() {
        return Math.max(1024, getInt("AI_MAX_OUTPUT_TOKENS", 8192));
    }

    public int aiMaxInputChars() {
        return Math.max(4000, getInt("AI_MAX_INPUT_CHARS", 60000));
    }

    public int aiRateLimitPerHour() {
        return Math.max(1, getInt("AI_RATE_LIMIT_PER_HOUR", 30));
    }

    public Path storageDir() {
        String explicit = get("EXAMLENS_STORAGE_DIR", "");
        if (!explicit.isBlank()) {
            return Path.of(explicit);
        }
        String home = System.getProperty("user.home");
        return Path.of(home == null ? "." : home, ".examlens", "uploads");
    }

    public int uploadMaxMb() {
        return Math.max(1, getInt("UPLOAD_MAX_MB", 15));
    }

    public int uploadMaxPages() {
        return Math.max(1, getInt("UPLOAD_MAX_PAGES", 80));
    }

    public String appBaseUrl() {
        return get("APP_BASE_URL", "http://localhost:8080/examlens");
    }

    public boolean secureCookies() {
        return getBoolean("APP_SECURE_COOKIES", false);
    }

    public String tesseractDataPath() {
        return get("TESSERACT_DATA_PATH", "");
    }

    public String tesseractLanguage() {
        return get("TESSERACT_LANGUAGE", "eng");
    }

    /** Diagnostic view with all secret values removed. */
    public String safeSummary() {
        String model = "groq".equals(aiProvider()) ? groqModel() : geminiModel();
        return "provider=" + aiProvider() + ", model=" + model
                + ", geminiKeyConfigured=" + !geminiApiKey().isBlank()
                + ", groqKeyConfigured=" + !groqApiKey().isBlank()
                + ", dbUrl=" + maskJdbc(dbUrl())
                + ", storage=" + storageDir();
    }

    private String maskJdbc(String url) {
        return url.replaceAll("(?i)(password=)[^&;]+", "$1***");
    }
}
