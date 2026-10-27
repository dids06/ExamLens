package com.examlens.dao.impl;

import com.examlens.dao.UserDao;
import com.examlens.exception.DataAccessException;
import com.examlens.model.User;
import com.examlens.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JDBC implementation of UserDao using prepared statements and DatabaseUtil.
 */
public class UserDaoImpl implements UserDao {

    private static final String INSERT_SQL =
            "INSERT INTO users (name, email, password_hash, created_at) VALUES (?, ?, ?, ?)";

    private static final String FIND_BY_ID_SQL =
            "SELECT id, name, email, password_hash, created_at FROM users WHERE id = ?";

    private static final String FIND_BY_EMAIL_SQL =
            "SELECT id, name, email, password_hash, created_at FROM users WHERE LOWER(email) = LOWER(?)";

    private static final String EXISTS_BY_EMAIL_SQL =
            "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";

    @Override
    public User save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User entity cannot be null.");
        }

        LocalDateTime createdAt = (user.getCreatedAt() != null) ? user.getCreatedAt() : LocalDateTime.now();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail().trim().toLowerCase());
            stmt.setString(3, user.getPasswordHash());
            stmt.setObject(4, createdAt);

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                    user.setCreatedAt(createdAt);
                } else {
                    throw new DataAccessException("Creating user failed, no ID obtained.");
                }
            }

            return user;

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DataAccessException("A user with email '" + user.getEmail() + "' already exists.", e);
            }
            throw new DataAccessException("Failed to persist user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_ID_SQL)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query user by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_EMAIL_SQL)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query user by email: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(EXISTS_BY_EMAIL_SQL)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check if email exists: " + e.getMessage(), e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return user;
    }
}
