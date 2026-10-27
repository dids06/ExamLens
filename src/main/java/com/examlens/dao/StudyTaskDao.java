package com.examlens.dao;

import com.examlens.model.StudyTask;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for StudyTask persistence operations.
 */
public interface StudyTaskDao {

    void saveAll(Connection conn, List<StudyTask> tasks);

    List<StudyTask> findAllByPlanId(Long planId);

    Optional<StudyTask> findById(Long taskId);

    boolean update(StudyTask task);
}
