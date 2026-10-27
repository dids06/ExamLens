package com.examlens.dao;

import com.examlens.model.AiJob;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AiJobDao {

    public long create(long userId, Long examId, String actionKey, String promptVersion, String provider,
                       String model, int inputChars) throws SQLException {
        return Sql.insert("""
                INSERT INTO ai_jobs (user_id, exam_id, action_key, prompt_version, provider, model, status, input_chars, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 'RUNNING', ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, userId);
            Sql.setLong(ps, 2, examId);
            ps.setString(3, actionKey);
            ps.setString(4, promptVersion);
            ps.setString(5, provider);
            Sql.setString(ps, 6, model);
            ps.setInt(7, inputChars);
        });
    }

    public void markSucceeded(long jobId, String rawResponse, int attempts, int corrections, long durationMs) throws SQLException {
        Sql.update("""
                UPDATE ai_jobs SET status = 'SUCCEEDED', raw_response = ?, attempt_count = ?, correction_attempts = ?,
                  duration_ms = ?, finished_at = CURRENT_TIMESTAMP, error_code = NULL, error_message = NULL
                WHERE id = ?
                """, ps -> {
            ps.setString(1, truncate(rawResponse, 60000));
            ps.setInt(2, attempts);
            ps.setInt(3, corrections);
            ps.setLong(4, durationMs);
            ps.setLong(5, jobId);
        });
    }

    public void markFailed(long jobId, String errorCode, String errorMessage, int attempts, int corrections, long durationMs) throws SQLException {
        Sql.update("""
                UPDATE ai_jobs SET status = 'FAILED', error_code = ?, error_message = ?, attempt_count = ?,
                  correction_attempts = ?, duration_ms = ?, finished_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, ps -> {
            ps.setString(1, truncate(errorCode, 60));
            ps.setString(2, truncate(errorMessage, 500));
            ps.setInt(3, attempts);
            ps.setInt(4, corrections);
            ps.setLong(5, durationMs);
            ps.setLong(6, jobId);
        });
    }

    public Optional<AiJob> find(long id, long userId) throws SQLException {
        return Sql.one("SELECT * FROM ai_jobs WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        }, this::map);
    }

    public List<AiJob> recentForExam(long examId, long userId, int limit) throws SQLException {
        return Sql.list("""
                SELECT * FROM ai_jobs WHERE exam_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT ?
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
        }, this::map);
    }

    public List<AiJob> recentForUser(long userId, int limit) throws SQLException {
        return Sql.list("SELECT * FROM ai_jobs WHERE user_id = ? ORDER BY created_at DESC LIMIT ?", ps -> {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
        }, this::map);
    }

    public int countSince(long userId, String actionKey, LocalDateTime since) throws SQLException {
        List<Integer> rows = Sql.list("""
                SELECT COUNT(*) AS c FROM ai_jobs
                WHERE user_id = ? AND action_key = ? AND created_at >= ? AND status IN ('RUNNING','SUCCEEDED')
                """, ps -> {
            ps.setLong(1, userId);
            ps.setString(2, actionKey);
            Sql.setTimestamp(ps, 3, since);
        }, rs -> rs.getInt("c"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    private AiJob map(java.sql.ResultSet rs) throws SQLException {
        AiJob j = new AiJob();
        j.setId(rs.getLong("id"));
        j.setUserId(rs.getLong("user_id"));
        j.setExamId(Sql.getLong(rs, "exam_id"));
        j.setActionKey(rs.getString("action_key"));
        j.setPromptVersion(rs.getString("prompt_version"));
        j.setProvider(rs.getString("provider"));
        j.setModel(rs.getString("model"));
        j.setStatus(rs.getString("status"));
        j.setAttemptCount(rs.getInt("attempt_count"));
        j.setCorrectionAttempts(rs.getInt("correction_attempts"));
        j.setDurationMs(Sql.getLong(rs, "duration_ms"));
        j.setInputChars(Sql.getInt(rs, "input_chars"));
        j.setErrorCode(rs.getString("error_code"));
        j.setErrorMessage(rs.getString("error_message"));
        j.setRawResponse(rs.getString("raw_response"));
        j.setCreatedAt(Sql.getDateTime(rs, "created_at"));
        j.setFinishedAt(Sql.getDateTime(rs, "finished_at"));
        return j;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
