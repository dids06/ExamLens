package com.examlens.dao.impl;

import com.examlens.dao.StudyTaskDao;
import com.examlens.exception.DataAccessException;
import com.examlens.model.StudyTask;
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
 * JDBC implementation of StudyTaskDao using prepared statements and MySQL.
 */
public class StudyTaskDaoImpl implements StudyTaskDao {

    private static final String SQL_INSERT =
            "INSERT INTO study_tasks (plan_id, topic_id, planned_start, planned_end, " +
            "planned_minutes, actual_minutes, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_ALL_BY_PLAN_ID =
            "SELECT st.id, st.plan_id, st.topic_id, st.planned_start, st.planned_end, " +
            "st.planned_minutes, st.actual_minutes, st.status, st.created_at, " +
            "t.name AS topic_name, t.priority AS topic_priority, t.parent_topic_id AS topic_parent_id " +
            "FROM study_tasks st " +
            "JOIN topics t ON st.topic_id = t.id " +
            "WHERE st.plan_id = ? " +
            "ORDER BY st.planned_start ASC, st.id ASC";

    private static final String SQL_FIND_BY_ID =
            "SELECT st.id, st.plan_id, st.topic_id, st.planned_start, st.planned_end, " +
            "st.planned_minutes, st.actual_minutes, st.status, st.created_at, " +
            "t.name AS topic_name, t.priority AS topic_priority, t.parent_topic_id AS topic_parent_id " +
            "FROM study_tasks st " +
            "JOIN topics t ON st.topic_id = t.id " +
            "WHERE st.id = ?";

    private static final String SQL_UPDATE =
            "UPDATE study_tasks SET planned_start = ?, planned_end = ?, planned_minutes = ?, " +
            "actual_minutes = ?, status = ? WHERE id = ?";

    @Override
    public void saveAll(Connection conn, List<StudyTask> tasks) {
        if (conn == null) {
            throw new IllegalArgumentException("Connection cannot be null for transactional saveAll.");
        }
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            for (StudyTask task : tasks) {
                stmt.setLong(1, task.getPlanId());
                stmt.setLong(2, task.getTopicId());
                stmt.setTimestamp(3, Timestamp.valueOf(task.getPlannedStart()));
                stmt.setTimestamp(4, Timestamp.valueOf(task.getPlannedEnd()));
                stmt.setInt(5, task.getPlannedMinutes());
                stmt.setInt(6, task.getActualMinutes() != null ? task.getActualMinutes() : 0);
                stmt.setString(7, task.getStatus() != null ? task.getStatus() : StudyTask.STATUS_PLANNED);
                stmt.setTimestamp(8, Timestamp.valueOf(task.getCreatedAt() != null ? task.getCreatedAt() : now));
                stmt.addBatch();
            }

            stmt.executeBatch();

            // Retrieve generated keys
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                int index = 0;
                while (rs.next() && index < tasks.size()) {
                    tasks.get(index).setId(rs.getLong(1));
                    tasks.get(index).setCreatedAt(now);
                    index++;
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error occurred while batch saving study tasks: " + e.getMessage(), e);
        }
    }

    @Override
    public List<StudyTask> findAllByPlanId(Long planId) {
        if (planId == null) {
            return List.of();
        }

        List<StudyTask> tasks = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL_BY_PLAN_ID)) {

            stmt.setLong(1, planId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
            return tasks;
        } catch (SQLException e) {
            throw new DataAccessException("Database error occurred while finding study tasks for plan #" + planId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<StudyTask> findById(Long taskId) {
        if (taskId == null) {
            return Optional.empty();
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            stmt.setLong(1, taskId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTask(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Database error occurred while finding study task #" + taskId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(StudyTask task) {
        if (task == null || task.getId() == null) {
            return false;
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setTimestamp(1, Timestamp.valueOf(task.getPlannedStart()));
            stmt.setTimestamp(2, Timestamp.valueOf(task.getPlannedEnd()));
            stmt.setInt(3, task.getPlannedMinutes());
            stmt.setInt(4, task.getActualMinutes() != null ? task.getActualMinutes() : 0);
            stmt.setString(5, task.getStatus());
            stmt.setLong(6, task.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Database error occurred while updating study task #" + task.getId() + ": " + e.getMessage(), e);
        }
    }

    private StudyTask mapResultSetToTask(ResultSet rs) throws SQLException {
        StudyTask task = new StudyTask();
        task.setId(rs.getLong("id"));
        task.setPlanId(rs.getLong("plan_id"));
        task.setTopicId(rs.getLong("topic_id"));

        Timestamp startTs = rs.getTimestamp("planned_start");
        if (startTs != null) {
            task.setPlannedStart(startTs.toLocalDateTime());
        }

        Timestamp endTs = rs.getTimestamp("planned_end");
        if (endTs != null) {
            task.setPlannedEnd(endTs.toLocalDateTime());
        }

        task.setPlannedMinutes(rs.getInt("planned_minutes"));
        task.setActualMinutes(rs.getInt("actual_minutes"));
        task.setStatus(rs.getString("status"));

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            task.setCreatedAt(createdTs.toLocalDateTime());
        }

        task.setTopicName(rs.getString("topic_name"));
        task.setTopicPriority(rs.getString("topic_priority"));
        long parentId = rs.getLong("topic_parent_id");
        task.setTopicIsSubtopic(!rs.wasNull() && parentId > 0);

        return task;
    }
}
