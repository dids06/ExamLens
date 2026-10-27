package com.examlens.dao;

import com.examlens.model.Topic;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Topic persistence operations.
 */
public interface TopicDao {

    Topic save(Topic topic);

    Optional<Topic> findById(Long topicId);

    Optional<Topic> findByIdAndExamId(Long topicId, Long examId);

    List<Topic> findAllByExamId(Long examId);

    boolean update(Topic topic);

    boolean deleteById(Long topicId);
}
