package com.examlens.service;

import com.examlens.exception.AuthorizationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ResourceNotFoundException;
import com.examlens.exception.ValidationException;
import com.examlens.model.Topic;

import java.util.List;

/**
 * Service interface for Topic / Syllabus management.
 * Contract: TopicService with exam ownership checks.
 */
public interface TopicService {

    Topic addTopic(Long examId, Long userId, Topic topic)
            throws ValidationException, ResourceNotFoundException, AuthorizationException, DataAccessException;

    List<Topic> getTopicsForExam(Long examId, Long userId)
            throws ResourceNotFoundException, AuthorizationException, DataAccessException;
}
