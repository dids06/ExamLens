package com.examlens.dao.impl;

import com.examlens.dao.ExamDao;
import com.examlens.exception.DataAccessException;
import com.examlens.model.Exam;
import com.examlens.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of ExamDao using prepared statements.
 */
public class ExamDaoImpl implements ExamDao {

    private static final String INSERT_SQL =
            "INSERT INTO exams (user_id, subject_name, exam_date_time, daily_study_minutes, target_marks, status, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_BY_ID_SQL =
            "SELECT id, user_id, subject_name, exam_date_time, daily_study_minutes, target_marks, status, created_at " +
            "FROM exams WHERE id = ?";

    private static final String FIND_BY_ID_AND_USER_SQL =
            "SELECT id, user_id, subject_name, exam_date_time, daily_study_minutes, target_marks, status, created_at " +
            "FROM exams WHERE id = ? AND user_id = ?";

    private static final String FIND_ALL_BY_USER_SQL =
            "SELECT id, user_id, subject_name, exam_date_time, daily_study_minutes, target_marks, status, created_at " +
            "FROM exams WHERE user_id = ? ORDER BY exam_date_time ASC, id ASC";

    private static final String UPDATE_SQL =
            "UPDATE exams SET subject_name = ?, exam_date_time = ?, daily_study_minutes = ?, target_marks = ?, status = ? " +
            "WHERE id = ? AND user_id = ?";

    private static final String DELETE_SQL =
            "DELETE FROM exams WHERE id = ? AND user_id = ?";

    @Override
    public Exam save(Exam exam) {
        if (exam == null) {
            throw new IllegalArgumentException("Exam entity cannot be null.");
        }

        LocalDateTime createdAt = (exam.getCreatedAt() != null) ? exam.getCreatedAt() : LocalDateTime.now();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, exam.getUserId());
            stmt.setString(2, exam.getSubjectName());
            stmt.setObject(3, exam.getExamDateTime());
            stmt.setInt(4, exam.getDailyStudyMinutes());
            stmt.setBigDecimal(5, exam.getTargetMarks());
            stmt.setString(6, exam.getStatus());
            stmt.setObject(7, createdAt);

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    exam.setId(generatedKeys.getLong(1));
                    exam.setCreatedAt(createdAt);
                } else {
                    throw new DataAccessException("Creating exam failed, no ID obtained.");
                }
            }

            return exam;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to persist exam: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Exam> findById(Long examId) {
        if (examId == null) {
            return Optional.empty();
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_ID_SQL)) {

            stmt.setLong(1, examId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToExam(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query exam by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Exam> findByIdAndUserId(Long examId, Long userId) {
        if (examId == null || userId == null) {
            return Optional.empty();
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_ID_AND_USER_SQL)) {

            stmt.setLong(1, examId);
            stmt.setLong(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToExam(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query exam by ID and user ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Exam> findAllByUserId(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        List<Exam> exams = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_ALL_BY_USER_SQL)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    exams.add(mapResultSetToExam(rs));
                }
            }

            return exams;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query exams by user ID: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Exam exam) {
        if (exam == null || exam.getId() == null || exam.getUserId() == null) {
            throw new IllegalArgumentException("Exam ID and User ID are required for update.");
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, exam.getSubjectName());
            stmt.setObject(2, exam.getExamDateTime());
            stmt.setInt(3, exam.getDailyStudyMinutes());
            stmt.setBigDecimal(4, exam.getTargetMarks());
            stmt.setString(5, exam.getStatus());
            stmt.setLong(6, exam.getId());
            stmt.setLong(7, exam.getUserId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update exam: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteByIdAndUserId(Long examId, Long userId) {
        if (examId == null || userId == null) {
            return false;
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setLong(1, examId);
            stmt.setLong(2, userId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete exam: " + e.getMessage(), e);
        }
    }

    private Exam mapResultSetToExam(ResultSet rs) throws SQLException {
        Exam exam = new Exam();
        exam.setId(rs.getLong("id"));
        exam.setUserId(rs.getLong("user_id"));
        exam.setSubjectName(rs.getString("subject_name"));
        exam.setExamDateTime(rs.getObject("exam_date_time", LocalDateTime.class));
        exam.setDailyStudyMinutes(rs.getInt("daily_study_minutes"));
        exam.setTargetMarks(rs.getBigDecimal("target_marks"));
        exam.setStatus(rs.getString("status"));
        exam.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return exam;
    }
}
