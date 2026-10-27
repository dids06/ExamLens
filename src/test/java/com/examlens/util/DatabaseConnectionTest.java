package com.examlens.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Minimal database connection test runner that can be executed locally without exposing credentials.
 */
public class DatabaseConnectionTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" ExamLens Database Connection Test");
        System.out.println("==================================================");

        String url = ConfigUtil.getDatabaseUrl();
        System.out.println("Target JDBC URL : " + url);

        if (!ConfigUtil.hasDatabaseCredentials()) {
            System.out.println();
            System.out.println("[SKIPPED] Missing Credentials.");
            System.out.println("Environment variables DB_USERNAME and DB_PASSWORD are not set.");
            System.out.println();
            System.out.println("To test against your local MySQL instance, set them in PowerShell:");
            System.out.println("  $env:DB_USERNAME = \"your_mysql_username\"");
            System.out.println("  $env:DB_PASSWORD = \"your_mysql_password\"");
            System.out.println("  $env:DB_URL      = \"jdbc:mysql://localhost:3306/examlens\" # Optional");
            System.out.println();
            System.out.println("Then re-run this connection test.");
            System.out.println("==================================================");
            return;
        }

        String username = ConfigUtil.getDatabaseUsername();
        System.out.println("Database User   : " + username);
        System.out.println("Connecting to MySQL...");

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT VERSION() AS ver, DATABASE() AS db_name")) {

            if (rs.next()) {
                String version = rs.getString("ver");
                String activeDb = rs.getString("db_name");
                System.out.println();
                System.out.println("[SUCCESS] Database connection established successfully!");
                System.out.println("MySQL Server Version : " + version);
                System.out.println("Active Database      : " + activeDb);
            } else {
                System.out.println("[SUCCESS] Connection opened, but metadata query returned empty result.");
            }

        } catch (SQLException e) {
            System.err.println();
            System.err.println("[FAILED] Database connection failed!");
            System.err.println("SQL State : " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Message   : " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println();
            System.err.println("[ERROR] Unexpected error during connection test: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("==================================================");
    }
}
