package com.examlens.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Applies pending database migrations at startup and verifies the storage directory.
 * Never drops or resets schema. A migration failure fails startup loudly instead of
 * running against a partially migrated database.
 */
@WebListener
public class AppBootstrap implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AppConfig cfg = AppConfig.load();
        sce.getServletContext().log("ExamLens starting. " + cfg.safeSummary());

        Path storage = cfg.storageDir();
        try {
            Files.createDirectories(storage);
            Path probe = storage.resolve(".write-probe");
            Files.writeString(probe, "ok");
            Files.deleteIfExists(probe);
        } catch (Exception e) {
            sce.getServletContext().log("ExamLens storage directory is not writable: " + storage, e);
            throw new IllegalStateException("Upload storage directory is not writable: " + storage, e);
        }

        try (Connection c = Db.connection()) {
            int applied = MigrationRunner.apply(c);
            sce.getServletContext().log("ExamLens database ready. Migrations applied this boot: " + applied);
        } catch (SQLException e) {
            sce.getServletContext().log("ExamLens database unavailable: " + e.getMessage());
            throw new IllegalStateException("Cannot connect to the ExamLens database. Check DB_URL/DB_USERNAME/DB_PASSWORD.", e);
        }

        // Build the composition root eagerly so a wiring problem fails at startup, not on first request.
        var context = com.examlens.web.AppContext.get();
        sce.getServletContext().log("ExamLens services wired. AI available=" + context.ai().isAiAvailable()
                + ", simulated=" + context.ai().isSimulated());
        if (!context.ai().isAiAvailable()) {
            sce.getServletContext().log("ExamLens AI is NOT available: " + context.ai().unavailabilityMessage()
                    + " (set GEMINI_API_KEY or GROQ_API_KEY to enable AI actions).");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Db.close();
    }
}
