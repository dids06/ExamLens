package com.examlens.dao;

import com.examlens.model.SyllabusClassification;
import com.examlens.model.SyllabusVersion;
import com.examlens.model.Topic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SyllabusDao {

    // ------------------------------------------------------------ versions

    public int nextVersionNumber(long examId) throws SQLException {
        List<Integer> rows = Sql.list("SELECT COALESCE(MAX(version_number), 0) AS v FROM syllabus_versions WHERE exam_id = ?",
                ps -> ps.setLong(1, examId), rs -> rs.getInt("v"));
        return (rows.isEmpty() ? 0 : rows.get(0)) + 1;
    }

    public long insertVersion(long examId, long userId, int versionNumber, String status, Long sourceDocumentId,
                              Long aiJobId, String summary, String warnings) throws SQLException {
        return Sql.insert("""
                INSERT INTO syllabus_versions (exam_id, user_id, version_number, status, source_document_id, ai_job_id,
                  summary, warnings, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            ps.setInt(3, versionNumber);
            ps.setString(4, status);
            Sql.setLong(ps, 5, sourceDocumentId);
            Sql.setLong(ps, 6, aiJobId);
            Sql.setString(ps, 7, summary);
            Sql.setString(ps, 8, warnings);
        });
    }

    public Optional<SyllabusVersion> findVersion(long id, long userId) throws SQLException {
        return Sql.one("SELECT * FROM syllabus_versions WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        }, this::mapVersion);
    }

    public Optional<SyllabusVersion> latestVersion(long examId, long userId, String status) throws SQLException {
        if (status == null) {
            return Sql.one("""
                    SELECT * FROM syllabus_versions WHERE exam_id = ? AND user_id = ?
                    ORDER BY version_number DESC LIMIT 1
                    """, ps -> {
                ps.setLong(1, examId);
                ps.setLong(2, userId);
            }, this::mapVersion);
        }
        return Sql.one("""
                SELECT * FROM syllabus_versions WHERE exam_id = ? AND user_id = ? AND status = ?
                ORDER BY version_number DESC LIMIT 1
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            ps.setString(3, status);
        }, this::mapVersion);
    }

    public List<SyllabusVersion> versions(long examId, long userId) throws SQLException {
        return Sql.list("""
                SELECT * FROM syllabus_versions WHERE exam_id = ? AND user_id = ? ORDER BY version_number DESC
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, this::mapVersion);
    }

    public void confirmVersion(long id, long userId) throws SQLException {
        Sql.update("""
                UPDATE syllabus_versions SET status = 'CONFIRMED', confirmed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """, ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        });
    }

    // ------------------------------------------------------------ topics

    public long insertTopic(Topic t) throws SQLException {
        return Sql.insert("""
                INSERT INTO topics (syllabus_version_id, exam_id, parent_topic_id, unit_name, title, subtopics,
                  learning_outcomes, importance, difficulty, estimated_minutes, source_pages, confidence, needs_review,
                  ai_reason, display_order, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, t.getSyllabusVersionId());
            ps.setLong(2, t.getExamId());
            Sql.setLong(ps, 3, t.getParentTopicId());
            Sql.setString(ps, 4, t.getUnitName());
            ps.setString(5, t.getTitle());
            Sql.setString(ps, 6, t.getSubtopics());
            Sql.setString(ps, 7, t.getLearningOutcomes());
            Sql.setString(ps, 8, t.getImportance());
            Sql.setString(ps, 9, t.getDifficulty());
            Sql.setInt(ps, 10, t.getEstimatedMinutes());
            Sql.setString(ps, 11, t.getSourcePages());
            Sql.setString(ps, 12, t.getConfidence());
            ps.setBoolean(13, t.isNeedsReview());
            Sql.setString(ps, 14, t.getAiReason());
            ps.setInt(15, t.getDisplayOrder());
        });
    }

    public void updateTopic(long id, long examId, long userId, String title, String unitName, String subtopics,
                            String learningOutcomes, String importance, String difficulty, Integer estimatedMinutes,
                            int displayOrder, boolean needsReview) throws SQLException {
        Sql.update("""
                UPDATE topics t SET t.title = ?, t.unit_name = ?, t.subtopics = ?, t.learning_outcomes = ?,
                  t.importance = ?, t.difficulty = ?, t.estimated_minutes = ?, t.display_order = ?, t.needs_review = ?
                WHERE t.id = ? AND t.exam_id = ?
                  AND EXISTS (SELECT 1 FROM exams e WHERE e.id = t.exam_id AND e.user_id = ?)
                """, ps -> {
            ps.setString(1, title);
            Sql.setString(ps, 2, unitName);
            Sql.setString(ps, 3, subtopics);
            Sql.setString(ps, 4, learningOutcomes);
            Sql.setString(ps, 5, importance);
            Sql.setString(ps, 6, difficulty);
            Sql.setInt(ps, 7, estimatedMinutes);
            ps.setInt(8, displayOrder);
            ps.setBoolean(9, needsReview);
            ps.setLong(10, id);
            ps.setLong(11, examId);
            ps.setLong(12, userId);
        });
    }

    public void deleteTopic(long id, long examId, long userId) throws SQLException {
        Sql.update("""
                DELETE FROM topics WHERE id = ? AND exam_id = ?
                  AND EXISTS (SELECT 1 FROM exams e WHERE e.id = topics.exam_id AND e.user_id = ?)
                """, ps -> {
            ps.setLong(1, id);
            ps.setLong(2, examId);
            ps.setLong(3, userId);
        });
    }

    public Optional<Topic> findTopic(long id, long userId) throws SQLException {
        return Sql.one("""
                SELECT t.* FROM topics t JOIN exams e ON e.id = t.exam_id
                WHERE t.id = ? AND e.user_id = ?
                """, ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        }, this::mapTopic);
    }

    public List<Topic> topicsForVersion(long versionId, long userId) throws SQLException {
        return Sql.list("""
                SELECT t.* FROM topics t JOIN exams e ON e.id = t.exam_id
                WHERE t.syllabus_version_id = ? AND e.user_id = ?
                ORDER BY t.display_order, t.id
                """, ps -> {
            ps.setLong(1, versionId);
            ps.setLong(2, userId);
        }, this::mapTopic);
    }

    public List<Topic> confirmedTopics(long examId, long userId) throws SQLException {
        return Sql.list("""
                SELECT t.* FROM topics t
                JOIN syllabus_versions v ON v.id = t.syllabus_version_id
                JOIN exams e ON e.id = t.exam_id
                WHERE t.exam_id = ? AND e.user_id = ? AND v.status = 'CONFIRMED'
                ORDER BY t.display_order, t.id
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, this::mapTopic);
    }

    public Optional<Long> confirmedVersionId(long examId, long userId) throws SQLException {
        List<Long> rows = Sql.list("""
                SELECT id FROM syllabus_versions WHERE exam_id = ? AND user_id = ? AND status = 'CONFIRMED'
                ORDER BY version_number DESC LIMIT 1
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> rs.getLong("id"));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public int maxDisplayOrder(long versionId) throws SQLException {
        List<Integer> rows = Sql.list("SELECT COALESCE(MAX(display_order), 0) AS m FROM topics WHERE syllabus_version_id = ?",
                ps -> ps.setLong(1, versionId), rs -> rs.getInt("m"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    public int countConfirmedTopics(long examId, long userId) throws SQLException {
        List<Integer> rows = Sql.list("""
                SELECT COUNT(*) AS c FROM topics t
                JOIN syllabus_versions v ON v.id = t.syllabus_version_id
                JOIN exams e ON e.id = t.exam_id
                WHERE t.exam_id = ? AND e.user_id = ? AND v.status = 'CONFIRMED'
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> rs.getInt("c"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    /** Existing topicId -> title map for the confirmed syllabus, used for AI reference checks. */
    public java.util.Map<Long, String> confirmedTopicTitles(long examId, long userId) throws SQLException {
        java.util.Map<Long, String> map = new java.util.LinkedHashMap<>();
        for (Topic t : confirmedTopics(examId, userId)) {
            map.put(t.getId(), t.getTitle());
        }
        return map;
    }

    // ------------------------------------------------------------ classifications

    public void insertClassification(SyllabusClassification c) throws SQLException {
        Sql.insert("""
                INSERT INTO syllabus_classifications (syllabus_version_id, block_key, page_number, original_text,
                  classification, confidence, normalized_topic, parent_unit, reason, included)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setLong(1, c.getSyllabusVersionId());
            Sql.setString(ps, 2, c.getBlockKey());
            Sql.setInt(ps, 3, c.getPageNumber());
            ps.setString(4, c.getOriginalText());
            ps.setString(5, c.getClassification());
            if (c.getConfidence() == null) {
                ps.setNull(6, java.sql.Types.DECIMAL);
            } else {
                ps.setDouble(6, c.getConfidence());
            }
            Sql.setString(ps, 7, c.getNormalizedTopic());
            Sql.setString(ps, 8, c.getParentUnit());
            Sql.setString(ps, 9, c.getReason());
            ps.setBoolean(10, c.isIncluded());
        });
    }

    public List<SyllabusClassification> classifications(long versionId, long userId) throws SQLException {
        return Sql.list("""
                SELECT c.* FROM syllabus_classifications c
                JOIN syllabus_versions v ON v.id = c.syllabus_version_id
                WHERE c.syllabus_version_id = ? AND v.user_id = ?
                ORDER BY c.page_number, c.id
                """, ps -> {
            ps.setLong(1, versionId);
            ps.setLong(2, userId);
        }, rs -> {
            SyllabusClassification c = new SyllabusClassification();
            c.setId(rs.getLong("id"));
            c.setSyllabusVersionId(rs.getLong("syllabus_version_id"));
            c.setBlockKey(rs.getString("block_key"));
            c.setPageNumber(Sql.getInt(rs, "page_number"));
            c.setOriginalText(rs.getString("original_text"));
            c.setClassification(rs.getString("classification"));
            c.setConfidence(Sql.getDouble(rs, "confidence"));
            c.setNormalizedTopic(rs.getString("normalized_topic"));
            c.setParentUnit(rs.getString("parent_unit"));
            c.setReason(rs.getString("reason"));
            c.setIncluded(rs.getBoolean("included"));
            return c;
        });
    }

    public void deleteTopicsForVersion(long versionId) throws SQLException {
        Sql.update("DELETE FROM topics WHERE syllabus_version_id = ?", ps -> ps.setLong(1, versionId));
    }

    public void deleteClassificationsForVersion(long versionId) throws SQLException {
        Sql.update("DELETE FROM syllabus_classifications WHERE syllabus_version_id = ?", ps -> ps.setLong(1, versionId));
    }

    // ------------------------------------------------------------ mapping

    private SyllabusVersion mapVersion(java.sql.ResultSet rs) throws SQLException {
        SyllabusVersion v = new SyllabusVersion();
        v.setId(rs.getLong("id"));
        v.setExamId(rs.getLong("exam_id"));
        v.setUserId(rs.getLong("user_id"));
        v.setVersionNumber(rs.getInt("version_number"));
        v.setStatus(rs.getString("status"));
        v.setSourceDocumentId(Sql.getLong(rs, "source_document_id"));
        v.setAiJobId(Sql.getLong(rs, "ai_job_id"));
        v.setSummary(rs.getString("summary"));
        v.setWarnings(rs.getString("warnings"));
        v.setCreatedAt(Sql.getDateTime(rs, "created_at"));
        v.setConfirmedAt(Sql.getDateTime(rs, "confirmed_at"));
        return v;
    }

    private Topic mapTopic(java.sql.ResultSet rs) throws SQLException {
        Topic t = new Topic();
        t.setId(rs.getLong("id"));
        t.setSyllabusVersionId(rs.getLong("syllabus_version_id"));
        t.setExamId(rs.getLong("exam_id"));
        t.setParentTopicId(Sql.getLong(rs, "parent_topic_id"));
        t.setUnitName(rs.getString("unit_name"));
        t.setTitle(rs.getString("title"));
        t.setSubtopics(rs.getString("subtopics"));
        t.setLearningOutcomes(rs.getString("learning_outcomes"));
        t.setImportance(rs.getString("importance"));
        t.setDifficulty(rs.getString("difficulty"));
        t.setEstimatedMinutes(Sql.getInt(rs, "estimated_minutes"));
        t.setSourcePages(rs.getString("source_pages"));
        t.setConfidence(rs.getString("confidence"));
        t.setNeedsReview(rs.getBoolean("needs_review"));
        t.setAiReason(rs.getString("ai_reason"));
        t.setDisplayOrder(rs.getInt("display_order"));
        return t;
    }

    public List<Topic> topicsByIds(List<Long> ids, long examId, long userId) throws SQLException {
        List<Topic> out = new ArrayList<>();
        for (Long id : ids) {
            findTopic(id, userId).filter(t -> t.getExamId() == examId).ifPresent(out::add);
        }
        return out;
    }
}
