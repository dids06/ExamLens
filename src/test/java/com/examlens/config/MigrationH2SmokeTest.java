package com.examlens.config;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the MySQL-targeted migrations also run cleanly against an in-memory MySQL-compatible H2. */
class MigrationH2SmokeTest {

    @Test
    void migrationsApplyAndCreateCoreTables() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:migrations;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "")) {
            int applied = MigrationRunner.apply(connection);
            assertTrue(applied >= 3, "all migrations should apply, applied=" + applied);

            // Second run must be idempotent.
            int again = MigrationRunner.apply(connection);
            assertTrue(again == 0, "re-running migrations must not reapply anything, applied=" + again);

            for (String table : new String[]{"users", "exams", "documents", "topics", "study_plans", "study_tasks"}) {
                try (Statement st = connection.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rs.next();
                }
            }
        }
    }
}
