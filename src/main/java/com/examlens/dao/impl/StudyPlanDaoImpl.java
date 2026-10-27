package com.examlens.dao.impl;

import com.examlens.dao.StudyPlanDao;
import com.examlens.exception.DataAccessException;
import com.examlens.model.StudyPlan;
import com.examlens.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of StudyPlanDao using prepared statements and MySQL.
 */
public class StudyPlanDaoImpl implements StudyPlanDao {

    private static final String SQL_INSERT =
            "INSERT INTO study_plans (exam_id, version, created_at, change_reason) VALUES (?, ?, ?, ?)";

    private static final String SQL_FIND_LATEST =
            "SELECT id, exam_id, version, created_at, change_reason " +
            "FROM study_plans WHERE exam_id = ? ORDER BY version DESC LIMIT 1";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, exam_id, version, created_at, change_reason FROM study_plans WHERE id = ?";

    private static final String SQL_FIND_ALL_BY_EXAM_ID =
            "SELECT id, exam_id, version, created_at, change_reason " +
            "FROM study_plans WHERE exam_id = ? ORDER BY version ASC";

    private static final String SQL_NEXT_VERSION =
            "SELECT COALESCE(MAX(version), 0) + 1 FROM study_plans WHERE exam_id = ?";

    @Override
    public StudyPlan save(Connection conn, StudyPlan plan) {
        if (conn == null) {
            throw new IllegalArgumentException("Connection cannot be null for transactional save.");
        }
        if (plan == null) {
            throw new IllegalArgumentException("StudyPlan cannot be null.");
        }

        LocalDateTime now = (plan.getCreatedAt() != null) ? plan.getCreatedAt() : LocalDateTime.now();

        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, plan.getExamId());
            stmt.setInt(2, (plan.getVersion() != null) ? plan.getVersion() : 1);
            stmt.setTimestamp(3, Timestamp.valueOf(now));
            stmt.setString(4, plan.getChangeReason() != null ? plan.getChangeReason() : StudyPlan.DEFAULT_INITIAL_REASON);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Failed to insert study plan, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    plan.setId(generatedKeys.getLong(1));
                    plan.setCreatedAt(now);
                } else {
                    throw new DataAccessException("Failed to insert study plan, no generated ID obtained.");
                }
            }

            return plan;
        } catch (SQLException e) {
            throw new DataAccessException("Database error occurred while saving study plan: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<StudyPlan> findLatestByExamId(Long examId) {
        if (examId == null) {
            return Optional.empty();
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_LATEST)) {

            stmt.setLong(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPlan(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Database error occurred while finding latest study plan for exam #" + examId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<StudyPlan> findById(Long planId) {
        if (planId == null) {
            return Optional.empty();
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            stmt.setLong(1, planId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPlan(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Database error occurred while finding study plan #" + planId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<StudyPlan> findAllByExamId(Long examId) {
        if (examId == null) {
            return List.of();
        }

        List<StudyPlan> plans = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL_BY_EXAM_ID)) {

            stmt.setLong(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    plans.add(mapResultSetToPlan(rs));
                }
            }
            return plans;
        } catch (SQLException e) {
            throw new DataAccessException("Database error occurred while finding study plans for exam #" + examId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public int getNextVersion(Connection conn, Long examId) {
        if (examId == null) {
            return 1;
        }

        boolean manageConn = (conn == null);
        Connection activeConn = null;
        try {
            activeConn = manageConn ? DatabaseUtil.getConnection() : conn;
            try (PreparedStatement stmt = activeConn.prepareStatement(SQL_NEXT_VERSION)) {
                stmt.setLong(1, examId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return 1;
        } catch (SQLException e) {
            throw new DataAccessException("Database error occurred while calculating next version for exam #" + examId + ": " + e.getMessage(), e);
        } finally {
            if (manageConn && activeConn != null) {
                DatabaseUtil.closeQuietly(activeConn);
            }
        }
    }

    private StudyPlan mapResultSetToPlan(ResultSet rs) throws SQLException {
        StudyPlan plan = new StudyPlan();
        plan.setId(rs.getLong("id"));
        plan.setExamId(rs.getLong("exam_id"));
        plan.setVersion(rs.getInt("version"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            plan.setCreatedAt(createdTs.toLocalDateTime());
        }
        plan.setChangeReason(rs.getString("change_reason"));
        return plan;
    }
}
