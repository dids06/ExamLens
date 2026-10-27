package com.examlens.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Connection pool holder. The pool is created lazily and closed by the application bootstrap. */
public final class Db {

    private static volatile HikariDataSource dataSource;
    private static volatile boolean testOverride;

    private Db() {
    }

    public static synchronized DataSource get() {
        if (dataSource == null) {
            AppConfig cfg = AppConfig.get();
            HikariConfig hc = new HikariConfig();
            hc.setJdbcUrl(cfg.dbUrl());
            // Tomcat's leak-prevention listener initialises DriverManager before the webapp classloader
            // exists, so ServiceLoader auto-registration of the JDBC driver cannot be relied on.
            // Loading the driver class explicitly avoids "No suitable driver" at startup.
            String driver = driverFor(cfg.dbUrl());
            if (driver != null) {
                hc.setDriverClassName(driver);
            }
            hc.setUsername(cfg.dbUsername());
            hc.setPassword(cfg.dbPassword());
            hc.setMaximumPoolSize(10);
            hc.setMinimumIdle(2);
            hc.setPoolName("examlens-pool");
            hc.setConnectionTimeout(10_000);
            hc.setValidationTimeout(5_000);
            hc.addDataSourceProperty("cachePrepStmts", "true");
            hc.addDataSourceProperty("prepStmtCacheSize", "250");
            dataSource = new HikariDataSource(hc);
        }
        return dataSource;
    }

    public static Connection connection() throws SQLException {
        return get().getConnection();
    }

    /** Maps a JDBC URL to the driver class so the pool does not depend on DriverManager auto-registration. */
    static String driverFor(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        String url = jdbcUrl.toLowerCase();
        if (url.startsWith("jdbc:mysql")) {
            return "com.mysql.cj.jdbc.Driver";
        }
        if (url.startsWith("jdbc:mariadb")) {
            return "org.mariadb.jdbc.Driver";
        }
        if (url.startsWith("jdbc:h2")) {
            return "org.h2.Driver";
        }
        return null;
    }

    /** Used by automated tests with an H2 datasource. */
    public static synchronized void setDataSource(DataSource ds) {
        close();
        if (ds instanceof HikariDataSource hds) {
            dataSource = hds;
        } else {
            HikariConfig hc = new HikariConfig();
            hc.setDataSource(ds);
            hc.setMaximumPoolSize(4);
            dataSource = new HikariDataSource(hc);
        }
        testOverride = true;
    }

    public static synchronized void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        testOverride = false;
    }

    public static boolean isTestOverride() {
        return testOverride;
    }
}
