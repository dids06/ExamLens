package com.examlens.dao;

import com.examlens.model.ConceptExplanation;
import com.examlens.model.Conversation;
import com.examlens.model.ConversationMessage;
import com.examlens.model.MaterialIndex;
import com.examlens.model.PreviousPaper;
import com.examlens.model.PyqAnalysis;
import com.examlens.model.PyqQuestion;
import com.examlens.model.ReadinessAssessment;
import com.examlens.model.RevisionResource;
import com.examlens.model.SourceComparison;
import com.examlens.model.TeacherGuidance;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class LearningDao {

    // ------------------------------------------------------------ teacher guidance

    public long insertTeacherGuidance(TeacherGuidance g) throws SQLException {
        return Sql.insert("""
                INSERT INTO teacher_guidance (exam_id, user_id, document_id, ai_job_id, hints, important_topics,
                  instructions, exam_format, uncertainties, confirmed, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, g.getExamId());
            ps.setLong(2, g.getUserId());
            Sql.setLong(ps, 3, g.getDocumentId());
            Sql.setLong(ps, 4, g.getAiJobId());
            Sql.setString(ps, 5, g.getHints());
            Sql.setString(ps, 6, g.getImportantTopics());
            Sql.setString(ps, 7, g.getInstructions());
            Sql.setString(ps, 8, g.getExamFormat());
            Sql.setString(ps, 9, g.getUncertainties());
            ps.setBoolean(10, g.isConfirmed());
        });
    }

    public void confirmTeacherGuidance(long id, long userId) throws SQLException {
        Sql.update("UPDATE teacher_guidance SET confirmed = TRUE WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        });
    }

    public Optional<TeacherGuidance> latestTeacherGuidance(long examId, long userId) throws SQLException {
        return Sql.one("""
                SELECT * FROM teacher_guidance WHERE exam_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT 1
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> {
            TeacherGuidance g = new TeacherGuidance();
            g.setId(rs.getLong("id"));
            g.setExamId(rs.getLong("exam_id"));
            g.setUserId(rs.getLong("user_id"));
            g.setDocumentId(Sql.getLong(rs, "document_id"));
            g.setAiJobId(Sql.getLong(rs, "ai_job_id"));
            g.setHints(rs.getString("hints"));
            g.setImportantTopics(rs.getString("important_topics"));
            g.setInstructions(rs.getString("instructions"));
            g.setExamFormat(rs.getString("exam_format"));
            g.setUncertainties(rs.getString("uncertainties"));
            g.setConfirmed(rs.getBoolean("confirmed"));
            g.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            return g;
        });
    }

    // ------------------------------------------------------------ material index

    public long upsertMaterialIndex(MaterialIndex m) throws SQLException {
        Optional<Long> existing = Sql.one("SELECT id FROM material_indexes WHERE document_id = ?",
                ps -> ps.setLong(1, m.getDocumentId()), rs -> rs.getLong("id"));
        if (existing.isPresent()) {
            Sql.update("""
                    UPDATE material_indexes SET ai_job_id = ?, sections_json = ?, unmapped_topics = ?, warnings = ?,
                      created_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """, ps -> {
                Sql.setLong(ps, 1, m.getAiJobId());
                Sql.setString(ps, 2, m.getSectionsJson());
                Sql.setString(ps, 3, m.getUnmappedTopics());
                Sql.setString(ps, 4, m.getWarnings());
                ps.setLong(5, existing.get());
            });
            return existing.get();
        }
        return Sql.insert("""
                INSERT INTO material_indexes (exam_id, user_id, document_id, ai_job_id, sections_json, unmapped_topics,
                  warnings, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, m.getExamId());
            ps.setLong(2, m.getUserId());
            ps.setLong(3, m.getDocumentId());
            Sql.setLong(ps, 4, m.getAiJobId());
            Sql.setString(ps, 5, m.getSectionsJson());
            Sql.setString(ps, 6, m.getUnmappedTopics());
            Sql.setString(ps, 7, m.getWarnings());
        });
    }

    public List<MaterialIndex> materialIndexes(long examId, long userId) throws SQLException {
        return Sql.list("""
                SELECT mi.*, d.original_filename AS doc_name FROM material_indexes mi
                JOIN documents d ON d.id = mi.document_id
                WHERE mi.exam_id = ? AND mi.user_id = ? ORDER BY mi.created_at DESC
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> {
            MaterialIndex m = new MaterialIndex();
            m.setId(rs.getLong("id"));
            m.setExamId(rs.getLong("exam_id"));
            m.setUserId(rs.getLong("user_id"));
            m.setDocumentId(rs.getLong("document_id"));
            m.setAiJobId(Sql.getLong(rs, "ai_job_id"));
            m.setSectionsJson(rs.getString("sections_json"));
            m.setUnmappedTopics(rs.getString("unmapped_topics"));
            m.setWarnings(rs.getString("warnings"));
            m.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            m.setDocumentName(rs.getString("doc_name"));
            return m;
        });
    }

    // ------------------------------------------------------------ source comparison

    public long insertSourceComparison(SourceComparison c) throws SQLException {
        return Sql.insert("""
                INSERT INTO source_comparisons (exam_id, user_id, ai_job_id, recommendations_json, uncovered_topics,
                  warnings, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, c.getExamId());
            ps.setLong(2, c.getUserId());
            Sql.setLong(ps, 3, c.getAiJobId());
            Sql.setString(ps, 4, c.getRecommendationsJson());
            Sql.setString(ps, 5, c.getUncoveredTopics());
            Sql.setString(ps, 6, c.getWarnings());
        });
    }

    public Optional<SourceComparison> latestSourceComparison(long examId, long userId) throws SQLException {
        return Sql.one("""
                SELECT * FROM source_comparisons WHERE exam_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT 1
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> {
            SourceComparison c = new SourceComparison();
            c.setId(rs.getLong("id"));
            c.setExamId(rs.getLong("exam_id"));
            c.setUserId(rs.getLong("user_id"));
            c.setAiJobId(Sql.getLong(rs, "ai_job_id"));
            c.setRecommendationsJson(rs.getString("recommendations_json"));
            c.setUncoveredTopics(rs.getString("uncovered_topics"));
            c.setWarnings(rs.getString("warnings"));
            c.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            return c;
        });
    }

    // ------------------------------------------------------------ previous papers

    public long insertPaper(long examId, long userId, long documentId, String label, String year, Integer totalMarks)
            throws SQLException {
        return Sql.insert("""
                INSERT INTO previous_papers (exam_id, user_id, document_id, paper_label, exam_year, total_marks, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            ps.setLong(3, documentId);
            ps.setString(4, label);
            Sql.setString(ps, 5, year);
            Sql.setInt(ps, 6, totalMarks);
        });
    }

    public List<PreviousPaper> previousPapers(long examId, long userId) throws SQLException {
        List<PreviousPaper> papers = Sql.list("""
                SELECT pp.*, d.original_filename AS doc_name FROM previous_papers pp
                JOIN documents d ON d.id = pp.document_id
                WHERE pp.exam_id = ? AND pp.user_id = ? ORDER BY pp.created_at DESC
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> {
            PreviousPaper p = new PreviousPaper();
            p.setId(rs.getLong("id"));
            p.setExamId(rs.getLong("exam_id"));
            p.setUserId(rs.getLong("user_id"));
            p.setDocumentId(rs.getLong("document_id"));
            p.setPaperLabel(rs.getString("paper_label"));
            p.setExamYear(rs.getString("exam_year"));
            p.setTotalMarks(Sql.getInt(rs, "total_marks"));
            p.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            p.setDocumentName(rs.getString("doc_name"));
            return p;
        });
        for (PreviousPaper p : papers) {
            p.setQuestionCount(countPyqQuestions(p.getId()));
        }
        return papers;
    }

    public Optional<PreviousPaper> findPaper(long paperId, long userId) throws SQLException {
        return Sql.one("SELECT * FROM previous_papers WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, paperId);
            ps.setLong(2, userId);
        }, rs -> {
            PreviousPaper p = new PreviousPaper();
            p.setId(rs.getLong("id"));
            p.setExamId(rs.getLong("exam_id"));
            p.setUserId(rs.getLong("user_id"));
            p.setDocumentId(rs.getLong("document_id"));
            p.setPaperLabel(rs.getString("paper_label"));
            p.setExamYear(rs.getString("exam_year"));
            p.setTotalMarks(Sql.getInt(rs, "total_marks"));
            p.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            return p;
        });
    }

    public void replacePyqQuestions(long paperId, List<PyqQuestion> questions) throws SQLException {
        Sql.transaction(c -> {
            try (java.sql.PreparedStatement del = c.prepareStatement("DELETE FROM pyq_questions WHERE paper_id = ?")) {
                del.setLong(1, paperId);
                del.executeUpdate();
            }
            try (java.sql.PreparedStatement ins = c.prepareStatement("""
                    INSERT INTO pyq_questions (paper_id, question_key, question_text, marks, page_number, matched_topic)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                for (PyqQuestion q : questions) {
                    ins.setLong(1, paperId);
                    ins.setString(2, q.getQuestionKey());
                    ins.setString(3, q.getQuestionText());
                    Sql.setInt(ins, 4, q.getMarks());
                    Sql.setInt(ins, 5, q.getPageNumber());
                    Sql.setString(ins, 6, q.getMatchedTopic());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            return null;
        });
    }

    public List<PyqQuestion> pyqQuestions(long paperId) throws SQLException {
        return Sql.list("SELECT * FROM pyq_questions WHERE paper_id = ? ORDER BY id", ps -> ps.setLong(1, paperId), rs -> {
            PyqQuestion q = new PyqQuestion();
            q.setId(rs.getLong("id"));
            q.setPaperId(rs.getLong("paper_id"));
            q.setQuestionKey(rs.getString("question_key"));
            q.setQuestionText(rs.getString("question_text"));
            q.setMarks(Sql.getInt(rs, "marks"));
            q.setPageNumber(Sql.getInt(rs, "page_number"));
            q.setMatchedTopic(rs.getString("matched_topic"));
            return q;
        });
    }

    public int countPyqQuestions(long paperId) throws SQLException {
        List<Integer> rows = Sql.list("SELECT COUNT(*) AS c FROM pyq_questions WHERE paper_id = ?",
                ps -> ps.setLong(1, paperId), rs -> rs.getInt("c"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    public long insertPyqAnalysis(PyqAnalysis a) throws SQLException {
        return Sql.insert("""
                INSERT INTO pyq_analyses (exam_id, user_id, ai_job_id, topic_frequency_json, recurring_patterns,
                  unmatched_questions, limitations, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, a.getExamId());
            ps.setLong(2, a.getUserId());
            Sql.setLong(ps, 3, a.getAiJobId());
            Sql.setString(ps, 4, a.getTopicFrequencyJson());
            Sql.setString(ps, 5, a.getRecurringPatterns());
            Sql.setString(ps, 6, a.getUnmatchedQuestions());
            Sql.setString(ps, 7, a.getLimitations());
        });
    }

    public Optional<PyqAnalysis> latestPyqAnalysis(long examId, long userId) throws SQLException {
        return Sql.one("""
                SELECT * FROM pyq_analyses WHERE exam_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT 1
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> {
            PyqAnalysis a = new PyqAnalysis();
            a.setId(rs.getLong("id"));
            a.setExamId(rs.getLong("exam_id"));
            a.setUserId(rs.getLong("user_id"));
            a.setAiJobId(Sql.getLong(rs, "ai_job_id"));
            a.setTopicFrequencyJson(rs.getString("topic_frequency_json"));
            a.setRecurringPatterns(rs.getString("recurring_patterns"));
            a.setUnmatchedQuestions(rs.getString("unmatched_questions"));
            a.setLimitations(rs.getString("limitations"));
            a.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            return a;
        });
    }

    // ------------------------------------------------------------ revision resources

    public long insertRevisionResource(RevisionResource r) throws SQLException {
        return Sql.insert("""
                INSERT INTO revision_resources (exam_id, user_id, ai_job_id, topic_title, topic_id, key_points,
                  flashcards_json, recall_json, source_references, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, r.getExamId());
            ps.setLong(2, r.getUserId());
            Sql.setLong(ps, 3, r.getAiJobId());
            ps.setString(4, r.getTopicTitle());
            Sql.setLong(ps, 5, r.getTopicId());
            Sql.setString(ps, 6, r.getKeyPoints());
            Sql.setString(ps, 7, r.getFlashcardsJson());
            Sql.setString(ps, 8, r.getRecallJson());
            Sql.setString(ps, 9, r.getSourceReferences());
        });
    }

    public List<RevisionResource> revisionResources(long examId, long userId) throws SQLException {
        return Sql.list("""
                SELECT * FROM revision_resources WHERE exam_id = ? AND user_id = ? ORDER BY created_at DESC
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> {
            RevisionResource r = new RevisionResource();
            r.setId(rs.getLong("id"));
            r.setExamId(rs.getLong("exam_id"));
            r.setUserId(rs.getLong("user_id"));
            r.setAiJobId(Sql.getLong(rs, "ai_job_id"));
            r.setTopicTitle(rs.getString("topic_title"));
            r.setTopicId(Sql.getLong(rs, "topic_id"));
            r.setKeyPoints(rs.getString("key_points"));
            r.setFlashcardsJson(rs.getString("flashcards_json"));
            r.setRecallJson(rs.getString("recall_json"));
            r.setSourceReferences(rs.getString("source_references"));
            r.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            return r;
        });
    }

    public int countRevisionResources(long examId, long userId) throws SQLException {
        List<Integer> rows = Sql.list("SELECT COUNT(*) AS c FROM revision_resources WHERE exam_id = ? AND user_id = ?",
                ps -> {
                    ps.setLong(1, examId);
                    ps.setLong(2, userId);
                }, rs -> rs.getInt("c"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    // ------------------------------------------------------------ readiness

    public long insertReadiness(ReadinessAssessment a) throws SQLException {
        return Sql.insert("""
                INSERT INTO readiness_assessments (exam_id, user_id, ai_job_id, weak_topics_json, strengths_json,
                  coverage_gaps, readiness_summary, data_limitations, completion_percent, planned_minutes, actual_minutes,
                  practice_average, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, a.getExamId());
            ps.setLong(2, a.getUserId());
            Sql.setLong(ps, 3, a.getAiJobId());
            Sql.setString(ps, 4, a.getWeakTopicsJson());
            Sql.setString(ps, 5, a.getStrengthsJson());
            Sql.setString(ps, 6, a.getCoverageGaps());
            Sql.setString(ps, 7, a.getReadinessSummary());
            Sql.setString(ps, 8, a.getDataLimitations());
            if (a.getCompletionPercent() == null) {
                ps.setNull(9, java.sql.Types.DECIMAL);
            } else {
                ps.setDouble(9, a.getCompletionPercent());
            }
            Sql.setInt(ps, 10, a.getPlannedMinutes());
            Sql.setInt(ps, 11, a.getActualMinutes());
            if (a.getPracticeAverage() == null) {
                ps.setNull(12, java.sql.Types.DECIMAL);
            } else {
                ps.setDouble(12, a.getPracticeAverage());
            }
        });
    }

    public Optional<ReadinessAssessment> latestReadiness(long examId, long userId) throws SQLException {
        return Sql.one("""
                SELECT * FROM readiness_assessments WHERE exam_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT 1
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> {
            ReadinessAssessment a = new ReadinessAssessment();
            a.setId(rs.getLong("id"));
            a.setExamId(rs.getLong("exam_id"));
            a.setUserId(rs.getLong("user_id"));
            a.setAiJobId(Sql.getLong(rs, "ai_job_id"));
            a.setWeakTopicsJson(rs.getString("weak_topics_json"));
            a.setStrengthsJson(rs.getString("strengths_json"));
            a.setCoverageGaps(rs.getString("coverage_gaps"));
            a.setReadinessSummary(rs.getString("readiness_summary"));
            a.setDataLimitations(rs.getString("data_limitations"));
            a.setCompletionPercent(Sql.getDouble(rs, "completion_percent"));
            a.setPlannedMinutes(Sql.getInt(rs, "planned_minutes"));
            a.setActualMinutes(Sql.getInt(rs, "actual_minutes"));
            a.setPracticeAverage(Sql.getDouble(rs, "practice_average"));
            a.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            return a;
        });
    }

    // ------------------------------------------------------------ concept explanations

    public long insertExplanation(ConceptExplanation e) throws SQLException {
        return Sql.insert("""
                INSERT INTO concept_explanations (exam_id, user_id, ai_job_id, topic_title, question, direct_answer,
                  explanation, examples_json, check_json, source_references, limitations, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            Sql.setLong(ps, 1, e.getExamId());
            ps.setLong(2, e.getUserId());
            Sql.setLong(ps, 3, e.getAiJobId());
            Sql.setString(ps, 4, e.getTopicTitle());
            ps.setString(5, e.getQuestion());
            Sql.setString(ps, 6, e.getDirectAnswer());
            Sql.setString(ps, 7, e.getExplanation());
            Sql.setString(ps, 8, e.getExamplesJson());
            Sql.setString(ps, 9, e.getCheckJson());
            Sql.setString(ps, 10, e.getSourceReferences());
            Sql.setString(ps, 11, e.getLimitations());
        });
    }

    public List<ConceptExplanation> explanations(long userId, Long examId, int limit) throws SQLException {
        if (examId == null) {
            return Sql.list("""
                    SELECT * FROM concept_explanations WHERE user_id = ? ORDER BY created_at DESC LIMIT ?
                    """, ps -> {
                ps.setLong(1, userId);
                ps.setInt(2, limit);
            }, this::mapExplanation);
        }
        return Sql.list("""
                SELECT * FROM concept_explanations WHERE user_id = ? AND exam_id = ? ORDER BY created_at DESC LIMIT ?
                """, ps -> {
            ps.setLong(1, userId);
            ps.setLong(2, examId);
            ps.setInt(3, limit);
        }, this::mapExplanation);
    }

    private ConceptExplanation mapExplanation(java.sql.ResultSet rs) throws SQLException {
        ConceptExplanation e = new ConceptExplanation();
        e.setId(rs.getLong("id"));
        e.setExamId(Sql.getLong(rs, "exam_id"));
        e.setUserId(rs.getLong("user_id"));
        e.setAiJobId(Sql.getLong(rs, "ai_job_id"));
        e.setTopicTitle(rs.getString("topic_title"));
        e.setQuestion(rs.getString("question"));
        e.setDirectAnswer(rs.getString("direct_answer"));
        e.setExplanation(rs.getString("explanation"));
        e.setExamplesJson(rs.getString("examples_json"));
        e.setCheckJson(rs.getString("check_json"));
        e.setSourceReferences(rs.getString("source_references"));
        e.setLimitations(rs.getString("limitations"));
        e.setCreatedAt(Sql.getDateTime(rs, "created_at"));
        return e;
    }

    // ------------------------------------------------------------ conversations

    public long ensureConversation(long userId, Long examId, String title) throws SQLException {
        Optional<Long> existing = Sql.one("""
                SELECT id FROM conversations WHERE user_id = ? AND ((exam_id IS NULL AND ? IS NULL) OR exam_id = ?)
                ORDER BY updated_at DESC LIMIT 1
                """, ps -> {
            ps.setLong(1, userId);
            Sql.setLong(ps, 2, examId);
            Sql.setLong(ps, 3, examId);
        }, rs -> rs.getLong("id"));
        if (existing.isPresent()) {
            return existing.get();
        }
        return Sql.insert("""
                INSERT INTO conversations (user_id, exam_id, title, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, userId);
            Sql.setLong(ps, 2, examId);
            ps.setString(3, title);
        });
    }

    public void insertMessage(long conversationId, String role, String content, String intent, Long aiJobId)
            throws SQLException {
        Sql.insert("""
                INSERT INTO conversation_messages (conversation_id, role, content, intent, ai_job_id, created_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, conversationId);
            ps.setString(2, role);
            ps.setString(3, content);
            Sql.setString(ps, 4, intent);
            Sql.setLong(ps, 5, aiJobId);
        });
        Sql.update("UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", ps -> ps.setLong(1, conversationId));
    }

    public List<ConversationMessage> messages(long conversationId, long userId, int limit) throws SQLException {
        return Sql.list("""
                SELECT m.* FROM conversation_messages m
                JOIN conversations c ON c.id = m.conversation_id
                WHERE m.conversation_id = ? AND c.user_id = ?
                ORDER BY m.created_at ASC LIMIT ?
                """, ps -> {
            ps.setLong(1, conversationId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
        }, rs -> {
            ConversationMessage m = new ConversationMessage();
            m.setId(rs.getLong("id"));
            m.setConversationId(rs.getLong("conversation_id"));
            m.setRole(rs.getString("role"));
            m.setContent(rs.getString("content"));
            m.setIntent(rs.getString("intent"));
            m.setAiJobId(Sql.getLong(rs, "ai_job_id"));
            m.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            return m;
        });
    }

    public List<Conversation> conversations(long userId) throws SQLException {
        return Sql.list("""
                SELECT * FROM conversations WHERE user_id = ? ORDER BY updated_at DESC
                """, ps -> ps.setLong(1, userId), rs -> {
            Conversation c = new Conversation();
            c.setId(rs.getLong("id"));
            c.setUserId(rs.getLong("user_id"));
            c.setExamId(Sql.getLong(rs, "exam_id"));
            c.setTitle(rs.getString("title"));
            c.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            c.setUpdatedAt(Sql.getDateTime(rs, "updated_at"));
            return c;
        });
    }

    public void deleteConversation(long id, long userId) throws SQLException {
        Sql.update("DELETE FROM conversations WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        });
    }
}
