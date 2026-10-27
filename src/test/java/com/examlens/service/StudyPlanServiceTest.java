package com.examlens.service;

import com.examlens.dao.StudyPlanDao;
import com.examlens.dao.StudyTaskDao;
import com.examlens.exception.AuthorizationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ResourceNotFoundException;
import com.examlens.exception.ValidationException;
import com.examlens.model.Exam;
import com.examlens.model.StudyPlan;
import com.examlens.model.StudyTask;
import com.examlens.model.Topic;
import com.examlens.service.impl.StudyPlanServiceImpl;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit test suite for StudyPlanService validating baseline generation,
 * scheduling rules, priority/hierarchy ordering, duplicate prevention,
 * cross-student authorization, and transaction rollback on persistence failure.
 */
public class StudyPlanServiceTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running StudyPlanServiceTest...");

        testGenerateInitialPlanSuccess();
        testMissingSyllabusTopics();
        testInvalidDailyStudyMinutes();
        testInvalidPastExamDate();
        testWorkloadExceedingAvailableTimeShortfall();
        testTopicPriorityAndPrerequisiteOrdering();
        testRepeatedGenerationDuplicatePrevention();
        testStudentOwnershipViolations();
        testTransactionRollbackOnPersistenceFailure();

        System.out.println("[PASS] All StudyPlanService tests passed successfully!");
    }

    private static void testGenerateInitialPlanSuccess() throws Exception {
        MockExamService examService = new MockExamService();
        Exam exam = new Exam();
        exam.setId(10L);
        exam.setUserId(1L);
        exam.setSubjectName("Computer Networks");
        exam.setDailyStudyMinutes(120);
        exam.setExamDateTime(LocalDateTime.now().plusDays(10));
        examService.registerExam(exam);

        MockTopicService topicService = new MockTopicService();
        Topic t1 = new Topic();
        t1.setId(101L);
        t1.setExamId(10L);
        t1.setName("OSI Model");
        t1.setEstimatedMinutes(120);
        t1.setPriority(Topic.PRIORITY_MEDIUM);

        Topic t2 = new Topic();
        t2.setId(102L);
        t2.setExamId(10L);
        t2.setName("TCP/IP Protocols");
        t2.setEstimatedMinutes(120);
        t2.setPriority(Topic.PRIORITY_HIGH);

        topicService.registerTopics(10L, List.of(t1, t2));

        InMemoryStudyPlanDao planDao = new InMemoryStudyPlanDao();
        InMemoryStudyTaskDao taskDao = new InMemoryStudyTaskDao();
        DummyConnectionProvider connProvider = new DummyConnectionProvider();

        StudyPlanService planService = new StudyPlanServiceImpl(planDao, taskDao, examService, topicService, connProvider);

        StudyPlan plan = planService.generateInitialPlan(10L, 1L);

        assertTrue(plan != null, "Plan should not be null.");
        assertTrue(plan.getId() != null, "Plan ID should be generated.");
        assertTrue(Integer.valueOf(1).equals(plan.getVersion()), "Plan version should be 1.");
        assertTrue(plan.getTasks().size() == 2, "Should contain 2 scheduled tasks.");
        assertTrue(plan.getTotalPlannedMinutes() == 240, "Total planned minutes should be 240.");
        assertTrue(connProvider.isCommitted(), "Transaction should be committed.");
    }

    private static void testMissingSyllabusTopics() throws Exception {
        MockExamService examService = new MockExamService();
        Exam exam = new Exam();
        exam.setId(20L);
        exam.setUserId(1L);
        exam.setDailyStudyMinutes(60);
        exam.setExamDateTime(LocalDateTime.now().plusDays(5));
        examService.registerExam(exam);

        MockTopicService topicService = new MockTopicService();
        topicService.registerTopics(20L, List.of()); // Empty topics

        InMemoryStudyPlanDao planDao = new InMemoryStudyPlanDao();
        InMemoryStudyTaskDao taskDao = new InMemoryStudyTaskDao();
        DummyConnectionProvider connProvider = new DummyConnectionProvider();

        StudyPlanService planService = new StudyPlanServiceImpl(planDao, taskDao, examService, topicService, connProvider);

        try {
            planService.generateInitialPlan(20L, 1L);
            throw new AssertionError("Expected ValidationException for missing syllabus topics.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("without syllabus topics"), "Expected missing topics message.");
        }
    }

    private static void testInvalidDailyStudyMinutes() throws Exception {
        MockExamService examService = new MockExamService();
        Exam exam = new Exam();
        exam.setId(30L);
        exam.setUserId(1L);
        exam.setDailyStudyMinutes(0); // Invalid <= 0
        exam.setExamDateTime(LocalDateTime.now().plusDays(5));
        examService.registerExam(exam);

        MockTopicService topicService = new MockTopicService();
        Topic t = new Topic();
        t.setId(301L);
        t.setExamId(30L);
        t.setName("Sample");
        topicService.registerTopics(30L, List.of(t));

        InMemoryStudyPlanDao planDao = new InMemoryStudyPlanDao();
        InMemoryStudyTaskDao taskDao = new InMemoryStudyTaskDao();
        DummyConnectionProvider connProvider = new DummyConnectionProvider();

        StudyPlanService planService = new StudyPlanServiceImpl(planDao, taskDao, examService, topicService, connProvider);

        try {
            planService.generateInitialPlan(30L, 1L);
            throw new AssertionError("Expected ValidationException for invalid daily study minutes.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("positive"), "Expected positive minutes message.");
        }
    }

    private static void testInvalidPastExamDate() throws Exception {
        MockExamService examService = new MockExamService();
        Exam exam = new Exam();
        exam.setId(40L);
        exam.setUserId(1L);
        exam.setDailyStudyMinutes(60);
        exam.setExamDateTime(LocalDateTime.now().minusDays(1)); // Past date
        examService.registerExam(exam);

        MockTopicService topicService = new MockTopicService();
        Topic t = new Topic();
        t.setId(401L);
        t.setExamId(40L);
        t.setName("Sample");
        topicService.registerTopics(40L, List.of(t));

        InMemoryStudyPlanDao planDao = new InMemoryStudyPlanDao();
        InMemoryStudyTaskDao taskDao = new InMemoryStudyTaskDao();
        DummyConnectionProvider connProvider = new DummyConnectionProvider();

        StudyPlanService planService = new StudyPlanServiceImpl(planDao, taskDao, examService, topicService, connProvider);

        try {
            planService.generateInitialPlan(40L, 1L);
            throw new AssertionError("Expected ValidationException for past exam date.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("in the future"), "Expected future exam date message.");
        }
    }

    private static void testWorkloadExceedingAvailableTimeShortfall() throws Exception {
        MockExamService examService = new MockExamService();
        Exam exam = new Exam();
        exam.setId(50L);
        exam.setUserId(1L);
        exam.setDailyStudyMinutes(60); // 1 hour per day
        exam.setExamDateTime(LocalDateTime.now().plusDays(2).withHour(10)); // ~2 study days max
        examService.registerExam(exam);

        MockTopicService topicService = new MockTopicService();
        // Massive workload: 600 minutes (10 hours)
        Topic t1 = new Topic();
        t1.setId(501L);
        t1.setExamId(50L);
        t1.setName("Massive Topic");
        t1.setEstimatedMinutes(600);
        t1.setPriority(Topic.PRIORITY_HIGH);
        topicService.registerTopics(50L, List.of(t1));

        InMemoryStudyPlanDao planDao = new InMemoryStudyPlanDao();
        InMemoryStudyTaskDao taskDao = new InMemoryStudyTaskDao();
        DummyConnectionProvider connProvider = new DummyConnectionProvider();

        StudyPlanService planService = new StudyPlanServiceImpl(planDao, taskDao, examService, topicService, connProvider);

        StudyPlan plan = planService.generateInitialPlan(50L, 1L);

        assertTrue(plan != null, "Plan should be generated.");
        assertTrue(plan.getChangeReason().contains("shortfall"), "Plan changeReason must explicitly record shortfall.");
        // Daily limits should not be exceeded
        for (StudyTask task : plan.getTasks()) {
            assertTrue(task.getPlannedMinutes() <= 60, "Task should never exceed daily limit.");
            assertTrue(task.getPlannedEnd().isBefore(exam.getExamDateTime()), "Task must end before exam deadline.");
        }
    }

    private static void testTopicPriorityAndPrerequisiteOrdering() throws Exception {
        MockExamService examService = new MockExamService();
        Exam exam = new Exam();
        exam.setId(60L);
        exam.setUserId(1L);
        exam.setDailyStudyMinutes(180);
        exam.setExamDateTime(LocalDateTime.now().plusDays(10));
        examService.registerExam(exam);

        MockTopicService topicService = new MockTopicService();

        // Topic 1: Parent (Medium)
        Topic p1 = new Topic();
        p1.setId(1L);
        p1.setName("Parent Data Structures");
        p1.setEstimatedMinutes(60);
        p1.setPriority(Topic.PRIORITY_MEDIUM);

        // Topic 2: Subtopic of Topic 1 with TEACHER_IMPORTANT
        Topic c1 = new Topic();
        c1.setId(2L);
        c1.setName("Red-Black Trees (Subtopic)");
        c1.setParentTopicId(1L);
        c1.setEstimatedMinutes(60);
        c1.setPriority(Topic.PRIORITY_TEACHER_IMPORTANT);

        // Topic 3: Unrelated top-level with LOW priority
        Topic p2 = new Topic();
        p2.setId(3L);
        p2.setName("Introduction to Computing");
        p2.setEstimatedMinutes(60);
        p2.setPriority(Topic.PRIORITY_LOW);

        // Topic 4: Top-level with TEACHER_IMPORTANT
        Topic p3 = new Topic();
        p3.setId(4L);
        p3.setName("Dynamic Programming");
        p3.setEstimatedMinutes(60);
        p3.setPriority(Topic.PRIORITY_TEACHER_IMPORTANT);

        topicService.registerTopics(60L, List.of(p1, c1, p2, p3));

        StudyPlanServiceImpl planServiceImpl = new StudyPlanServiceImpl(
                new InMemoryStudyPlanDao(), new InMemoryStudyTaskDao(), examService, topicService, new DummyConnectionProvider());

        List<Topic> ordered = planServiceImpl.sortTopicsByPriorityAndHierarchy(List.of(p1, c1, p2, p3));

        // P3 (TEACHER_IMPORTANT) should be ahead of P1 (MEDIUM) and P2 (LOW)
        assertTrue(ordered.get(0).getId().equals(4L), "Top-level TEACHER_IMPORTANT topic must be first.");

        // For Topic 1 and child C1: Parent (1) MUST appear before child (2)
        int parentIdx = -1;
        int childIdx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(1L)) parentIdx = i;
            if (ordered.get(i).getId().equals(2L)) childIdx = i;
        }
        assertTrue(parentIdx < childIdx, "Parent topic must strictly precede child subtopic.");
    }

    private static void testRepeatedGenerationDuplicatePrevention() throws Exception {
        MockExamService examService = new MockExamService();
        Exam exam = new Exam();
        exam.setId(70L);
        exam.setUserId(1L);
        exam.setDailyStudyMinutes(60);
        exam.setExamDateTime(LocalDateTime.now().plusDays(5));
        examService.registerExam(exam);

        MockTopicService topicService = new MockTopicService();
        Topic t = new Topic();
        t.setId(701L);
        t.setName("Sample Topic");
        t.setEstimatedMinutes(60);
        topicService.registerTopics(70L, List.of(t));

        InMemoryStudyPlanDao planDao = new InMemoryStudyPlanDao();
        InMemoryStudyTaskDao taskDao = new InMemoryStudyTaskDao();
        DummyConnectionProvider connProvider = new DummyConnectionProvider();

        StudyPlanService planService = new StudyPlanServiceImpl(planDao, taskDao, examService, topicService, connProvider);

        // First generation
        StudyPlan plan1 = planService.generateInitialPlan(70L, 1L);
        assertTrue(plan1.getId() != null, "First plan generated.");
        int initialTaskCount = plan1.getTasks().size();

        // Second submission of generation form
        StudyPlan plan2 = planService.generateInitialPlan(70L, 1L);

        // Must return the existing plan without creating duplicate version or duplicate tasks
        assertTrue(plan1.getId().equals(plan2.getId()), "Repeated generation must return existing plan.");
        assertTrue(plan2.getVersion() == 1, "Plan version should remain 1.");
        assertTrue(planDao.findAllByExamId(70L).size() == 1, "Only 1 plan row should exist in database.");
        assertTrue(taskDao.findAllByPlanId(plan1.getId()).size() == initialTaskCount, "Tasks count should remain identical.");
    }

    private static void testStudentOwnershipViolations() throws Exception {
        MockExamService examService = new MockExamService();
        Exam exam = new Exam();
        exam.setId(80L);
        exam.setUserId(1L); // Owned by User 1
        exam.setDailyStudyMinutes(60);
        exam.setExamDateTime(LocalDateTime.now().plusDays(5));
        examService.registerExam(exam);

        MockTopicService topicService = new MockTopicService();
        Topic t = new Topic();
        t.setId(801L);
        t.setName("Sample");
        topicService.registerTopics(80L, List.of(t));

        InMemoryStudyPlanDao planDao = new InMemoryStudyPlanDao();
        InMemoryStudyTaskDao taskDao = new InMemoryStudyTaskDao();
        DummyConnectionProvider connProvider = new DummyConnectionProvider();

        StudyPlanService planService = new StudyPlanServiceImpl(planDao, taskDao, examService, topicService, connProvider);

        // User 2 attempts to generate plan for User 1's exam
        try {
            planService.generateInitialPlan(80L, 2L);
            throw new AssertionError("Expected AuthorizationException for cross-student access.");
        } catch (AuthorizationException expected) {
            assertTrue(expected.getMessage().contains("Access denied"), "Expected access denied message.");
        }
    }

    private static void testTransactionRollbackOnPersistenceFailure() throws Exception {
        MockExamService examService = new MockExamService();
        Exam exam = new Exam();
        exam.setId(90L);
        exam.setUserId(1L);
        exam.setDailyStudyMinutes(60);
        exam.setExamDateTime(LocalDateTime.now().plusDays(5));
        examService.registerExam(exam);

        MockTopicService topicService = new MockTopicService();
        Topic t = new Topic();
        t.setId(901L);
        t.setName("Topic to fail");
        t.setEstimatedMinutes(60);
        topicService.registerTopics(90L, List.of(t));

        InMemoryStudyPlanDao planDao = new InMemoryStudyPlanDao();
        FailingStudyTaskDao taskDao = new FailingStudyTaskDao(); // Simulates failure on task insert
        DummyConnectionProvider connProvider = new DummyConnectionProvider();

        StudyPlanService planService = new StudyPlanServiceImpl(planDao, taskDao, examService, topicService, connProvider);

        try {
            planService.generateInitialPlan(90L, 1L);
            throw new AssertionError("Expected DataAccessException when task persistence fails.");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("Transaction failed"), "Expected transaction failed message.");
            assertTrue(connProvider.isRolledBack(), "Transaction rollback must have been invoked.");
            assertTrue(!connProvider.isCommitted(), "Transaction commit must NOT have occurred.");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    // -------------------------------------------------------------------------
    // Test Doubles & In-Memory Stubs
    // -------------------------------------------------------------------------

    private static class InMemoryStudyPlanDao implements StudyPlanDao {
        private final Map<Long, StudyPlan> byId = new HashMap<>();
        private final Map<Long, List<StudyPlan>> byExamId = new HashMap<>();
        private long idSeq = 1L;

        @Override
        public StudyPlan save(Connection conn, StudyPlan plan) {
            plan.setId(idSeq++);
            byId.put(plan.getId(), plan);
            byExamId.computeIfAbsent(plan.getExamId(), k -> new ArrayList<>()).add(plan);
            return plan;
        }

        @Override
        public Optional<StudyPlan> findLatestByExamId(Long examId) {
            List<StudyPlan> plans = byExamId.get(examId);
            if (plans == null || plans.isEmpty()) return Optional.empty();
            return Optional.of(plans.get(plans.size() - 1));
        }

        @Override
        public Optional<StudyPlan> findById(Long planId) {
            return Optional.ofNullable(byId.get(planId));
        }

        @Override
        public List<StudyPlan> findAllByExamId(Long examId) {
            return byExamId.getOrDefault(examId, List.of());
        }

        @Override
        public int getNextVersion(Connection conn, Long examId) {
            return byExamId.getOrDefault(examId, List.of()).size() + 1;
        }
    }

    private static class InMemoryStudyTaskDao implements StudyTaskDao {
        private final Map<Long, List<StudyTask>> byPlanId = new HashMap<>();
        private long idSeq = 1L;

        @Override
        public void saveAll(Connection conn, List<StudyTask> tasks) {
            for (StudyTask t : tasks) {
                t.setId(idSeq++);
                byPlanId.computeIfAbsent(t.getPlanId(), k -> new ArrayList<>()).add(t);
            }
        }

        @Override
        public List<StudyTask> findAllByPlanId(Long planId) {
            return byPlanId.getOrDefault(planId, List.of());
        }

        @Override
        public Optional<StudyTask> findById(Long taskId) {
            return Optional.empty();
        }

        @Override
        public boolean update(StudyTask task) {
            return true;
        }
    }

    private static class FailingStudyTaskDao implements StudyTaskDao {
        @Override
        public void saveAll(Connection conn, List<StudyTask> tasks) {
            throw new DataAccessException("Simulated database failure during task batch insert.");
        }

        @Override
        public List<StudyTask> findAllByPlanId(Long planId) {
            return List.of();
        }

        @Override
        public Optional<StudyTask> findById(Long taskId) {
            return Optional.empty();
        }

        @Override
        public boolean update(StudyTask task) {
            return false;
        }
    }

    private static class MockExamService implements ExamService {
        private final Map<Long, Exam> store = new HashMap<>();

        public void registerExam(Exam exam) {
            store.put(exam.getId(), exam);
        }

        @Override
        public Exam getExam(Long examId, Long userId)
                throws ResourceNotFoundException, AuthorizationException, DataAccessException {
            Exam exam = store.get(examId);
            if (exam == null) {
                throw new ResourceNotFoundException("Exam not found: #" + examId);
            }
            if (!exam.getUserId().equals(userId)) {
                throw new AuthorizationException("Access denied: You do not own exam #" + examId);
            }
            return exam;
        }

        @Override
        public Exam createExam(Long userId, Exam exam) { return null; }
        @Override
        public List<Exam> getExams(Long userId) { return List.of(); }
        @Override
        public void updateExam(Long examId, Long userId, Exam exam) {}
        @Override
        public void deleteExam(Long examId, Long userId) {}
    }

    private static class MockTopicService implements TopicService {
        private final Map<Long, List<Topic>> store = new HashMap<>();

        public void registerTopics(Long examId, List<Topic> topics) {
            store.put(examId, topics);
        }

        @Override
        public List<Topic> getTopicsForExam(Long examId, Long userId) {
            return store.getOrDefault(examId, List.of());
        }

        @Override
        public Topic addTopic(Long examId, Long userId, Topic topic) { return null; }
    }

    private static class DummyConnectionProvider implements StudyPlanServiceImpl.ConnectionProvider {
        private final AtomicBoolean committed = new AtomicBoolean(false);
        private final AtomicBoolean rolledBack = new AtomicBoolean(false);
        private boolean autoCommit = true;

        @Override
        public Connection getConnection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, methodArgs) -> {
                        String name = method.getName();
                        if ("setAutoCommit".equals(name)) {
                            autoCommit = (Boolean) methodArgs[0];
                            return null;
                        } else if ("getAutoCommit".equals(name)) {
                            return autoCommit;
                        } else if ("commit".equals(name)) {
                            committed.set(true);
                            return null;
                        } else if ("rollback".equals(name)) {
                            rolledBack.set(true);
                            return null;
                        } else if ("close".equals(name)) {
                            return null;
                        }
                        return null;
                    }
            );
        }

        public boolean isCommitted() {
            return committed.get();
        }

        public boolean isRolledBack() {
            return rolledBack.get();
        }
    }
}
