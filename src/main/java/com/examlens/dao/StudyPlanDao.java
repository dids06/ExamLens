package com.examlens.dao;

import com.examlens.model.StudyPlan;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for StudyPlan persistence operations.
 * Contract: StudyPlanDao supporting versioned plans and transactional persistence.
 */
public interface StudyPlanDao {

    StudyPlan save(Connection conn, StudyPlan plan);

    Optional<StudyPlan> findLatestByExamId(Long examId);

    Optional<StudyPlan> findById(Long planId);

    List<StudyPlan> findAllByExamId(Long examId);

    int getNextVersion(Connection conn, Long examId);
}
