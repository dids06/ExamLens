package com.examlens.dao;

import com.examlens.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserDao {

    /** Study-preference row for a user, with schema defaults applied when no row exists yet. */
    public record Settings(int defaultStudyMinutes, String preferredDepth, int retentionDays) {
        public int getDefaultStudyMinutes() { return defaultStudyMinutes; }
        public String getPreferredDepth() { return preferredDepth; }
        public int getRetentionDays() { return retentionDays; }
    }

    public long insert(String email, String displayName, String passwordHash, String timezone) throws SQLException {
        return Sql.insert("""
                INSERT INTO users (email, display_name, password_hash, timezone, theme, created_at)
                VALUES (?, ?, ?, ?, 'system', CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setString(1, email.toLowerCase());
            ps.setString(2, displayName);
            ps.setString(3, passwordHash);
            ps.setString(4, timezone);
        });
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        return Sql.one("SELECT * FROM users WHERE email = ?", ps -> ps.setString(1, email.toLowerCase()), this::map);
    }

    public Optional<User> find(long id) throws SQLException {
        return Sql.one("SELECT * FROM users WHERE id = ?", ps -> ps.setLong(1, id), this::map);
    }

    public void touchLogin(long id) throws SQLException {
        Sql.update("UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?", ps -> ps.setLong(1, id));
    }

    public void updateProfile(long id, String displayName, String timezone, String theme) throws SQLException {
        Sql.update("UPDATE users SET display_name = ?, timezone = ?, theme = ? WHERE id = ?", ps -> {
            ps.setString(1, displayName);
            ps.setString(2, timezone);
            ps.setString(3, theme);
            ps.setLong(4, id);
        });
    }

    public void updatePassword(long id, String passwordHash) throws SQLException {
        Sql.update("UPDATE users SET password_hash = ? WHERE id = ?", ps -> {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
        });
    }

    public void updateTheme(long id, String theme) throws SQLException {
        Sql.update("UPDATE users SET theme = ? WHERE id = ?", ps -> {
            ps.setString(1, theme);
            ps.setLong(2, id);
        });
    }

    public void deleteAccount(long id) throws SQLException {
        Sql.update("DELETE FROM users WHERE id = ?", ps -> ps.setLong(1, id));
    }

    public void ensureSettings(long userId) throws SQLException {
        List<Long> rows = Sql.list("SELECT user_id FROM user_settings WHERE user_id = ?", ps -> ps.setLong(1, userId),
                rs -> rs.getLong("user_id"));
        if (rows.isEmpty()) {
            Sql.update("INSERT INTO user_settings (user_id) VALUES (?)", ps -> ps.setLong(1, userId));
        }
    }

    public int defaultStudyMinutes(long userId) throws SQLException {
        List<Integer> rows = Sql.list("SELECT default_study_minutes FROM user_settings WHERE user_id = ?",
                ps -> ps.setLong(1, userId), rs -> rs.getInt("default_study_minutes"));
        return rows.isEmpty() ? 120 : rows.get(0);
    }

    public Settings settings(long userId) throws SQLException {
        List<Settings> rows = Sql.list(
                "SELECT default_study_minutes, preferred_depth, retention_days FROM user_settings WHERE user_id = ?",
                ps -> ps.setLong(1, userId),
                rs -> new Settings(rs.getInt("default_study_minutes"), rs.getString("preferred_depth"),
                        rs.getInt("retention_days")));
        return rows.isEmpty() ? new Settings(120, "STANDARD", 365) : rows.get(0);
    }

    public void updateSettings(long userId, int defaultStudyMinutes, String depth, int retentionDays) throws SQLException {
        Sql.update("""
                UPDATE user_settings SET default_study_minutes = ?, preferred_depth = ?, retention_days = ?
                WHERE user_id = ?
                """, ps -> {
            ps.setInt(1, defaultStudyMinutes);
            ps.setString(2, depth);
            ps.setInt(3, retentionDays);
            ps.setLong(4, userId);
        });
    }

    private User map(java.sql.ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setEmail(rs.getString("email"));
        u.setDisplayName(rs.getString("display_name"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setTimezone(rs.getString("timezone"));
        u.setTheme(rs.getString("theme"));
        u.setCreatedAt(Sql.getDateTime(rs, "created_at"));
        u.setLastLoginAt(Sql.getDateTime(rs, "last_login_at"));
        return u;
    }

    /** Test/demo helper used by automated tests only. */
    public LocalDateTime createdAt(long id) throws SQLException {
        List<LocalDateTime> rows = Sql.list("SELECT created_at FROM users WHERE id = ?", ps -> ps.setLong(1, id),
                rs -> Sql.getDateTime(rs, "created_at"));
        return rows.isEmpty() ? null : rows.get(0);
    }
}
