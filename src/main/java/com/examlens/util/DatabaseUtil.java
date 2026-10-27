package com.examlens.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Reusable JDBC connection utility using DriverManager and MySQL Connector/J.
 * Reads connection settings from ConfigUtil without hardcoding credentials.
 */
public final class DatabaseUtil {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    static {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC Driver (" + DRIVER_CLASS + ") not found on classpath.", e);
        }
    }

    private DatabaseUtil() {
        // Utility class, prevent instantiation
    }

    /**
     * Obtains a new database connection using settings from ConfigUtil.
     *
     * @return a live java.sql.Connection
     * @throws IllegalStateException if DB_USERNAME or DB_PASSWORD is not configured
     * @throws SQLException          if connection to the database fails
     */
    public static Connection getConnection() throws SQLException {
        String url = ConfigUtil.getDatabaseUrl();
        String username = ConfigUtil.getDatabaseUsername();
        String password = ConfigUtil.getDatabasePassword();

        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            // Provide a clear and sanitized error message without exposing passwords
            throw new SQLException(
                "Unable to connect to MySQL database at " + sanitizeUrl(url) + " (user: " + username + "): " + e.getMessage(),
                e.getSQLState(),
                e.getErrorCode(),
                e
            );
        }
    }

    /**
     * Tests whether a live database connection can be established within timeout seconds.
     *
     * @param timeoutSeconds max wait time in seconds
     * @return true if connection was successfully opened and validated
     * @throws SQLException if a database access error occurs
     */
    public static boolean testConnection(int timeoutSeconds) throws SQLException {
        try (Connection conn = getConnection()) {
            return conn.isValid(timeoutSeconds);
        }
    }

    /**
     * Safely closes a Connection, suppressing any SQLException.
     *
     * @param conn Connection to close (may be null)
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // Suppressed
            }
        }
    }

    /**
     * Sanitizes a JDBC URL for logging/diagnostics by hiding sensitive parameters.
     */
    private static String sanitizeUrl(String url) {
        if (url == null) {
            return "null";
        }
        int qIdx = url.indexOf('?');
        return (qIdx > 0) ? url.substring(0, qIdx) + "?[parameters hidden]" : url;
    }
}
