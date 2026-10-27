package com.examlens.service.impl;

import com.examlens.dao.TopicDao;
import com.examlens.dao.impl.TopicDaoImpl;
import com.examlens.exception.AuthorizationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ResourceNotFoundException;
import com.examlens.exception.ValidationException;
import com.examlens.model.Topic;
import com.examlens.service.ExamService;
import com.examlens.service.TopicService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of TopicService verifying exam ownership and managing syllabus topics.
 */
public class TopicServiceImpl implements TopicService {

    private static final int MIN_TOPIC_LENGTH = 2;
    private static final int MAX_TOPIC_LENGTH = 200;
    private static final Set<String> ALLOWED_PRIORITIES = Set.of(
            Topic.PRIORITY_LOW,
            Topic.PRIORITY_MEDIUM,
            Topic.PRIORITY_HIGH,
            Topic.PRIORITY_TEACHER_IMPORTANT
    );

    private final TopicDao topicDao;
    private final ExamService examService;

    public TopicServiceImpl() {
        this(new TopicDaoImpl(), new ExamServiceImpl());
    }

    public TopicServiceImpl(TopicDao topicDao, ExamService examService) {
        if (topicDao == null) {
            throw new IllegalArgumentException("TopicDao cannot be null.");
        }
        if (examService == null) {
            throw new IllegalArgumentException("ExamService cannot be null.");
        }
        this.topicDao = topicDao;
        this.examService = examService;
    }

    @Override
    public Topic addTopic(Long examId, Long userId, Topic topic)
            throws ValidationException, ResourceNotFoundException, AuthorizationException, DataAccessException {

        if (examId == null) {
            throw new ValidationException("Exam ID is required to add a syllabus topic.");
        }
        if (topic == null) {
            throw new ValidationException("Topic details cannot be null.");
        }

        // Verify exam exists and belongs to the authenticated student
        examService.getExam(examId, userId);

        validateTopic(topic);

        // If subtopic, verify parent topic exists within the exact same exam
        if (topic.getParentTopicId() != null) {
            Optional<Topic> parentOpt = topicDao.findByIdAndExamId(topic.getParentTopicId(), examId);
            if (parentOpt.isEmpty()) {
                throw new ValidationException("Selected parent topic does not belong to this exam workspace.");
            }
        }

        topic.setExamId(examId);
        if (topic.getStatus() == null || topic.getStatus().isBlank()) {
            topic.setStatus(Topic.STATUS_NOT_STARTED);
        }

        return topicDao.save(topic);
    }

    @Override
    public List<Topic> getTopicsForExam(Long examId, Long userId)
            throws ResourceNotFoundException, AuthorizationException, DataAccessException {

        if (examId == null) {
            throw new ResourceNotFoundException("Exam ID cannot be null.");
        }

        // Verify exam exists and belongs to the authenticated student
        examService.getExam(examId, userId);

        return topicDao.findAllByExamId(examId);
    }

    private void validateTopic(Topic topic) throws ValidationException {
        if (topic.getName() == null || topic.getName().isBlank()) {
            throw new ValidationException("Topic name is required.");
        }

        String name = topic.getName().trim();
        if (name.length() < MIN_TOPIC_LENGTH || name.length() > MAX_TOPIC_LENGTH) {
            throw new ValidationException("Topic name must be between " + MIN_TOPIC_LENGTH + " and " + MAX_TOPIC_LENGTH + " characters.");
        }
        topic.setName(name);

        if (topic.getEstimatedMinutes() == null || topic.getEstimatedMinutes() <= 0) {
            topic.setEstimatedMinutes(60);
        }

        if (topic.getPriority() == null || topic.getPriority().isBlank()) {
            topic.setPriority(Topic.PRIORITY_MEDIUM);
        } else {
            String norm = topic.getPriority().trim().toUpperCase();
            if (!ALLOWED_PRIORITIES.contains(norm)) {
                topic.setPriority(Topic.PRIORITY_MEDIUM);
            } else {
                topic.setPriority(norm);
            }
        }
    }
}
