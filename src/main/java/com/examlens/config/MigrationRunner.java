package com.examlens.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Simple, forward-only, idempotent SQL migration runner. */
public final class MigrationRunner {

    private MigrationRunner() {
    }

    public static int apply(Connection connection) throws SQLException {
        ensureHistoryTable(connection);
        List<String> files = listMigrations();
        int applied = 0;
        for (String file : files) {
            if (isApplied(connection, file)) {
                continue;
            }
            String sql = read(file);
            connection.setAutoCommit(false);
            try {
                for (String statement : splitStatements(sql)) {
                    try (Statement st = connection.createStatement()) {
                        st.execute(statement);
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO schema_migrations (version, applied_at) VALUES (?, CURRENT_TIMESTAMP)")) {
                    ps.setString(1, file);
                    ps.executeUpdate();
                }
                connection.commit();
                applied++;
            } catch (SQLException e) {
                connection.rollback();
                throw new SQLException("Migration failed: " + file + " — " + e.getMessage(), e);
            } finally {
                connection.setAutoCommit(true);
            }
        }
        return applied;
    }

    public static List<String> listMigrations() {
        List<String> files = new ArrayList<>();
        try (InputStream in = MigrationRunner.class.getResourceAsStream("/db/migration/index.txt")) {
            if (in == null) {
                throw new IllegalStateException("Migration index missing");
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        files.add(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read migration index", e);
        }
        files.sort(Comparator.naturalOrder());
        return files;
    }

    private static void ensureHistoryTable(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                      version VARCHAR(160) NOT NULL PRIMARY KEY,
                      applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }
    }

    private static boolean isApplied(Connection c, String version) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM schema_migrations WHERE version = ?")) {
            ps.setString(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    static String read(String file) {
        try (InputStream in = MigrationRunner.class.getResourceAsStream("/db/migration/" + file)) {
            if (in == null) {
                throw new IllegalStateException("Migration not found: " + file);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read migration " + file, e);
        }
    }

    /** Splits on semicolons that are not inside single/double quotes or comments. */
    static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (inLineComment) {
                if (ch == '\n') {
                    inLineComment = false;
                    current.append(ch);
                }
                continue;
            }
            if (inBlockComment) {
                if (ch == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (!inSingle && !inDouble) {
                if (ch == '-' && next == '-') {
                    inLineComment = true;
                    i++;
                    continue;
                }
                if (ch == '/' && next == '*') {
                    inBlockComment = true;
                    i++;
                    continue;
                }
            }
            if (ch == '\'' && !inDouble) {
                inSingle = !inSingle;
                current.append(ch);
                continue;
            }
            if (ch == '"' && !inSingle) {
                inDouble = !inDouble;
                current.append(ch);
                continue;
            }
            if (ch == ';' && !inSingle && !inDouble) {
                addIfPresent(statements, current);
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        addIfPresent(statements, current);
        return statements;
    }

    private static void addIfPresent(List<String> list, StringBuilder sb) {
        String s = sb.toString().trim();
        if (!s.isEmpty()) {
            list.add(s);
        }
    }
}
