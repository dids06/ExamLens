package com.examlens.dao;

import com.examlens.model.Exam;
import com.examlens.model.ExamAvailability;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ExamDao {

    public long insert(long userId, String subjectName, LocalDateTime examDatetime, String timezone,
                       Integer targetMarks, Integer totalMarks, String notes) throws SQLException {
        return Sql.insert("""
                INSERT INTO exams (user_id, subject_name, exam_datetime, timezone, target_marks, total_marks, notes, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, userId);
            ps.setString(2, subjectName);
            Sql.setTimestamp(ps, 3, examDatetime);
            ps.setString(4, timezone);
            Sql.setInt(ps, 5, targetMarks);
            Sql.setInt(ps, 6, totalMarks);
            Sql.setString(ps, 7, notes);
        });
    }

    public void update(long id, long userId, String subjectName, LocalDateTime examDatetime, String timezone,
                       Integer targetMarks, Integer totalMarks, String notes, String status) throws SQLException {
        Sql.update("""
                UPDATE exams SET subject_name = ?, exam_datetime = ?, timezone = ?, target_marks = ?, total_marks = ?,
                  notes = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """, ps -> {
            ps.setString(1, subjectName);
            Sql.setTimestamp(ps, 2, examDatetime);
            ps.setString(3, timezone);
            Sql.setInt(ps, 4, targetMarks);
            Sql.setInt(ps, 5, totalMarks);
            Sql.setString(ps, 6, notes);
            ps.setString(7, status);
            ps.setLong(8, id);
            ps.setLong(9, userId);
        });
    }

    public Optional<Exam> find(long id, long userId) throws SQLException {
        return Sql.one("SELECT * FROM exams WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        }, this::map);
    }

    public List<Exam> listForUser(long userId) throws SQLException {
        return Sql.list("SELECT * FROM exams WHERE user_id = ? ORDER BY exam_datetime ASC", ps -> ps.setLong(1, userId), this::map);
    }

    public Optional<Exam> nextExam(long userId) throws SQLException {
        return Sql.one("""
                SELECT * FROM exams WHERE user_id = ? AND status = 'ACTIVE' AND exam_datetime >= CURRENT_TIMESTAMP
                ORDER BY exam_datetime ASC
                """, ps -> ps.setLong(1, userId), this::map);
    }

    public void delete(long id, long userId) throws SQLException {
        Sql.update("DELETE FROM exams WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        });
    }

    public void addAvailability(long examId, Integer dayOfWeek, java.time.LocalDate specificDate, int minutes, String label)
            throws SQLException {
        Sql.insert("""
                INSERT INTO exam_availability (exam_id, day_of_week, specific_date, available_minutes, label)
                VALUES (?, ?, ?, ?, ?)
                """, ps -> {
            ps.setLong(1, examId);
            Sql.setInt(ps, 2, dayOfWeek);
            Sql.setDate(ps, 3, specificDate);
            ps.setInt(4, minutes);
            Sql.setString(ps, 5, label);
        });
    }

    public void clearAvailability(long examId) throws SQLException {
        Sql.update("DELETE FROM exam_availability WHERE exam_id = ?", ps -> ps.setLong(1, examId));
    }

    public List<ExamAvailability> availability(long examId) throws SQLException {
        return Sql.list("""
                SELECT * FROM exam_availability WHERE exam_id = ? ORDER BY day_of_week, specific_date
                """, ps -> ps.setLong(1, examId), rs -> {
            ExamAvailability a = new ExamAvailability();
            a.setId(rs.getLong("id"));
            a.setExamId(rs.getLong("exam_id"));
            a.setDayOfWeek(Sql.getInt(rs, "day_of_week"));
            a.setSpecificDate(Sql.getDate(rs, "specific_date"));
            a.setAvailableMinutes(rs.getInt("available_minutes"));
            a.setLabel(rs.getString("label"));
            return a;
        });
    }

    public int countForUser(long userId) throws SQLException {
        List<Integer> rows = Sql.list("SELECT COUNT(*) AS c FROM exams WHERE user_id = ?", ps -> ps.setLong(1, userId),
                rs -> rs.getInt("c"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    private Exam map(java.sql.ResultSet rs) throws SQLException {
        Exam e = new Exam();
        e.setId(rs.getLong("id"));
        e.setUserId(rs.getLong("user_id"));
        e.setSubjectName(rs.getString("subject_name"));
        e.setExamDatetime(Sql.getDateTime(rs, "exam_datetime"));
        e.setTimezone(rs.getString("timezone"));
        e.setTargetMarks(Sql.getInt(rs, "target_marks"));
        e.setTotalMarks(Sql.getInt(rs, "total_marks"));
        e.setNotes(rs.getString("notes"));
        e.setStatus(rs.getString("status"));
        e.setCreatedAt(Sql.getDateTime(rs, "created_at"));
        e.setUpdatedAt(Sql.getDateTime(rs, "updated_at"));
        return e;
    }
}
