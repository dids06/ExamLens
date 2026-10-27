package com.examlens.dao;

import com.examlens.model.PlanChangeExplanation;
import com.examlens.model.PlanVersion;
import com.examlens.model.StudyPlan;
import com.examlens.model.StudySession;
import com.examlens.model.StudyTask;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlanDao {

    // ------------------------------------------------------------ plan

    public long ensurePlan(long examId, long userId) throws SQLException {
        Optional<Long> existing = Sql.one("SELECT id FROM study_plans WHERE exam_id = ? AND user_id = ?", ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> rs.getLong("id"));
        if (existing.isPresent()) {
            return existing.get();
        }
        return Sql.insert("""
                INSERT INTO study_plans (exam_id, user_id, created_at, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        });
    }

    public Optional<StudyPlan> findPlan(long examId, long userId) throws SQLException {
        return Sql.one("SELECT * FROM study_plans WHERE exam_id = ? AND user_id = ?", ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> {
            StudyPlan p = new StudyPlan();
            p.setId(rs.getLong("id"));
            p.setExamId(rs.getLong("exam_id"));
            p.setUserId(rs.getLong("user_id"));
            p.setActiveVersionId(Sql.getLong(rs, "active_version_id"));
            p.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            p.setUpdatedAt(Sql.getDateTime(rs, "updated_at"));
            return p;
        });
    }

    public int nextVersionNumber(long planId) throws SQLException {
        List<Integer> rows = Sql.list("SELECT COALESCE(MAX(version_number), 0) AS v FROM plan_versions WHERE plan_id = ?",
                ps -> ps.setLong(1, planId), rs -> rs.getInt("v"));
        return (rows.isEmpty() ? 0 : rows.get(0)) + 1;
    }

    public long insertPlanVersion(PlanVersion v) throws SQLException {
        return Sql.insert("""
                INSERT INTO plan_versions (plan_id, version_number, kind, status, ai_job_id, summary, feasibility,
                  feasibility_note, assumptions, warnings, risks, tradeoffs, unscheduled_work, total_estimated_minutes,
                  available_capacity_minutes, change_summary, change_explanation, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, v.getPlanId());
            ps.setInt(2, v.getVersionNumber());
            ps.setString(3, v.getKind());
            ps.setString(4, v.getStatus());
            Sql.setLong(ps, 5, v.getAiJobId());
            Sql.setString(ps, 6, v.getSummary());
            Sql.setString(ps, 7, v.getFeasibility());
            Sql.setString(ps, 8, v.getFeasibilityNote());
            Sql.setString(ps, 9, v.getAssumptions());
            Sql.setString(ps, 10, v.getWarnings());
            Sql.setString(ps, 11, v.getRisks());
            Sql.setString(ps, 12, v.getTradeoffs());
            Sql.setString(ps, 13, v.getUnscheduledWork());
            Sql.setInt(ps, 14, v.getTotalEstimatedMinutes());
            Sql.setInt(ps, 15, v.getAvailableCapacityMinutes());
            Sql.setString(ps, 16, v.getChangeSummary());
            Sql.setString(ps, 17, v.getChangeExplanation());
        });
    }

    public void archiveVersions(long planId) throws SQLException {
        Sql.update("UPDATE plan_versions SET status = 'ARCHIVED' WHERE plan_id = ?", ps -> ps.setLong(1, planId));
    }

    public void setActiveVersion(long planId, long versionId) throws SQLException {
        Sql.update("UPDATE study_plans SET active_version_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", ps -> {
            ps.setLong(1, versionId);
            ps.setLong(2, planId);
        });
    }

    public Optional<PlanVersion> findVersion(long versionId, long userId) throws SQLException {
        return Sql.one("""
                SELECT pv.*, p.user_id AS owner_id FROM plan_versions pv
                JOIN study_plans p ON p.id = pv.plan_id
                WHERE pv.id = ? AND p.user_id = ?
                """, ps -> {
            ps.setLong(1, versionId);
            ps.setLong(2, userId);
        }, this::mapVersion);
    }

    public Optional<PlanVersion> activeVersion(long examId, long userId) throws SQLException {
        return Sql.one("""
                SELECT pv.* FROM plan_versions pv
                JOIN study_plans p ON p.id = pv.plan_id
                WHERE p.exam_id = ? AND p.user_id = ? AND pv.status = 'ACTIVE'
                ORDER BY pv.version_number DESC LIMIT 1
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, this::mapVersion);
    }

    public List<PlanVersion> versionsForExam(long examId, long userId) throws SQLException {
        return Sql.list("""
                SELECT pv.* FROM plan_versions pv
                JOIN study_plans p ON p.id = pv.plan_id
                WHERE p.exam_id = ? AND p.user_id = ?
                ORDER BY pv.version_number DESC
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, this::mapVersion);
    }

    // ------------------------------------------------------------ tasks

    public long insertTask(StudyTask t) throws SQLException {
        return Sql.insert("""
                INSERT INTO study_tasks (plan_version_id, task_key, exam_id, user_id, topic_id, task_type, title,
                  objective, study_method, estimated_minutes, scheduled_date, task_order, priority, status, ai_rationale,
                  revision_of_task_key, change_type, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, t.getPlanVersionId());
            ps.setString(2, t.getTaskKey());
            ps.setLong(3, t.getExamId());
            ps.setLong(4, t.getUserId());
            Sql.setLong(ps, 5, t.getTopicId());
            ps.setString(6, t.getTaskType());
            ps.setString(7, t.getTitle());
            Sql.setString(ps, 8, t.getObjective());
            Sql.setString(ps, 9, t.getStudyMethod());
            ps.setInt(10, t.getEstimatedMinutes());
            Sql.setDate(ps, 11, t.getScheduledDate());
            ps.setInt(12, t.getTaskOrder());
            Sql.setString(ps, 13, t.getPriority());
            ps.setString(14, t.getStatus());
            Sql.setString(ps, 15, t.getAiRationale());
            Sql.setString(ps, 16, t.getRevisionOfTaskKey());
            Sql.setString(ps, 17, t.getChangeType());
        });
    }

    public void insertPrerequisite(long taskId, String prerequisiteTaskKey) throws SQLException {
        Sql.update("INSERT INTO task_prerequisites (task_id, prerequisite_task_key) VALUES (?, ?)", ps -> {
            ps.setLong(1, taskId);
            ps.setString(2, prerequisiteTaskKey);
        });
    }

    public List<StudyTask> tasksForVersion(long versionId, long userId) throws SQLException {
        List<StudyTask> tasks = Sql.list("""
                SELECT t.* FROM study_tasks t JOIN exams e ON e.id = t.exam_id
                WHERE t.plan_version_id = ? AND e.user_id = ?
                ORDER BY t.scheduled_date, t.task_order, t.id
                """, ps -> {
            ps.setLong(1, versionId);
            ps.setLong(2, userId);
        }, this::mapTask);
        for (StudyTask t : tasks) {
            t.setPrerequisites(prerequisites(t.getId()));
        }
        return tasks;
    }

    public List<String> prerequisites(long taskId) throws SQLException {
        return Sql.list("SELECT prerequisite_task_key FROM task_prerequisites WHERE task_id = ?",
                ps -> ps.setLong(1, taskId), rs -> rs.getString("prerequisite_task_key"));
    }

    public Optional<StudyTask> findTask(long taskId, long userId) throws SQLException {
        Optional<StudyTask> task = Sql.one("""
                SELECT t.* FROM study_tasks t WHERE t.id = ? AND t.user_id = ?
                """, ps -> {
            ps.setLong(1, taskId);
            ps.setLong(2, userId);
        }, this::mapTask);
        task.ifPresent(t -> {
            try {
                t.setPrerequisites(prerequisites(t.getId()));
            } catch (SQLException ignored) {
                // non-fatal for display
            }
        });
        return task;
    }

    public void updateTaskStatus(long taskId, long userId, String status) throws SQLException {
        if ("COMPLETED".equals(status)) {
            Sql.update("""
                    UPDATE study_tasks SET status = ?, completed_at = CURRENT_TIMESTAMP
                    WHERE id = ? AND user_id = ?
                    """, ps -> {
                ps.setString(1, status);
                ps.setLong(2, taskId);
                ps.setLong(3, userId);
            });
        } else {
            Sql.update("""
                    UPDATE study_tasks SET status = ?, completed_at = NULL WHERE id = ? AND user_id = ?
                    """, ps -> {
                ps.setString(1, status);
                ps.setLong(2, taskId);
                ps.setLong(3, userId);
            });
        }
    }

    public List<StudyTask> tasksForDate(long examId, long userId, java.time.LocalDate date) throws SQLException {
        return Sql.list("""
                SELECT t.* FROM study_tasks t
                JOIN plan_versions pv ON pv.id = t.plan_version_id
                WHERE t.exam_id = ? AND t.user_id = ? AND t.scheduled_date = ? AND pv.status = 'ACTIVE'
                ORDER BY t.task_order
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            Sql.setDate(ps, 3, date);
        }, this::mapTask);
    }

    public List<StudyTask> upcomingTasks(long examId, long userId, int limit) throws SQLException {
        return Sql.list("""
                SELECT t.* FROM study_tasks t
                JOIN plan_versions pv ON pv.id = t.plan_version_id
                WHERE t.exam_id = ? AND t.user_id = ? AND pv.status = 'ACTIVE' AND t.status = 'PENDING'
                ORDER BY t.scheduled_date, t.task_order LIMIT ?
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
        }, this::mapTask);
    }

    public int[] taskStats(long examId, long userId) throws SQLException {
        List<int[]> rows = Sql.list("""
                SELECT COUNT(*) AS total, SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) AS done
                FROM study_tasks t JOIN plan_versions pv ON pv.id = t.plan_version_id
                WHERE t.exam_id = ? AND t.user_id = ? AND pv.status = 'ACTIVE'
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> new int[]{rs.getInt("total"), rs.getInt("done")});
        return rows.isEmpty() ? new int[]{0, 0} : rows.get(0);
    }

    public int plannedMinutes(long examId, long userId) throws SQLException {
        List<Integer> rows = Sql.list("""
                SELECT COALESCE(SUM(t.estimated_minutes), 0) AS m FROM study_tasks t
                JOIN plan_versions pv ON pv.id = t.plan_version_id
                WHERE t.exam_id = ? AND t.user_id = ? AND pv.status = 'ACTIVE'
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> rs.getInt("m"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    // ------------------------------------------------------------ sessions

    public long insertSession(StudySession s) throws SQLException {
        return Sql.insert("""
                INSERT INTO study_sessions (user_id, exam_id, task_id, planned_minutes, actual_minutes, status,
                  difficulty_feedback, student_notes, session_date, ai_job_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, s.getUserId());
            ps.setLong(2, s.getExamId());
            Sql.setLong(ps, 3, s.getTaskId());
            Sql.setInt(ps, 4, s.getPlannedMinutes());
            ps.setInt(5, s.getActualMinutes());
            ps.setString(6, s.getStatus());
            Sql.setString(ps, 7, s.getDifficultyFeedback());
            Sql.setString(ps, 8, s.getStudentNotes());
            Sql.setDate(ps, 9, s.getSessionDate());
            Sql.setLong(ps, 10, s.getAiJobId());
        });
    }

    public void attachSessionJob(long sessionId, long aiJobId) throws SQLException {
        Sql.update("UPDATE study_sessions SET ai_job_id = ? WHERE id = ?", ps -> {
            ps.setLong(1, aiJobId);
            ps.setLong(2, sessionId);
        });
    }

    public List<StudySession> sessionsForExam(long examId, long userId, int limit) throws SQLException {
        return Sql.list("""
                SELECT * FROM study_sessions WHERE exam_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT ?
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
        }, this::mapSession);
    }

    public List<StudySession> sessionsForTask(long taskId, long userId) throws SQLException {
        return Sql.list("""
                SELECT * FROM study_sessions WHERE task_id = ? AND user_id = ? ORDER BY created_at DESC
                """, ps -> {
            ps.setLong(1, taskId);
            ps.setLong(2, userId);
        }, this::mapSession);
    }

    public int actualMinutes(long examId, long userId) throws SQLException {
        List<Integer> rows = Sql.list("""
                SELECT COALESCE(SUM(actual_minutes), 0) AS m FROM study_sessions WHERE exam_id = ? AND user_id = ?
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> rs.getInt("m"));
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    // ------------------------------------------------------------ change explanations

    public long insertChangeExplanation(PlanChangeExplanation e) throws SQLException {
        return Sql.insert("""
                INSERT INTO plan_change_explanations (plan_version_id, ai_job_id, summary, changes_json,
                  unchanged_priorities, action_items, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, ps -> {
            ps.setLong(1, e.getPlanVersionId());
            Sql.setLong(ps, 2, e.getAiJobId());
            Sql.setString(ps, 3, e.getSummary());
            Sql.setString(ps, 4, e.getChangesJson());
            Sql.setString(ps, 5, e.getUnchangedPriorities());
            Sql.setString(ps, 6, e.getActionItems());
        });
    }

    public Optional<PlanChangeExplanation> changeExplanation(long planVersionId) throws SQLException {
        return Sql.one("""
                SELECT * FROM plan_change_explanations WHERE plan_version_id = ? ORDER BY created_at DESC LIMIT 1
                """, ps -> ps.setLong(1, planVersionId), rs -> {
            PlanChangeExplanation e = new PlanChangeExplanation();
            e.setId(rs.getLong("id"));
            e.setPlanVersionId(rs.getLong("plan_version_id"));
            e.setAiJobId(Sql.getLong(rs, "ai_job_id"));
            e.setSummary(rs.getString("summary"));
            e.setChangesJson(rs.getString("changes_json"));
            e.setUnchangedPriorities(rs.getString("unchanged_priorities"));
            e.setActionItems(rs.getString("action_items"));
            e.setCreatedAt(Sql.getDateTime(rs, "created_at"));
            return e;
        });
    }

    // ------------------------------------------------------------ mapping

    private PlanVersion mapVersion(java.sql.ResultSet rs) throws SQLException {
        PlanVersion v = new PlanVersion();
        v.setId(rs.getLong("id"));
        v.setPlanId(rs.getLong("plan_id"));
        v.setVersionNumber(rs.getInt("version_number"));
        v.setKind(rs.getString("kind"));
        v.setStatus(rs.getString("status"));
        v.setAiJobId(Sql.getLong(rs, "ai_job_id"));
        v.setSummary(rs.getString("summary"));
        v.setFeasibility(rs.getString("feasibility"));
        v.setFeasibilityNote(rs.getString("feasibility_note"));
        v.setAssumptions(rs.getString("assumptions"));
        v.setWarnings(rs.getString("warnings"));
        v.setRisks(rs.getString("risks"));
        v.setTradeoffs(rs.getString("tradeoffs"));
        v.setUnscheduledWork(rs.getString("unscheduled_work"));
        v.setTotalEstimatedMinutes(Sql.getInt(rs, "total_estimated_minutes"));
        v.setAvailableCapacityMinutes(Sql.getInt(rs, "available_capacity_minutes"));
        v.setChangeSummary(rs.getString("change_summary"));
        v.setChangeExplanation(rs.getString("change_explanation"));
        v.setCreatedAt(Sql.getDateTime(rs, "created_at"));
        return v;
    }

    private StudyTask mapTask(java.sql.ResultSet rs) throws SQLException {
        StudyTask t = new StudyTask();
        t.setId(rs.getLong("id"));
        t.setPlanVersionId(rs.getLong("plan_version_id"));
        t.setTaskKey(rs.getString("task_key"));
        t.setExamId(rs.getLong("exam_id"));
        t.setUserId(rs.getLong("user_id"));
        t.setTopicId(Sql.getLong(rs, "topic_id"));
        t.setTaskType(rs.getString("task_type"));
        t.setTitle(rs.getString("title"));
        t.setObjective(rs.getString("objective"));
        t.setStudyMethod(rs.getString("study_method"));
        t.setEstimatedMinutes(rs.getInt("estimated_minutes"));
        t.setScheduledDate(Sql.getDate(rs, "scheduled_date"));
        t.setTaskOrder(rs.getInt("task_order"));
        t.setPriority(rs.getString("priority"));
        t.setStatus(rs.getString("status"));
        t.setAiRationale(rs.getString("ai_rationale"));
        t.setRevisionOfTaskKey(rs.getString("revision_of_task_key"));
        t.setChangeType(rs.getString("change_type"));
        t.setCompletedAt(Sql.getDateTime(rs, "completed_at"));
        t.setPrerequisites(new ArrayList<>());
        return t;
    }

    private StudySession mapSession(java.sql.ResultSet rs) throws SQLException {
        StudySession s = new StudySession();
        s.setId(rs.getLong("id"));
        s.setUserId(rs.getLong("user_id"));
        s.setExamId(rs.getLong("exam_id"));
        s.setTaskId(Sql.getLong(rs, "task_id"));
        s.setPlannedMinutes(Sql.getInt(rs, "planned_minutes"));
        s.setActualMinutes(rs.getInt("actual_minutes"));
        s.setStatus(rs.getString("status"));
        s.setDifficultyFeedback(rs.getString("difficulty_feedback"));
        s.setStudentNotes(rs.getString("student_notes"));
        s.setSessionDate(Sql.getDate(rs, "session_date"));
        s.setAiJobId(Sql.getLong(rs, "ai_job_id"));
        s.setCreatedAt(Sql.getDateTime(rs, "created_at"));
        return s;
    }

    public LocalDateTime latestSessionTime(long examId, long userId) throws SQLException {
        List<LocalDateTime> rows = Sql.list("""
                SELECT MAX(created_at) AS m FROM study_sessions WHERE exam_id = ? AND user_id = ?
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, userId);
        }, rs -> Sql.getDateTime(rs, "m"));
        return rows.isEmpty() ? null : rows.get(0);
    }
}
