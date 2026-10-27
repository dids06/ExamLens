package com.examlens.dao.impl;

import com.examlens.dao.TopicDao;
import com.examlens.exception.DataAccessException;
import com.examlens.model.Topic;
import com.examlens.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of TopicDao using prepared statements.
 */
public class TopicDaoImpl implements TopicDao {

    private static final String INSERT_SQL =
            "INSERT INTO topics (exam_id, parent_topic_id, name, estimated_minutes, priority, status, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_BY_ID_SQL =
            "SELECT id, exam_id, parent_topic_id, name, estimated_minutes, priority, status, created_at " +
            "FROM topics WHERE id = ?";

    private static final String FIND_BY_ID_AND_EXAM_SQL =
            "SELECT id, exam_id, parent_topic_id, name, estimated_minutes, priority, status, created_at " +
            "FROM topics WHERE id = ? AND exam_id = ?";

    private static final String FIND_ALL_BY_EXAM_SQL =
            "SELECT id, exam_id, parent_topic_id, name, estimated_minutes, priority, status, created_at " +
            "FROM topics WHERE exam_id = ? " +
            "ORDER BY CASE WHEN parent_topic_id IS NULL THEN id ELSE parent_topic_id END ASC, id ASC";

    private static final String UPDATE_SQL =
            "UPDATE topics SET parent_topic_id = ?, name = ?, estimated_minutes = ?, priority = ?, status = ? " +
            "WHERE id = ? AND exam_id = ?";

    private static final String DELETE_SQL =
            "DELETE FROM topics WHERE id = ?";

    @Override
    public Topic save(Topic topic) {
        if (topic == null) {
            throw new IllegalArgumentException("Topic entity cannot be null.");
        }

        LocalDateTime createdAt = (topic.getCreatedAt() != null) ? topic.getCreatedAt() : LocalDateTime.now();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, topic.getExamId());
            if (topic.getParentTopicId() != null) {
                stmt.setLong(2, topic.getParentTopicId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, topic.getName());
            stmt.setInt(4, topic.getEstimatedMinutes());
            stmt.setString(5, topic.getPriority());
            stmt.setString(6, topic.getStatus());
            stmt.setObject(7, createdAt);

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    topic.setId(generatedKeys.getLong(1));
                    topic.setCreatedAt(createdAt);
                } else {
                    throw new DataAccessException("Creating topic failed, no ID obtained.");
                }
            }

            return topic;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to persist topic: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Topic> findById(Long topicId) {
        if (topicId == null) {
            return Optional.empty();
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_ID_SQL)) {

            stmt.setLong(1, topicId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTopic(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query topic by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Topic> findByIdAndExamId(Long topicId, Long examId) {
        if (topicId == null || examId == null) {
            return Optional.empty();
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_ID_AND_EXAM_SQL)) {

            stmt.setLong(1, topicId);
            stmt.setLong(2, examId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTopic(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query topic by ID and exam ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Topic> findAllByExamId(Long examId) {
        if (examId == null) {
            return new ArrayList<>();
        }

        List<Topic> topics = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_ALL_BY_EXAM_SQL)) {

            stmt.setLong(1, examId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    topics.add(mapResultSetToTopic(rs));
                }
            }

            return topics;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query topics by exam ID: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Topic topic) {
        if (topic == null || topic.getId() == null || topic.getExamId() == null) {
            throw new IllegalArgumentException("Topic ID and Exam ID are required for update.");
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            if (topic.getParentTopicId() != null) {
                stmt.setLong(1, topic.getParentTopicId());
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            stmt.setString(2, topic.getName());
            stmt.setInt(3, topic.getEstimatedMinutes());
            stmt.setString(4, topic.getPriority());
            stmt.setString(5, topic.getStatus());
            stmt.setLong(6, topic.getId());
            stmt.setLong(7, topic.getExamId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update topic: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long topicId) {
        if (topicId == null) {
            return false;
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setLong(1, topicId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete topic: " + e.getMessage(), e);
        }
    }

    private Topic mapResultSetToTopic(ResultSet rs) throws SQLException {
        Topic topic = new Topic();
        topic.setId(rs.getLong("id"));
        topic.setExamId(rs.getLong("exam_id"));

        long parentId = rs.getLong("parent_topic_id");
        if (!rs.wasNull()) {
            topic.setParentTopicId(parentId);
        } else {
            topic.setParentTopicId(null);
        }

        topic.setName(rs.getString("name"));
        topic.setEstimatedMinutes(rs.getInt("estimated_minutes"));
        topic.setPriority(rs.getString("priority"));
        topic.setStatus(rs.getString("status"));
        topic.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return topic;
    }
}
