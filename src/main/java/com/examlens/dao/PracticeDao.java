package com.examlens.dao;

import com.examlens.model.AttemptAnswer;
import com.examlens.model.PaperAttempt;
import com.examlens.model.PaperQuestion;
import com.examlens.model.PracticePaper;
import com.examlens.util.Json;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class PracticeDao {

    // ------------------------------------------------------------ papers

    public long insertPaper(long examId, long userId, Long aiJobId, String title, String instructions,
                            int totalMarks, Integer durationMinutes, String difficulty, String status) throws SQLException {
        return Sql.insert("""
                INSERT INTO practice_papers (exam_id, user_id, ai_job_id, title, instructions, total_marks,
                  duration_minutes, difficulty, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            Sql.setLong(ps, 3, aiJobId);
            ps.setString(4, title);
            Sql.setString(ps, 5, instructions);
            ps.setInt(6, totalMarks);
            Sql.setInt(ps, 7, durationMinutes);
            Sql.setString(ps, 8, difficulty);
            ps.setString(9, status);
        });
    }

    public long insertQuestion(PaperQuestion q) throws SQLException {
        return Sql.insert("""
                INSERT INTO paper_questions (paper_id, client_key, question_type, topic, topic_id, prompt, options_json,
                  correct_option, marks, difficulty, model_answer, rubric_json, display_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setLong(1, q.getPaperId());
            ps.setString(2, q.getClientKey());
            ps.setString(3, q.getQuestionType());
            Sql.setString(ps, 4, q.getTopic());
            Sql.setLong(ps, 5, q.getTopicId());
            ps.setString(6, q.getPrompt());
            Sql.setString(ps, 7, q.getOptions().isEmpty() ? null : Json.write(q.getOptions()));
            Sql.setString(ps, 8, q.getCorrectOption());
            ps.setInt(9, q.getMarks());
            Sql.setString(ps, 10, q.getDifficulty());
            Sql.setString(ps, 11, q.getModelAnswer());
            Sql.setString(ps, 12, q.getRubric().isEmpty() ? null : Json.write(q.getRubric()));
            ps.setInt(13, q.getDisplayOrder());
        });
    }

    public Optional<PracticePaper> findPaper(long paperId, long userId) throws SQLException {
        Optional<PracticePaper> paper = Sql.one("SELECT * FROM practice_papers WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, paperId);
            ps.setLong(2, userId);
        }, this::mapPaper);
        paper.ifPresent(p -> {
            try {
                p.setQuestions(questions(p.getId()));
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
        return paper;
    }

    public List<PracticePaper> papersForExam(long examId, long userId) throws SQLException {
        List<PracticePaper> papers = Sql.list("""
                SELECT * FROM practice_papers WHERE exam_id = ? AND user_id = ? ORDER BY created_at DESC
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, this::mapPaper);
        for (PracticePaper p : papers) {
            p.setQuestions(questions(p.getId()));
        }
        return papers;
    }

    public List<PaperQuestion> questions(long paperId) throws SQLException {
        return Sql.list("SELECT * FROM paper_questions WHERE paper_id = ? ORDER BY display_order, id",
                ps -> ps.setLong(1, paperId), this::mapQuestion);
    }

    public void updateQuestionSolutions(long questionId, String modelAnswer, String rubricJson) throws SQLException {
        Sql.update("UPDATE paper_questions SET model_answer = ?, rubric_json = ? WHERE id = ?", ps -> {
            ps.setString(1, modelAnswer);
            ps.setString(2, rubricJson);
            ps.setLong(3, questionId);
        });
    }

    public void updatePaperStatus(long paperId, long userId, String status) throws SQLException {
        Sql.update("UPDATE practice_papers SET status = ? WHERE id = ? AND user_id = ?", ps -> {
            ps.setString(1, status);
            ps.setLong(2, paperId);
            ps.setLong(3, userId);
        });
    }

    // ------------------------------------------------------------ attempts

    public long insertAttempt(long paperId, long userId, boolean timed, Integer durationMinutes, int totalPossible) throws SQLException {
        return Sql.insert("""
                INSERT INTO paper_attempts (paper_id, user_id, status, started_at, timed, duration_minutes, total_possible)
                VALUES (?, ?, 'IN_PROGRESS', CURRENT_TIMESTAMP, ?, ?, ?)
                """, ps -> {
            ps.setLong(1, paperId);
            ps.setLong(2, userId);
            ps.setBoolean(3, timed);
            Sql.setInt(ps, 4, durationMinutes);
            ps.setInt(5, totalPossible);
        });
    }

    public Optional<PaperAttempt> findAttempt(long attemptId, long userId) throws SQLException {
        Optional<PaperAttempt> attempt = Sql.one("SELECT * FROM paper_attempts WHERE id = ? AND user_id = ?", ps -> {
            ps.setLong(1, attemptId);
            ps.setLong(2, userId);
        }, this::mapAttempt);
        if (attempt.isEmpty()) {
            return attempt;
        }
        PaperAttempt a = attempt.get();
        Optional<PracticePaper> paper = findPaper(a.getPaperId(), userId);
        paper.ifPresent(a::setPaper);
        return attempt;
    }

    public List<PaperAttempt> attemptsForPaper(long paperId, long userId) throws SQLException {
        return Sql.list("""
                SELECT * FROM paper_attempts WHERE paper_id = ? AND user_id = ? ORDER BY started_at DESC
                """, ps -> {
            ps.setLong(1, paperId);
            ps.setLong(2, userId);
        }, this::mapAttempt);
    }

    public List<PaperAttempt> attemptsForExam(long examId, long userId, int limit) throws SQLException {
        return Sql.list("""
                SELECT a.* FROM paper_attempts a JOIN practice_papers p ON p.id = a.paper_id
                WHERE p.exam_id = ? AND a.user_id = ? ORDER BY a.started_at DESC LIMIT ?
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
        }, this::mapAttempt);
    }

    public void updateAttemptTotals(long attemptId, long userId, String status, Double awarded, int possible, Long aiJobId)
            throws SQLException {
        Sql.update("""
                UPDATE paper_attempts SET status = ?, submitted_at = CURRENT_TIMESTAMP, total_awarded = ?,
                  total_possible = ?, ai_job_id = ?
                WHERE id = ? AND user_id = ?
                """, ps -> {
            ps.setString(1, status);
            if (awarded == null) {
                ps.setNull(2, java.sql.Types.DECIMAL);
            } else {
                ps.setDouble(2, awarded);
            }
            ps.setInt(3, possible);
            Sql.setLong(ps, 4, aiJobId);
            ps.setLong(5, attemptId);
            ps.setLong(6, userId);
        });
    }

    public long upsertAnswer(long attemptId, long questionId, String answerText, String selectedOption,
                             Double awarded, String gradingMethod, String feedback, String criteriaJson,
                             String confidence, boolean requiresManualReview, Long aiJobId) throws SQLException {
        Optional<Long> existing = Sql.one("""
                SELECT id FROM attempt_answers WHERE attempt_id = ? AND question_id = ?
                """, ps -> {
            ps.setLong(1, attemptId);
            ps.setLong(2, questionId);
        }, rs -> rs.getLong("id"));
        if (existing.isPresent()) {
            Sql.update("""
                    UPDATE attempt_answers SET answer_text = ?, selected_option = ?, awarded_marks = ?, grading_method = ?,
                      feedback = ?, criteria_json = ?, confidence = ?, requires_manual_review = ?, ai_job_id = ?,
                      updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """, ps -> {
                Sql.setString(ps, 1, answerText);
                Sql.setString(ps, 2, selectedOption);
                if (awarded == null) {
                    ps.setNull(3, java.sql.Types.DECIMAL);
                } else {
                    ps.setDouble(3, awarded);
                }
                Sql.setString(ps, 4, gradingMethod);
                Sql.setString(ps, 5, feedback);
                Sql.setString(ps, 6, criteriaJson);
                Sql.setString(ps, 7, confidence);
                ps.setBoolean(8, requiresManualReview);
                Sql.setLong(ps, 9, aiJobId);
                ps.setLong(10, existing.get());
            });
            return existing.get();
        }
        return Sql.insert("""
                INSERT INTO attempt_answers (attempt_id, question_id, answer_text, selected_option, awarded_marks,
                  grading_method, feedback, criteria_json, confidence, requires_manual_review, ai_job_id, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, attemptId);
            ps.setLong(2, questionId);
            Sql.setString(ps, 3, answerText);
            Sql.setString(ps, 4, selectedOption);
            if (awarded == null) {
                ps.setNull(5, java.sql.Types.DECIMAL);
            } else {
                ps.setDouble(5, awarded);
            }
            Sql.setString(ps, 6, gradingMethod);
            Sql.setString(ps, 7, feedback);
            Sql.setString(ps, 8, criteriaJson);
            Sql.setString(ps, 9, confidence);
            ps.setBoolean(10, requiresManualReview);
            Sql.setLong(ps, 11, aiJobId);
        });
    }

    public List<AttemptAnswer> answers(long attemptId) throws SQLException {
        List<AttemptAnswer> answers = Sql.list("""
                SELECT * FROM attempt_answers WHERE attempt_id = ? ORDER BY question_id
                """, ps -> ps.setLong(1, attemptId), this::mapAnswer);
        for (AttemptAnswer a : answers) {
            Sql.one("SELECT * FROM paper_questions WHERE id = ?", ps -> ps.setLong(1, a.getQuestionId()), this::mapQuestion)
                    .ifPresent(a::setQuestion);
        }
        return answers;
    }

    public Double practiceAverageForExam(long examId, long userId) throws SQLException {
        List<Double> rows = Sql.list("""
                SELECT AVG(CASE WHEN a.total_possible > 0 THEN a.total_awarded * 100.0 / a.total_possible ELSE NULL END) AS avg
                FROM paper_attempts a JOIN practice_papers p ON p.id = a.paper_id
                WHERE p.exam_id = ? AND a.user_id = ? AND a.status IN ('SUBMITTED','GRADED')
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> Sql.getDouble(rs, "avg"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Double topicScoreRatio(long examId, long userId, long topicId) throws SQLException {
        List<Double> rows = Sql.list("""
                SELECT SUM(aa.awarded_marks) AS earned, SUM(q.marks) AS possible
                FROM attempt_answers aa
                JOIN paper_questions q ON q.id = aa.question_id
                JOIN paper_attempts a ON a.id = aa.attempt_id
                JOIN practice_papers p ON p.id = a.paper_id
                WHERE p.exam_id = ? AND a.user_id = ? AND q.topic_id = ? AND a.status IN ('SUBMITTED','GRADED')
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            ps.setLong(3, topicId);
        }, rs -> {
            Double earned = Sql.getDouble(rs, "earned");
            Double possible = Sql.getDouble(rs, "possible");
            if (earned == null || possible == null || possible == 0) {
                return null;
            }
            return earned / possible;
        });
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int countPapers(long examId, long userId) throws SQLException {
        List<Integer> rows = Sql.list("SELECT COUNT(*) AS c FROM practice_papers WHERE exam_id = ? AND user_id = ?",
                ps -> {
                    ps.setLong(1, examId);
                    ps.setLong(2, userId);
                }, rs -> rs.getInt("c"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    // ------------------------------------------------------------ mapping

    private PracticePaper mapPaper(java.sql.ResultSet rs) throws SQLException {
        PracticePaper p = new PracticePaper();
        p.setId(rs.getLong("id"));
        p.setExamId(rs.getLong("exam_id"));
        p.setUserId(rs.getLong("user_id"));
        p.setAiJobId(Sql.getLong(rs, "ai_job_id"));
        p.setTitle(rs.getString("title"));
        p.setInstructions(rs.getString("instructions"));
        p.setTotalMarks(rs.getInt("total_marks"));
        p.setDurationMinutes(Sql.getInt(rs, "duration_minutes"));
        p.setDifficulty(rs.getString("difficulty"));
        p.setStatus(rs.getString("status"));
        p.setCreatedAt(Sql.getDateTime(rs, "created_at"));
        return p;
    }

    private PaperQuestion mapQuestion(java.sql.ResultSet rs) throws SQLException {
        PaperQuestion q = new PaperQuestion();
        q.setId(rs.getLong("id"));
        q.setPaperId(rs.getLong("paper_id"));
        q.setClientKey(rs.getString("client_key"));
        q.setQuestionType(rs.getString("question_type"));
        q.setTopic(rs.getString("topic"));
        q.setTopicId(Sql.getLong(rs, "topic_id"));
        q.setPrompt(rs.getString("prompt"));
        String options = rs.getString("options_json");
        if (options != null && !options.isBlank()) {
            try {
                q.setOptions(Json.mapper().readValue(options, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                }));
            } catch (Exception ignored) {
                // keep empty options
            }
        }
        q.setCorrectOption(rs.getString("correct_option"));
        q.setMarks(rs.getInt("marks"));
        q.setDifficulty(rs.getString("difficulty"));
        q.setModelAnswer(rs.getString("model_answer"));
        String rubric = rs.getString("rubric_json");
        if (rubric != null && !rubric.isBlank()) {
            try {
                q.setRubric(Json.mapper().readValue(rubric, new com.fasterxml.jackson.core.type.TypeReference<List<PaperQuestion.RubricItem>>() {
                }));
            } catch (Exception ignored) {
                // keep empty rubric
            }
        }
        q.setDisplayOrder(rs.getInt("display_order"));
        return q;
    }

    private PaperAttempt mapAttempt(java.sql.ResultSet rs) throws SQLException {
        PaperAttempt a = new PaperAttempt();
        a.setId(rs.getLong("id"));
        a.setPaperId(rs.getLong("paper_id"));
        a.setUserId(rs.getLong("user_id"));
        a.setStatus(rs.getString("status"));
        a.setStartedAt(Sql.getDateTime(rs, "started_at"));
        a.setSubmittedAt(Sql.getDateTime(rs, "submitted_at"));
        a.setTimed(rs.getBoolean("timed"));
        a.setDurationMinutes(Sql.getInt(rs, "duration_minutes"));
        a.setTotalAwarded(Sql.getDouble(rs, "total_awarded"));
        a.setTotalPossible(Sql.getInt(rs, "total_possible"));
        a.setAiJobId(Sql.getLong(rs, "ai_job_id"));
        return a;
    }

    private AttemptAnswer mapAnswer(java.sql.ResultSet rs) throws SQLException {
        AttemptAnswer a = new AttemptAnswer();
        a.setId(rs.getLong("id"));
        a.setAttemptId(rs.getLong("attempt_id"));
        a.setQuestionId(rs.getLong("question_id"));
        a.setAnswerText(rs.getString("answer_text"));
        a.setSelectedOption(rs.getString("selected_option"));
        a.setAwardedMarks(Sql.getDouble(rs, "awarded_marks"));
        a.setGradingMethod(rs.getString("grading_method"));
        a.setFeedback(rs.getString("feedback"));
        String criteria = rs.getString("criteria_json");
        if (criteria != null && !criteria.isBlank()) {
            try {
                a.setCriteria(Json.mapper().readValue(criteria,
                        new com.fasterxml.jackson.core.type.TypeReference<List<AttemptAnswer.CriterionResult>>() {
                        }));
            } catch (Exception ignored) {
                // keep empty criteria
            }
        }
        a.setConfidence(rs.getString("confidence"));
        a.setRequiresManualReview(rs.getBoolean("requires_manual_review"));
        a.setAiJobId(Sql.getLong(rs, "ai_job_id"));
        a.setUpdatedAt(Sql.getDateTime(rs, "updated_at"));
        return a;
    }

    public LocalDateTime paperCreatedAt(long paperId) throws SQLException {
        List<LocalDateTime> rows = Sql.list("SELECT created_at FROM practice_papers WHERE id = ?", ps -> ps.setLong(1, paperId),
                rs -> Sql.getDateTime(rs, "created_at"));
        return rows.isEmpty() ? null : rows.get(0);
    }
}
