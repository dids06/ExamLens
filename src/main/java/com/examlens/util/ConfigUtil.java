package com.examlens.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for reading application configuration from environment variables,
 * system properties, or an optional local .env file.
 */
public final class ConfigUtil {

    private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/examlens";
    private static final Map<String, String> DOTENV_MAP = new HashMap<>();

    static {
        loadDotEnv();
    }

    private ConfigUtil() {
        // Prevent instantiation
    }

    /**
     * Loads key-value pairs from a local .env file if present in the working directory.
     * System environment variables always take precedence over .env entries.
     */
    private static void loadDotEnv() {
        File dotEnvFile = new File(".env");
        if (!dotEnvFile.exists() || !dotEnvFile.isFile()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(dotEnvFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    DOTENV_MAP.put(key, value);
                }
            }
        } catch (IOException ignored) {
            // Silently fall back to system environment
        }
    }

    /**
     * Retrieves configuration value for key.
     * Lookup order:
     * 1. System environment variable (System.getenv)
     * 2. System property (System.getProperty)
     * 3. Local .env file entry
     */
    public static String get(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) {
            return val;
        }
        val = System.getProperty(key);
        if (val != null && !val.isBlank()) {
            return val;
        }
        val = DOTENV_MAP.get(key);
        if (val != null && !val.isBlank()) {
            return val;
        }
        return null;
    }

    /**
     * Retrieves configuration value for key, or defaultValue if absent/blank.
     */
    public static String get(String key, String defaultValue) {
        String val = get(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    /**
     * Retrieves a required configuration value. Throws IllegalStateException if missing.
     * Never reveals secret values in exception messages.
     */
    public static String getRequired(String key) {
        String val = get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Required configuration key is missing: " + key);
        }
        return val;
    }

    /**
     * Gets the database JDBC URL. Defaults to jdbc:mysql://localhost:3306/examlens.
     */
    public static String getDatabaseUrl() {
        return get("DB_URL", DEFAULT_DB_URL);
    }

    /**
     * Gets the database username. Throws IllegalStateException if not set.
     */
    public static String getDatabaseUsername() {
        return getRequired("DB_USERNAME");
    }

    /**
     * Gets the database password. Throws IllegalStateException if not set.
     */
    public static String getDatabasePassword() {
        return getRequired("DB_PASSWORD");
    }

    /**
     * Checks whether both DB_USERNAME and DB_PASSWORD are configured.
     */
    public static boolean hasDatabaseCredentials() {
        String user = get("DB_USERNAME");
        String pass = get("DB_PASSWORD");
        return user != null && !user.isBlank() && pass != null && !pass.isBlank();
    }
}
