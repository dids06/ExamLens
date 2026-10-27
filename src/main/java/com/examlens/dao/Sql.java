package com.examlens.dao;

import com.examlens.config.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Minimal JDBC helpers. Every query uses prepared statements and explicit parameter binding. */
public final class Sql {

    private Sql() {
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    public static List<java.util.Map<String, Object>> query(String sql, StatementBinder binder) throws SQLException {
        List<java.util.Map<String, Object>> rows = new ArrayList<>();
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                var meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    java.util.LinkedHashMap<String, Object> row = new java.util.LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    public static <T> List<T> list(String sql, StatementBinder binder, RowMapper<T> mapper) throws SQLException {
        List<T> out = new ArrayList<>();
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
            }
        }
        return out;
    }

    public static <T> Optional<T> one(String sql, StatementBinder binder, RowMapper<T> mapper) throws SQLException {
        List<T> rows = list(sql, binder, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public static long insert(String sql, StatementBinder binder) throws SQLException {
        try (Connection c = Db.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SQLException("Insert did not return a generated key");
        }
    }

    public static int update(String sql, StatementBinder binder) throws SQLException {
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    public static <T> T transaction(TxWork<T> work) throws SQLException {
        try (Connection c = Db.connection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(auto);
            }
        }
    }

    public static void setLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    public static void setInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    public static void setString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    public static void setTimestamp(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    public static void setDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, java.sql.Date.valueOf(value));
        }
    }

    public static Long getLong(ResultSet rs, String column) throws SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }

    public static Integer getInt(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    public static Double getDouble(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }

    public static LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    public static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }
}
