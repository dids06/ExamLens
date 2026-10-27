package com.examlens.service;

import com.examlens.dao.TopicDao;
import com.examlens.exception.AuthorizationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ResourceNotFoundException;
import com.examlens.exception.ValidationException;
import com.examlens.model.Exam;
import com.examlens.model.Topic;
import com.examlens.service.impl.TopicServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for TopicService business rules, subtopic hierarchy validation,
 * and exam workspace ownership verification using in-memory stubs.
 */
public class TopicServiceTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running TopicServiceTest...");

        testAddTopicSuccess();
        testAddTopicDefaultsApplied();
        testAddTopicValidationFailures();
        testAddTopicUnauthorizedExam();
        testAddTopicNonExistentExam();
        testAddSubtopicSuccess();
        testAddSubtopicParentFromDifferentExam();
        testGetTopicsForExamSuccessAndAuthorization();

        System.out.println("[PASS] All TopicService tests passed successfully!");
    }

    private static void testAddTopicSuccess() throws Exception {
        MockExamService examService = new MockExamService();
        examService.registerExam(100L, 1L); // Exam 100 owned by User 1

        InMemoryTopicDao topicDao = new InMemoryTopicDao();
        TopicService topicService = new TopicServiceImpl(topicDao, examService);

        Topic topic = new Topic();
        topic.setName("Binary Search Trees");
        topic.setEstimatedMinutes(90);
        topic.setPriority(Topic.PRIORITY_HIGH);

        Topic saved = topicService.addTopic(100L, 1L, topic);

        assertTrue(saved.getId() != null, "Topic ID should be generated.");
        assertTrue(Long.valueOf(100L).equals(saved.getExamId()), "Exam ID should match.");
        assertTrue("Binary Search Trees".equals(saved.getName()), "Name should match.");
        assertTrue(Integer.valueOf(90).equals(saved.getEstimatedMinutes()), "Estimated minutes should match.");
        assertTrue(Topic.PRIORITY_HIGH.equals(saved.getPriority()), "Priority should match HIGH.");
        assertTrue(Topic.STATUS_NOT_STARTED.equals(saved.getStatus()), "Status should default to NOT_STARTED.");
    }

    private static void testAddTopicDefaultsApplied() throws Exception {
        MockExamService examService = new MockExamService();
        examService.registerExam(100L, 1L);

        InMemoryTopicDao topicDao = new InMemoryTopicDao();
        TopicService topicService = new TopicServiceImpl(topicDao, examService);

        Topic topic = new Topic();
        topic.setName("Dynamic Programming");
        topic.setEstimatedMinutes(null); // Should default to 60
        topic.setPriority("UNKNOWN_PRIORITY"); // Should default to MEDIUM

        Topic saved = topicService.addTopic(100L, 1L, topic);

        assertTrue(Integer.valueOf(60).equals(saved.getEstimatedMinutes()), "Null estimated minutes should default to 60.");
        assertTrue(Topic.PRIORITY_MEDIUM.equals(saved.getPriority()), "Unknown priority should default to MEDIUM.");
    }

    private static void testAddTopicValidationFailures() {
        MockExamService examService = new MockExamService();
        examService.registerExam(100L, 1L);

        InMemoryTopicDao topicDao = new InMemoryTopicDao();
        TopicService topicService = new TopicServiceImpl(topicDao, examService);

        // Blank name
        Topic blankTopic = new Topic();
        blankTopic.setName("   ");
        try {
            topicService.addTopic(100L, 1L, blankTopic);
            throw new AssertionError("Expected ValidationException for blank name.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Topic name is required"), "Expected required message.");
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception: " + e.getMessage());
        }

        // Short name (< 2 chars)
        Topic shortTopic = new Topic();
        shortTopic.setName("A");
        try {
            topicService.addTopic(100L, 1L, shortTopic);
            throw new AssertionError("Expected ValidationException for 1-char name.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("between 2 and 200"), "Expected length bounds message.");
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception: " + e.getMessage());
        }
    }

    private static void testAddTopicUnauthorizedExam() {
        MockExamService examService = new MockExamService();
        examService.registerExam(100L, 1L); // Owned by User 1

        InMemoryTopicDao topicDao = new InMemoryTopicDao();
        TopicService topicService = new TopicServiceImpl(topicDao, examService);

        Topic topic = new Topic();
        topic.setName("Hacking Attempts");

        // User 2 attempts to add a topic to User 1's exam
        try {
            topicService.addTopic(100L, 2L, topic);
            throw new AssertionError("Expected AuthorizationException for cross-student topic addition.");
        } catch (AuthorizationException expected) {
            assertTrue(expected.getMessage().contains("Access denied"), "Expected access denied message.");
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception: " + e.getMessage());
        }
    }

    private static void testAddTopicNonExistentExam() {
        MockExamService examService = new MockExamService();

        InMemoryTopicDao topicDao = new InMemoryTopicDao();
        TopicService topicService = new TopicServiceImpl(topicDao, examService);

        Topic topic = new Topic();
        topic.setName("Lost Topic");

        try {
            topicService.addTopic(999L, 1L, topic);
            throw new AssertionError("Expected ResourceNotFoundException for non-existent exam.");
        } catch (ResourceNotFoundException expected) {
            assertTrue(expected.getMessage().contains("not found"), "Expected not found message.");
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception: " + e.getMessage());
        }
    }

    private static void testAddSubtopicSuccess() throws Exception {
        MockExamService examService = new MockExamService();
        examService.registerExam(100L, 1L);

        InMemoryTopicDao topicDao = new InMemoryTopicDao();
        TopicService topicService = new TopicServiceImpl(topicDao, examService);

        // Add parent topic
        Topic parent = new Topic();
        parent.setName("Graph Theory");
        Topic parentSaved = topicService.addTopic(100L, 1L, parent);

        // Add subtopic
        Topic subtopic = new Topic();
        subtopic.setName("Dijkstra Algorithm");
        subtopic.setParentTopicId(parentSaved.getId());

        Topic subtopicSaved = topicService.addTopic(100L, 1L, subtopic);
        assertTrue(parentSaved.getId().equals(subtopicSaved.getParentTopicId()), "Subtopic parent ID must match.");
    }

    private static void testAddSubtopicParentFromDifferentExam() throws Exception {
        MockExamService examService = new MockExamService();
        examService.registerExam(100L, 1L); // Exam 100
        examService.registerExam(200L, 1L); // Exam 200

        InMemoryTopicDao topicDao = new InMemoryTopicDao();
        TopicService topicService = new TopicServiceImpl(topicDao, examService);

        // Topic in Exam 200
        Topic topicExam200 = new Topic();
        topicExam200.setName("Chemistry Topic");
        Topic parentInExam200 = topicService.addTopic(200L, 1L, topicExam200);

        // Subtopic attempting to be added to Exam 100 but pointing to parent in Exam 200
        Topic subtopicInExam100 = new Topic();
        subtopicInExam100.setName("Cross-Exam Subtopic");
        subtopicInExam100.setParentTopicId(parentInExam200.getId());

        try {
            topicService.addTopic(100L, 1L, subtopicInExam100);
            throw new AssertionError("Expected ValidationException when parent belongs to another exam.");
        } catch (ValidationException expected) {
            assertTrue(expected.getMessage().contains("does not belong to this exam workspace"),
                    "Expected foreign parent error.");
        }
    }

    private static void testGetTopicsForExamSuccessAndAuthorization() throws Exception {
        MockExamService examService = new MockExamService();
        examService.registerExam(100L, 1L);

        InMemoryTopicDao topicDao = new InMemoryTopicDao();
        TopicService topicService = new TopicServiceImpl(topicDao, examService);

        Topic t1 = new Topic();
        t1.setName("Topic 1");
        topicService.addTopic(100L, 1L, t1);

        Topic t2 = new Topic();
        t2.setName("Topic 2");
        topicService.addTopic(100L, 1L, t2);

        // Owner retrieves topics
        List<Topic> topics = topicService.getTopicsForExam(100L, 1L);
        assertTrue(topics.size() == 2, "Owner should see 2 topics.");

        // Non-owner attempts to retrieve topics
        try {
            topicService.getTopicsForExam(100L, 2L);
            throw new AssertionError("Expected AuthorizationException for non-owner topic list.");
        } catch (AuthorizationException expected) {
            assertTrue(expected.getMessage().contains("Access denied"), "Expected access denied message.");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    /**
     * In-memory test stub for TopicDao.
     */
    private static class InMemoryTopicDao implements TopicDao {
        private final Map<Long, Topic> store = new HashMap<>();
        private long idSequence = 1L;

        @Override
        public Topic save(Topic topic) {
            topic.setId(idSequence++);
            topic.setCreatedAt(LocalDateTime.now());
            store.put(topic.getId(), topic);
            return topic;
        }

        @Override
        public Optional<Topic> findById(Long topicId) {
            return Optional.ofNullable(store.get(topicId));
        }

        @Override
        public Optional<Topic> findByIdAndExamId(Long topicId, Long examId) {
            Topic t = store.get(topicId);
            if (t != null && t.getExamId().equals(examId)) {
                return Optional.of(t);
            }
            return Optional.empty();
        }

        @Override
        public List<Topic> findAllByExamId(Long examId) {
            List<Topic> list = new ArrayList<>();
            for (Topic t : store.values()) {
                if (t.getExamId().equals(examId)) {
                    list.add(t);
                }
            }
            return list;
        }

        @Override
        public boolean update(Topic topic) {
            if (store.containsKey(topic.getId())) {
                store.put(topic.getId(), topic);
                return true;
            }
            return false;
        }

        @Override
        public boolean deleteById(Long topicId) {
            return store.remove(topicId) != null;
        }
    }

    /**
     * Mock ExamService for testing topic authorization and workspace linking.
     */
    private static class MockExamService implements ExamService {
        private final Map<Long, Long> examToOwner = new HashMap<>();

        public void registerExam(Long examId, Long userId) {
            examToOwner.put(examId, userId);
        }

        @Override
        public Exam createExam(Long userId, Exam exam) {
            return null;
        }

        @Override
        public Exam getExam(Long examId, Long userId)
                throws ResourceNotFoundException, AuthorizationException, DataAccessException {
            if (!examToOwner.containsKey(examId)) {
                throw new ResourceNotFoundException("Exam #" + examId + " not found.");
            }
            Long ownerId = examToOwner.get(examId);
            if (!ownerId.equals(userId)) {
                throw new AuthorizationException("Access denied: You do not own exam #" + examId);
            }
            Exam exam = new Exam();
            exam.setId(examId);
            exam.setUserId(ownerId);
            return exam;
        }

        @Override
        public List<Exam> getExams(Long userId) {
            return List.of();
        }

        @Override
        public void updateExam(Long examId, Long userId, Exam exam) {
        }

        @Override
        public void deleteExam(Long examId, Long userId) {
        }
    }
}
